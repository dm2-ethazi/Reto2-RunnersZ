# Pruebas de herramientas

## Uso

1. Desde la raíz ejecuta `uv run --python 3.12 --with pytest python -m pytest scripts/tests -q`.
2. Para comprobar solo el hook utiliza la misma orden con `scripts/tests/test_commit_message.py`.
3. Revisa los fallos antes de preparar cualquier commit; estas pruebas no crean commits.

## Cómo funciona

test_board.py simula las respuestas de la CLI de GitHub sin llamar a la red.
test_commit_message.py comprueba mensajes válidos, rechazo del formato antiguo,
el límite de longitud, cuerpo opcional y códigos de salida de la CLI. Los archivos
de mensajes se crean en directorios temporales de pytest y se conservan intactos.

## Qué utiliza

Python, pytest y la biblioteca estándar; no necesita credenciales reales.
La orden con uv es la misma que ya usa .pre-commit-config.yaml.

## Por qué se organiza así

Comprueba la retirada del validador antiguo y la compatibilidad con
reglas/AGENTS.md §4, conforme a la validación local del pliego §4.13.
El pliego y el contrato están fuera del repositorio, en reglas/docs.
