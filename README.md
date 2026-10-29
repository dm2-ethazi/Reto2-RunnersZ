# RunnersZ · Reto 2 · Fase Discovery

Proyecto del Reto 2 para el cliente **Hogwarts**. Fecha de entrega: **12 de noviembre de 2026**.

Arquitectura v2.0 (contrato [`docs/contrato/Contrato_Tecnico_RunnersZ_v2.md`](docs/contrato/Contrato_Tecnico_RunnersZ_v2.md)):

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

## Cómo trabajamos

- **Protocolo de equipo**: [`docs/WORKFLOW.md`](docs/WORKFLOW.md) (revisiones C.0–C.11).
- **Tablero kanban**: <https://github.com/orgs/dm2-ethazi/projects/14> (Proyecto 14 «RunnersZ»).
  Consultar o moverlo con `python scripts/board.py status|start|review|done <n>`.
- **Backlog**: 19 issues `[I-01]`…`[I-19]`; correspondencia I-NN ↔ #N en
  [`docs/planificacion/indice-issues.md`](docs/planificacion/indice-issues.md).
- **Reglas de commit**: `TIPO: Asunto en presente imperativo` (≤ 50 caracteres),
  `git add` por rutas, revisión cruzada antes de integrar en `main`.

## Estado

**Reinicio del repositorio (29-oct-2026).** Se arranca de cero: `main` contiene solo la base y la
estructura vacía; el material de la v1 rechazada se conserva como material reutilizable, no como
referencia. El trabajo de integración vive en la rama `prototype/runnersz`.

> ⚠️ **`desktop/` y `android/` están en migración hasta I-10 / I-07**: la v1 rechazada no se usa
> como referencia. No copiar código de esas carpetas hasta que esos issues queden en «Hecho».

## Equipo (roles del Anexo I.2)

- **Alain** (`@alain8123`): servidor, cliente JavaFX, CI/hooks y documentación de servidor/escritorio.
- **Jairo** (`@jairoben`): Android, Unity y arranque del repositorio.
