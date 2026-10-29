# RunnersZ · Reto 2 · Fase Discovery

Proyecto del Reto 2 para el cliente **Hogwarts**. Fecha de entrega: **12 de noviembre de 2026**.

Arquitectura v2.0 (contrato `docs/contrato/Contrato_Tecnico_RunnersZ_v2.md`):

- **Servidor** Python (FastAPI) en **Docker Compose**: concentra toda la lógica de cifrado y expone la API HTTP.
- **Cliente de escritorio** JavaFX: usa la API solo por HTTP.
- **App Android**: usa la API solo por HTTP.
- **Minijuego Unity 6.3**: autónomo (no usa la API).

## Estructura del monorepo

| Carpeta | Contenido |
|---|---|
| `backend/` | Servidor FastAPI + Docker Compose |
| `desktop/` | Cliente de escritorio JavaFX |
| `android/` | Aplicación Android |
| `unity/` | Minijuego Unity 6.3 |
| `scripts/` | Utilidades del equipo (p. ej. `board.py` del tablero) |
| `docs/` | Contrato, workflow, planificación, plan de pruebas, evidencias |

## Estado

**Reinicio del repositorio (29-oct-2026).** Se arranca de cero: `main` contiene solo la base y la
estructura vacía; el material de la v1 rechazada se conserva como material reutilizable, no como
referencia. El trabajo de integración vive en la rama `prototype/runnersz`.

## Equipo

- **Alain** (`@GH_ALAIN`): servidor, cliente JavaFX, CI/hooks y documentación de servidor/escritorio.
- **Jairo** (`@jairoben`): Android, Unity y arranque del repositorio.

Flujo de trabajo: ver `docs/WORKFLOW.md`. Tablero: <https://github.com/orgs/dm2-ethazi/projects/14>.

## Estructura del backend y documentación

`main.py` compone la aplicación; `routes.py` atiende HTTP; `crypto.py` y `text.py` encapsulan algoritmos. Los módulos incluyen docstrings en español con parámetros, retornos y errores. Configuración, autenticación y modelos auxiliares siguen pendientes de integrar con las rutas.

2026-10-29 — feature/i02-api-base: separa HTTP y documenta el código. Esta reorganización conserva el comportamiento existente y no acredita todavía cumplimiento completo de §5.3/§6.2 ni recepción en streaming.
