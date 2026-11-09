import re
import sys
from pathlib import Path

TYPES = {
    "ASSETS",
    "BUILD",
    "CHORE",
    "CI",
    "CONFIG",
    "DB",
    "DEPS",
    "DOCS",
    "FEAT",
    "FIX",
    "HOTFIX",
    "MERGE",
    "PERF",
    "REFACTOR",
    "REVERT",
    "SECURITY",
    "STYLE",
    "TEST",
    "UI",
    "WIP",
}


def validate(message: str) -> list[str]:
    lines = message.splitlines()
    errors = []
    if not lines or not lines[0].strip():
        return ["El asunto es obligatorio."]
    subject = lines[0]
    prefix, separator, description = subject.partition(": ")
    if not separator or prefix not in TYPES or not description.strip():
        errors.append("Usa TIPO: descripci├│n con un prefijo permitido en may├║sculas.")
    if len(subject) > 50:
        errors.append("El asunto supera 50 caracteres.")
    if subject != subject.strip():
        errors.append("El asunto contiene espacios exteriores.")
    if len(lines) > 1 and lines[1] != "":
        errors.append("Separa el cuerpo con una l├¡nea en blanco.")
    if any(len(line) > 72 for line in lines[2:]):
        errors.append("El cuerpo supera 72 caracteres por l├¡nea.")
    if re.match(
        r"^(arreglado|cambiado|corregido|correcci├│n|varios cambios)\b",
        description,
        flags=re.IGNORECASE,
    ):
        errors.append("Redacta en presente e imperativo, sin mensajes vagos.")
    return errors


def main() -> int:
    if len(sys.argv) != 2:
        print("Uso: python scripts/check_commit_message.py COMMIT_EDITMSG")
        return 1
    errors = validate(Path(sys.argv[1]).read_text(encoding="utf-8-sig"))
    for error in errors:
        print(error, file=sys.stderr)
    return int(bool(errors))


if __name__ == "__main__":
    raise SystemExit(main())
