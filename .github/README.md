# Configuración de colaboración en GitHub

## Uso

1. Consulta `CODEOWNERS` para saber qué responsables revisan cada ruta.
2. Para registrar tareas o cambios de contrato utiliza las plantillas de `ISSUE_TEMPLATE`.
3. Quien abra una revisión puede rellenar `pull_request_template.md` con objetivo, validación y evidencias.

## Cómo funciona

Se conservan los responsables existentes y las plantillas; se retiran rutas
de documentación antigua que ya no pertenece al repositorio.
`workflows/` contiene solo un marcador y no ejecuta CI.

## Qué utiliza

Funciones estándar de GitHub: CODEOWNERS y plantillas Markdown. No añade acciones ni dependencias.

## Por qué se organiza así

Conserva la colaboración y trazabilidad del pliego §4.7 y el contrato §11, usando referencias externas al contrato sin duplicar documentación.
El pliego y el contrato están en `C:\Users\dm2\ia\reglas\docs`, fuera del repositorio.
