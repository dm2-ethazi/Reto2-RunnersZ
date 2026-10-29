# Instrucciones de mantenimiento del backend

- Responder y documentar en español; seguir las normas de `reglas` y el contrato v2.
- Petición expresa de Alain: cada cambio de código, configuración o comportamiento
  debe actualizar `backend/README.md` en el mismo commit. Documentar uso, motivo,
  pruebas reales y pendientes. El README raíz debe mantenerse vacío.
- Mantener `main.py` como composición; separar HTTP, servicios y modelos.
- Documentar funciones y clases con docstrings y comentarios que expliquen el porqué.
- No afirmar cumplimiento ni pruebas que no se hayan ejecutado.
- Informar a Alain de cualquier funcionalidad extra al contrato/pliego.
- Commits: `TIPO: Asunto en presente imperativo`; asunto de máximo 50 caracteres.
- No trabajar en main/develop, no sobrescribir cambios ajenos y no versionar secretos.
