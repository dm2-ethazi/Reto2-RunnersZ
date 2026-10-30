#!/usr/bin/env python3
"""Tablero kanban de RunnersZ vía GitHub Projects (protocolo C.5.1).

Envuelve la GitHub CLI (`gh`) para mantener el tablero del proyecto
`dm2-ethazi` número 14. No tiene dependencias externas: usa solo la
biblioteca estándar de Python 3.12 y llama a `gh` por subproceso.

Subcomandos:
    status          tabla por columna (nº, I-NN, título, responsable, horas, PR y CI)
    start N         mueve el issue N a «En curso» (y lo añade al proyecto si falta)
    review N        mueve el issue N a «En revisión»
    done N          mueve el issue N a «Hecho»
    --dry-run       global: imprime las acciones sin ejecutarlas

Si falta el scope `project`, el error indica exactamente:
    gh auth refresh -s repo,project,read:org,workflow
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import unicodedata
from pathlib import Path
from typing import Any

OWNER = "dm2-ethazi"
PROJECT = 14

INDICE = Path(__file__).resolve().parent.parent / "docs" / "planificacion" / "indice-issues.md"

STATUS_FIELD = "Status"
STATUS_OPTIONS = {
    "start": "En curso",
    "review": "En revisión",
    "done": "Hecho",
}
BACKLOG = "Backlog"
COLUMN_ORDER = [BACKLOG, "En curso", "En revisión", "Hecho"]

AUTH_HINT = "Ejecuta: gh auth refresh -s repo,project,read:org,workflow"


class GhError(RuntimeError):
    """Error al invocar gh (falta autenticación, scope o el elemento no existe)."""


def run_gh(args: list[str], *, dry_run: bool = False) -> str:
    """Ejecuta `gh ...` y devuelve su stdout. Con dry_run solo imprime."""
    if dry_run:
        print(f"[dry-run] gh {' '.join(args)}")
        return ""
    try:
        proc = subprocess.run(
            ["gh", *args],
            capture_output=True,
            text=True,
            encoding="utf-8",
            check=False,
        )
    except FileNotFoundError as exc:  # gh no está en el PATH
        raise GhError("No se encuentra `gh`. Instálala con: winget install GitHub.cli") from exc
    if proc.returncode != 0:
        msg = (proc.stderr or proc.stdout or "").strip()
        if "login" in msg.lower() or "auth" in msg.lower() or "scope" in msg.lower():
            raise GhError(f"{msg}\n{AUTH_HINT}")
        raise GhError(msg or f"gh {' '.join(args)} devolvió {proc.returncode}")
    return proc.stdout


def _project_field_map(dry_run: bool = False) -> dict[str, str]:
    """Devuelve {nombre de campo: id} del proyecto."""
    raw = run_gh(
        ["project", "field-list", str(PROJECT), "--owner", OWNER, "--format", "json"],
        dry_run=dry_run,
    )
    if not raw:
        return {}
    data = json.loads(raw)
    return {f["name"]: f["id"] for f in data.get("fields", [])}


def _option_map(raw: str, field_name: str) -> dict[str, str]:
    """Devuelve {nombre de opción: id} de un campo single-select."""
    data = json.loads(raw)
    for f in data.get("fields", []):
        if f["name"] == field_name:
            return {o["name"]: o["id"] for o in f.get("options", [])}
    return {}


def _project_id(dry_run: bool = False) -> str:
    raw = run_gh(
        ["project", "view", str(PROJECT), "--owner", OWNER, "--format", "json"],
        dry_run=dry_run,
    )
    if not raw:
        return ""
    return json.loads(raw)["id"]


def _items(dry_run: bool = False) -> list[dict[str, Any]]:
    """Lista los items del proyecto con sus campos y número de issue."""
    raw = run_gh(
        [
            "project", "item-list", str(PROJECT), "--owner", OWNER,
            "--format", "json", "--limit", "100",
        ],
        dry_run=dry_run,
    )
    if not raw:
        return []
    return json.loads(raw).get("items", [])


def _issue_number(item: dict[str, Any]) -> int | None:
    content = item.get("content") or {}
    return content.get("number")


def _load_indice(path: Path = INDICE) -> dict[int, tuple[str, str]]:
    """Carga el índice contrastado `indice-issues.md`: {nº issue: (I-NN, título)}.

    El I-NN **no se deduce del número de GitHub** (tras la reestructuración del
    29-10-2026 el orden cambió: #13=I-13, #15=I-12, #41=I-15). Se lee de la tabla
    verificada contra los cuerpos de los issues (§Anexo I).
    """
    if not path.is_file():
        return {}
    out: dict[int, tuple[str, str]] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        m = re.match(r"\|\s*(I-\d{2})\s*\|\s*#?(\d+)\s*\|\s*([^|]+?)\s*\|", line)
        if m:
            out[int(m.group(2))] = (m.group(1), m.group(3).strip())
    return out


def _backlog_id(item: dict[str, Any], indice: dict[int, tuple[str, str]] | None = None) -> str:
    """Identificador interno I-NN de una tarjeta.

    1. Si el título lo trae explícito («[I-07] …»), se usa.
    2. Si no, se busca el nº de issue en el índice contrastado y se **contrasta**
       que el título siga siendo el del índice; si difiere, se marca «(título≠índice)».
    3. Si no aparece en ninguno de los dos: «?».
    """
    title = item.get("title") or (item.get("content") or {}).get("title", "")
    m = re.search(r"\[(I-\d{2})\]", title)
    if m:
        return m.group(1)
    if indice is None:
        indice = _load_indice()
    n = _issue_number(item)
    if n in indice:
        iid, esperado = indice[n]
        if _norm(esperado)[:24] != _norm(title)[:24]:
            return f"{iid} (título≠índice)"
        return iid
    return "?"


def _assignees(item: dict[str, Any]) -> list[str]:
    """Logins asignados (gh devuelve strings en la raíz y dicts en content)."""
    raw = item.get("assignees")
    if raw is None:
        raw = (item.get("content") or {}).get("assignees", [])
    out = []
    for a in raw:
        out.append(a.get("login", "") if isinstance(a, dict) else str(a))
    return [a for a in out if a]


def _norm(s: str) -> str:
    """Normaliza para comparar sin acentos ni mayúsculas («Área» == «area»)."""
    s = unicodedata.normalize("NFKD", s or "")
    return "".join(c for c in s if not unicodedata.combining(c)).lower()


def _field_value(item: dict[str, Any], field: str) -> str:
    """Valor de un campo de la tarjeta.

    `gh project item-list` expone los campos como claves superiores en
    minúscula («horas», «prioridad», «sprint», «área», «status»), no como
    array `fields`; se comprueba primero ahí y se cae al formato anidado.
    """
    want = _norm(field)
    for key, value in item.items():
        if _norm(str(key)) == want and value is not None and not isinstance(value, (dict, list)):
            return str(value)
    for f in item.get("fields") or []:
        if f.get("name") == field:
            if "text" in f:
                return f["text"] or ""
            if "number" in f and f["number"] is not None:
                return str(f["number"])
            if "name" in f:
                return f["name"] or ""
    return ""


def _find_item(number: int) -> dict[str, Any] | None:
    for item in _items():
        if _issue_number(item) == number:
            return item
    return None


def _linked_pr(number: int, iid: str, dry_run: bool) -> tuple[str, str]:
    """Busca las PRs que referencian al issue; devuelve (nºs, estado CI).

    Coincide por `closingIssuesReferences`, por «#N» en el cuerpo o título y
    también por el identificador interno («[I-06] …», rama «i06-…»), porque
    el I-NN no coincide con el número de issue.
    """
    raw = run_gh(
        ["pr", "list", "--state", "all", "--limit", "100",
         "--json", "number,title,body,headRefName,statusCheckRollup,closingIssuesReferences"],
        dry_run=dry_run,
    )
    if not raw:
        return ("-", "-")
    nn = iid.split()[0].lower().replace("-", "") if iid != "?" else ""
    matches = []
    for pr in json.loads(raw):
        linked = any(issue.get("number") == number
                     for issue in pr.get("closingIssuesReferences", []))
        reference = re.search(rf"(?<![\w/])#{number}(?!\d)",
                              pr.get("body", "") + " " + pr.get("title", ""))
        por_iid = bool(nn) and (
            nn in (pr.get("title") or "").lower().replace("-", "")
            or nn in (pr.get("headRefName") or "").lower())
        if linked or reference or por_iid:
            matches.append(pr)
    if not matches:
        return ("-", "-")
    states = []
    for pr in matches:
        checks = pr.get("statusCheckRollup") or []
        if not checks:
            states.append("sin checks")
            continue
        values = [(check.get("conclusion") or check.get("state") or
                   check.get("status") or "").upper() for check in checks]
        if any(value in {"FAILURE", "ERROR", "CANCELLED", "TIMED_OUT",
                         "ACTION_REQUIRED", "STARTUP_FAILURE"} for value in values):
            states.append("CI rojo")
        elif all(value == "SUCCESS" for value in values):
            states.append("CI verde")
        else:
            states.append("CI en curso")
    priority = ["CI rojo", "CI en curso", "sin checks", "CI verde"]
    return (", ".join(str(pr["number"]) for pr in matches),
            next(state for state in priority if state in states))


def cmd_status(args: argparse.Namespace) -> None:
    items = _items(dry_run=args.dry_run)
    if not items and not args.dry_run:
        print("El proyecto no tiene tarjetas todavía.")
        return
    indice = _load_indice()
    by_col: dict[str, list[dict[str, Any]]] = {c: [] for c in COLUMN_ORDER}
    for item in items:
        # gh incluye la columna Status como clave superior "status" del item.
        col = item.get("status") or _field_value(item, STATUS_FIELD) or BACKLOG
        by_col.setdefault(col, []).append(item)
    for col in COLUMN_ORDER:
        print(f"\n== {col} ({len(by_col.get(col, []))}) ==")
        for item in sorted(by_col.get(col, []), key=lambda i: _issue_number(i) or 0):
            n = _issue_number(item)
            title = item.get("title") or (item.get("content") or {}).get("title", "")
            resp = ", ".join(_assignees(item)) or "sin asignar"
            horas = _field_value(item, "Horas") or "?"
            iid = _backlog_id(item, indice)
            pr, ci = _linked_pr(n, iid, args.dry_run) if n else ("-", "-")
            print(f"  #{n} {iid} | {title[:44]:44} | {resp:10} | {horas:>3} h | PR {pr} | {ci}")


def _set_status(number: int, target: str, dry_run: bool) -> None:
    item = _find_item(number)
    if item is None:
        run_gh(
            ["project", "item-add", str(PROJECT), "--owner", OWNER,
             "--url", f"https://github.com/{OWNER}/Reto2-RunnersZ/issues/{number}"],
            dry_run=dry_run,
        )
        item = _find_item(number) if not dry_run else None
        if item is None and not dry_run:
            raise GhError(f"No se pudo añadir el issue #{number} al proyecto.")
    if item is not None and not _assignees(item):
        run_gh(["issue", "edit", str(number), "--add-assignee", "@me"],
               dry_run=dry_run)
    fields = _project_field_map(dry_run=dry_run)
    status_id = fields.get(STATUS_FIELD)
    proj_id = _project_id(dry_run=dry_run)
    if not status_id or not proj_id:
        raise GhError(f"No se encontró el campo «{STATUS_FIELD}» en el proyecto {PROJECT}.")
    raw = run_gh(
        ["project", "field-list", str(PROJECT), "--owner", OWNER, "--format", "json"],
        dry_run=dry_run,
    )
    opts = _option_map(raw, STATUS_FIELD) if raw else {}
    option_id = opts.get(target)
    if not option_id and not dry_run:
        raise GhError(
            f"La opción «{target}» no existe en «{STATUS_FIELD}». "
            "Renómbrala en la UI del proyecto (Project → Settings → Status)."
        )
    item_id = item["id"] if item else "<item-id>"
    run_gh(
        ["project", "item-edit", "--id", item_id, "--project-id", proj_id,
         "--field-id", status_id, "--single-select-option-id", option_id or "<option-id>"],
        dry_run=dry_run,
    )
    print(f"Issue #{number} → {target}")


def _cmd_move(args: argparse.Namespace, key: str) -> None:
    _set_status(args.number, STATUS_OPTIONS[key], args.dry_run)


def _positive_int(value: str) -> int:
    n = int(value)
    if n < 1:
        raise argparse.ArgumentTypeError("el número de issue debe ser positivo")
    return n


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="board.py", description=__doc__)
    p.add_argument("--dry-run", action="store_true", help="imprime las acciones sin ejecutarlas")
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status", help="tabla del tablero por columna")
    for name, help_ in (("start", "mueve a En curso"), ("review", "mueve a En revisión"),
                        ("done", "mueve a Hecho")):
        sp = sub.add_parser(name, help=help_)
        sp.add_argument("number", type=_positive_int, help="número del issue de GitHub")
    return p


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        if args.cmd == "status":
            cmd_status(args)
        elif args.cmd == "start":
            _cmd_move(args, "start")
        elif args.cmd == "review":
            _cmd_move(args, "review")
        elif args.cmd == "done":
            _cmd_move(args, "done")
    except GhError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
