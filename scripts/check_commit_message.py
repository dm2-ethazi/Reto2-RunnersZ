"""Valida un mensaje local sin crear commits ni comunicarse con GitHub."""

import re
import sys
from pathlib import Path


TYPES = {"feat", "fix", "docs", "style", "refactor", "test", "chore",
         "perf", "build", "ci", "revert"}
SUBJECT = re.compile(
    r"^(?P<type>[a-z]+)(?:\((?P<scope>[^()\r\n]+)\))?: (?P<description>\S.*)$"
)


def validate(message: str) -> list[str]:
    """Aplica el apartado 4 de reglas/AGENTS.md al asunto y al cuerpo."""
    lines = message.splitlines()
    if not lines or not lines[0].strip():
        return ["El asunto es obligatorio."]

    subject = lines[0]
    errors = []
    match = SUBJECT.fullmatch(subject)
    if not match or match["type"] not in TYPES:
        errors.append("Usa tipo(ámbito opcional): descripción con un tipo permitido.")
    else:
        description = match["description"]
        if description[0].isupper():
            errors.append("Empieza la descripción en minúscula e imperativo.")
        if description.endswith("."):
            errors.append("El asunto no debe terminar en punto.")
        # La gramática no demuestra el modo imperativo; sí evita mensajes vagos conocidos.
        if re.match(
            r"^(arreglado|cambiado|corregido|corrección|varios cambios)\b",
            description,
            flags=re.IGNORECASE,
        ):
            errors.append("Describe una acción concreta en imperativo.")

    if len(subject) > 72:
        errors.append("El asunto supera 72 caracteres.")
    if subject != subject.strip():
        errors.append("El asunto contiene espacios exteriores.")
    if len(lines) > 1 and lines[1] != "":
        errors.append("Separa el cuerpo con una línea en blanco.")
    return errors


def main() -> int:
    """El hook recibe la ruta del mensaje; no modifica el archivo ni el índice."""
    if len(sys.argv) != 2:
        print("Uso: python scripts/check_commit_message.py COMMIT_EDITMSG")
        return 1
    errors = validate(Path(sys.argv[1]).read_text(encoding="utf-8-sig"))
    for error in errors:
        print(error, file=sys.stderr)
    return int(bool(errors))


if __name__ == "__main__":
    raise SystemExit(main())
