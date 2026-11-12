"""Comprueba la compatibilidad del hook con las normas y su salida como CLI."""

import subprocess
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import check_commit_message as hook  # noqa: E402


@pytest.mark.parametrize("message", [
    "refactor(desktop): reorganiza el cliente",
    "docs: actualiza la guía (REQ-012)",
    "feat(api): añade la operación\n\nExplicación del requisito.",
    "fix: " + "a" * 67,
    "chore: limpia el código\n\n" + "contexto " * 30,
])
def test_accepts_documented_messages(message):
    assert hook.validate(message) == []


@pytest.mark.parametrize("message", [
    "",
    "REFACTOR: Reorganiza el cliente",
    "hotfix: cambia el cliente",
    "feat(): añade el cliente",
    "feat: ",
    "feat: Añade el cliente",
    "feat: añade el cliente.",
    "fix: " + "a" * 68,
    "fix: corrige el cliente ",
    "fix: corrige el cliente\nCuerpo sin separación",
    "fix: varios cambios",
])
def test_rejects_messages_outside_rules(message):
    assert hook.validate(message)


def test_cli_checks_bom_file_without_changing_it(tmp_path):
    path = tmp_path / "message.txt"
    path.write_text("refactor(desktop): limpia los paquetes", encoding="utf-8-sig")
    original = path.read_bytes()
    result = subprocess.run(
        [sys.executable, str(Path(hook.__file__)), str(path)],
        capture_output=True,
        check=False,
    )
    assert result.returncode == 0
    assert path.read_bytes() == original


def test_cli_reports_invalid_message(tmp_path):
    path = tmp_path / "message.txt"
    path.write_text("CHORE: formato anterior", encoding="utf-8")
    result = subprocess.run(
        [sys.executable, str(Path(hook.__file__)), str(path)],
        capture_output=True,
        check=False,
    )
    assert result.returncode == 1
    assert result.stderr
