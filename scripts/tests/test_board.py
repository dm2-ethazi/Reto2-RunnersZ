"""Tests unitarios de scripts/board.py con la GitHub CLI simulada.

No requieren red: monkeypatchean `subprocess.run` para devolver salidas
ficticias de `gh` y verificar la búsqueda de ids de campo y de opción.
"""

import json
import subprocess
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import board  # noqa: E402

FIELDS_JSON = {
    "fields": [
        {"name": "Status", "id": "PVTT_lADO", "options": [
            {"name": "Backlog", "id": "opt-backlog"},
            {"name": "En curso", "id": "opt-curso"},
            {"name": "En revisión", "id": "opt-revision"},
            {"name": "Hecho", "id": "opt-hecho"},
        ]},
        {"name": "Horas", "id": "PVTSSF_lADO", "dataType": "NUMBER"},
        {"name": "Prioridad", "id": "PVTSSF_prior", "options": [
            {"name": "A", "id": "opt-a"}, {"name": "B", "id": "opt-b"}]},
    ]
}

ITEMS_JSON = {
    "items": [
        {"id": "PVTI_lADO1", "content": {"number": 1, "title": "[I-01] Repositorio",
                                          "assignees": [{"login": "jairoben"}]},
         "fields": [{"name": "Status", "name": "Status", "name": "Status"}]},
    ]
}


def _fake_proc(stdout="", stderr="", returncode=0):
    return subprocess.CompletedProcess(["gh"], returncode, stdout, stderr)


def test_option_map_devuelve_ids(monkeypatch):
    def fake_run(cmd, **kwargs):
        return _fake_proc(json.dumps(FIELDS_JSON))

    monkeypatch.setattr(subprocess, "run", fake_run)
    raw = board.run_gh(["project", "field-list", "14", "--owner", "dm2-ethazi",
                        "--format", "json"])
    opts = board._option_map(raw, "Status")
    assert opts["En curso"] == "opt-curso"
    assert opts["Hecho"] == "opt-hecho"
    assert board._option_map(raw, "inexistente") == {}


def test_project_field_map(monkeypatch):
    monkeypatch.setattr(subprocess, "run",
                        lambda *a, **k: _fake_proc(json.dumps(FIELDS_JSON)))
    fields = board._project_field_map()
    assert fields["Status"] == "PVTT_lADO"
    assert fields["Horas"] == "PVTSSF_lADO"


def test_field_value_text_y_number():
    item = {"fields": [{"name": "Prioridad", "text": "A"},
                       {"name": "Horas", "number": 5}]}
    assert board._field_value(item, "Prioridad") == "A"
    assert board._field_value(item, "Horas") == "5"
    assert board._field_value(item, "Sprint") == ""


def test_field_value_claves_superiores_formato_gh():
    """gh project item-list expone los campos como claves superiores en minúscula."""
    item = {"id": "PVTI_x", "title": "[I-01] Repositorio",
            "status": "En curso", "horas": 3, "prioridad": "A",
            "sprint": "1", "área": "infra", "assignees": ["alain8123"]}
    assert board._field_value(item, "Horas") == "3"
    assert board._field_value(item, "Prioridad") == "A"
    assert board._field_value(item, "Sprint") == "1"
    # «Área» con tilde se encuentra pidiendo «Area» o «Área»
    assert board._field_value(item, "Área") == "infra"
    assert board._field_value(item, "Area") == "infra"
    assert board._field_value(item, "Status") == "En curso"
    assert board._field_value(item, "Inexistente") == ""


def test_field_value_prefiere_clave_superior():
    item = {"horas": 7, "fields": [{"name": "Horas", "number": 9}]}
    assert board._field_value(item, "Horas") == "7"


def test_backlog_id_desde_titulo():
    item = {"title": "[I-07] Android: descifrar imagen por la API"}
    assert board._backlog_id(item) == "I-07"
    assert board._backlog_id({"title": "Sin identificador"}) == "?"


def test_assignees_acepta_strings_y_dicts():
    assert board._assignees({"assignees": ["jairoben"]}) == ["jairoben"]
    assert board._assignees({"content": {"assignees": [{"login": "alain8123"}]}}) == ["alain8123"]
    assert board._assignees({}) == []


def test_option_map_sin_status_devuelve_vacio():
    assert board._option_map(json.dumps({"fields": []}), "Status") == {}


def test_error_de_auth_sugiere_scope(monkeypatch):
    def fake_run(cmd, **kwargs):
        return _fake_proc("", "HTTP 403: missing scope project", 1)

    monkeypatch.setattr(subprocess, "run", fake_run)
    with pytest.raises(board.GhError) as exc:
        board.run_gh(["project", "view"])
    assert "gh auth refresh -s repo,project,read:org,workflow" in str(exc.value)


def test_gh_ausente_instalacion(monkeypatch):
    def fake_run(cmd, **kwargs):
        raise FileNotFoundError("gh")

    monkeypatch.setattr(subprocess, "run", fake_run)
    with pytest.raises(board.GhError) as exc:
        board.run_gh(["status"])
    assert "winget install GitHub.cli" in str(exc.value)


def test_parser_status_y_move():
    args = board.build_parser().parse_args(["start", "3"])
    assert args.cmd == "start" and args.number == 3
    args = board.build_parser().parse_args(["--dry-run", "status"])
    assert args.dry_run and args.cmd == "status"


def test_numero_no_positivo_rechazado():
    with pytest.raises(SystemExit):
        board.build_parser().parse_args(["done", "0"])


def test_linked_pr_en_verde(monkeypatch):
    prs = [{"number": 7, "title": "[I-01] workflow", "headRefName": "feature/i01-workflow",
            "statusCheckRollup": [{"conclusion": "success"}]}]
    monkeypatch.setattr(subprocess, "run",
                        lambda *a, **k: _fake_proc(json.dumps(prs)))
    num, ci = board._linked_pr(1, dry_run=False)
    assert num == "7" and ci == "CI verde"
