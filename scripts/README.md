# Herramientas del repositorio

## Uso

1. Desde la raíz del repositorio ejecuta `python scripts/check_branch.py` para comprobar la protección de la rama sin hacer cambios.
2. Para comprobar un mensaje escrito en un archivo local ejecuta `python scripts/check_commit_message.py ruta-del-mensaje.txt`; por ejemplo, `refactor(desktop): organiza el cliente`.
3. Ejecuta `uv run --python 3.12 --with pytest python -m pytest scripts/tests -q` para las pruebas existentes y las del hook.
4. `python scripts/board.py --help` explica las operaciones de tablero. Los subcomandos que mueven tarjetas modifican GitHub y requieren autorización antes de ejecutarse.

## Cómo funciona

`check_branch.py` consulta la rama actual y bloquea main/develop.
`check_commit_message.py` valida un archivo con Conventional Commits según
reglas/AGENTS.md: tipo permitido en minúsculas, ámbito opcional, asunto de hasta
72 caracteres, descripción iniciada en minúscula sin punto final y cuerpo opcional
separado por una línea en blanco. La revisión humana comprueba el sentido imperativo.
Ninguno de estos comprobadores crea commits ni hace push.
`board.py` conserva la configuración del Project 14; no se modificó ni se operó el tablero.

## Qué utiliza

Python y su biblioteca estándar para los comprobadores; pytest y uv para la orden
de pruebas usada por los hooks. board.py necesita la CLI de GitHub autenticada.
Los hooks existentes están definidos en .pre-commit-config.yaml.

## Por qué se organiza así

Mantiene la trazabilidad y validación del pliego §4.7 y §4.13 y del contrato §11.
El formato de mensajes sigue el apartado 4 de las normas externas en
`C:\Users\dm2\ia\reglas\AGENTS.md`. No se añaden dependencias ni se cambia el tablero.

## Validación de esta limpieza

Las 41 pruebas de `scripts/tests` pasan con Python 3.12. El marcador vacío
`.gitkeep` se retira porque esta carpeta contiene herramientas y documentación.
No se han ejecutado comandos que creen commits o modifiquen el tablero.
