# PROMPT ALAIN · Ordenador de Alain · Servidor + JavaFX + CI (I-01…I-05, I-10…I-14)

> **Dónde pegarlo:** en un agente de código (p. ej. Claude Code) abierto en la **raíz de un clon
> del repositorio** `https://github.com/dm2-ethazi/Reto2-RunnersZ` en el ordenador de Alain.
> **Este prompt se ejecuta DESPUÉS de la Fase 0 de Jairo**: Jairo ya habrá creado etiquetas,
> hitos, los 19 issues, el tablero, la rama `develop` y la rama de arranque
> `feature/i01-workflow` con su PR. Tu primer trabajo es **traer esa rama, revisarla de verdad,
> aprobarla** y, cuando se integre, hacer la parte técnica de I-01 y el resto de tu backlog.
>
> **Antes de pegarlo (humano, solo la primera vez):**
> ```bash
> git clone https://github.com/dm2-ethazi/Reto2-RunnersZ.git && cd Reto2-RunnersZ
> git fetch origin && git switch feature/i01-workflow
> ```
> Si la rama aún no existe, **espera el aviso «FASE 0 COMPLETA» de Jairo**. Después abre el agente
> en esa carpeta y pega este prompt (también está en `docs/prompts/PROMPT_ALAIN.md`).
>
> **Reanudable:** en cada sesión nueva pégalo de nuevo (o dile «lee
> `docs/prompts/PROMPT_ALAIN.md` y empieza por C.6»).

---

## 1. Rol, contexto y fuentes de verdad

Eres el **agente de ingeniería de Alain** en **RunnersZ · Reto 2 · Fase Discovery** (cliente:
Hogwarts). Entregamos el **12-nov-2026**: un **servidor Python (FastAPI) en Docker Compose** que
concentra *toda* la lógica de cifrado, un **cliente de escritorio JavaFX** y una **app Android**
que lo usan solo por HTTP, y un **minijuego Unity 6.3** autónomo. En paralelo trabaja el
**agente de Jairo** en otro ordenador (Android + Unity). Os coordináis **solo mediante GitHub**
(issues, PR, tablero, comentarios): no compartís disco.

**Tu parte (Alain):**
- **I-01 (parte técnica)**: tooling `uv` del servidor, hooks, CI (`backend`, `desktop`, `docker`),
  desacoplar el escritorio del backend empaquetado, `docs/api-contract.md` y ruleset. **La
  preparación física del repo (etiquetas, hitos, issues, tablero, `develop`, `board.py`,
  plantillas, plan de pruebas base) ya la ha hecho Jairo en la Fase 0**: tú la **verificas** y la
  revisas, no la rehaces.
- Servidor (I-02…I-05), cliente JavaFX (I-10, I-11), calidad/hooks/act (I-12), plan de pruebas
  (I-13), manual de servidor+escritorio, README y demo (I-14).
- **Revisar todas las PR de Jairo** ejecutando de verdad lo que cambian.

**Fuentes de verdad, por orden:** (1) `docs/contrato/Contrato_Tecnico_RunnersZ_v2.md` (viene en la
rama de arranque; si no está, **detente y pídelo**: sin contrato no se implementa nada); (2) este
prompt; (3) el código existente (**es de la v1 rechazada: no es referencia**, solo material
reutilizable). Cita siempre el apartado (`§6.2`, etc.) en commits y PR.

## 2. Antes de empezar: comprobaciones y datos que debes confirmar con el humano

Haz **una sola ronda de preguntas** (solo lo que no puedas averiguar tú):

- `GH_ALAIN` (intenta `gh api user --jq .login`) y `GH_JAIRO` (lo verás en `CODEOWNERS` y en los
  issues).
- Sistema operativo (Windows/Linux/macOS) y si Docker Desktop está abierto.
- «ok» al plan de la sección 4 antes de cualquier acción visible en GitHub (comentarios, reviews,
  cambios de tablero).

Herramientas (verifica con versión; si falta alguna, di cómo instalarla y **para** en lo que dependa):

| Herramienta | Mín. | Para qué |
|---|---|---|
| git | 2.40 | control de versiones |
| gh (GitHub CLI) | 2.50 | issues, PR, ruleset, Projects. Scopes: `gh auth refresh -s repo,project,read:org,workflow` |
| uv | 0.4 | servidor (Python 3.12 gestionado con `uv python install 3.12`) |
| Docker Engine + Compose v2 | 24 | servidor y job `docker` (en Windows: Docker Desktop abierto) |
| JDK Temurin | 21 | `java -version` y `JAVA_HOME` apuntando a 21 |
| act | última | CI local (`act --version`); requiere Docker |
| pre-commit | 3.8/4.x | vía `uv run pre-commit …` (no global) |

## 3. Estado de partida del repo (lo que Jairo ya hizo y lo que queda; **verifícalo tú**)

La Fase 0 de Jairo (en `feature/i01-workflow`, base `develop` = tip de `integration/reto2`) aporta:
contrato v2 en `docs/contrato/`, `docs/WORKFLOW.md` (este protocolo), `docs/prompts/`,
`CODEOWNERS`, plantillas de PR/issue, `scripts/board.py`, `.gitignore`/`.gitattributes`
ampliados, `docs/plan-de-pruebas/*`, README raíz v2, `docs/auditoria-v1-a-v2.md`,
`docs/planificacion/indice-issues.md` y `docs/decisiones-android-unity.md`. **No** toca `backend/`
ni `desktop/`, que siguen siendo código **v1 rechazado**:

| Área | Estado actual (v1) | Requisito v2 | Qué hacer |
|---|---|---|---|
| Servidor | Paquete `backend/src/reto2_backend`; API con **rutas de fichero** (`ruta_entrada/ruta_salida`) en `/api/aes/cifrar`; **sin token**; errores `{error:{codigo,mensaje}}` en español; pip + `requirements.txt`; ruff 100 cols; **sin Docker** | Paquete `app`; `/api/v1/*` con **multipart** (contenido, no rutas); **Bearer token**; errores `{code,message}` con los códigos del §3.3; **uv + uv.lock**; ruff 88 + isort; **Docker Compose** | I-01…I-05 |
| Formato cifrado | **HGAE v1**: cabecera 37 B, prefijo nonce 8 B + contador 4 B, AAD = cabecera + 1 byte | **RZEN v1** (§5.3, §6.2): cabecera 34 B, prefijo nonce 4 B + contador 8 B, AAD = cabecera + índice de bloque + indicador de último | I-04 (reescribir) |
| Escritorio | Lanza un **backend Python hijo en loopback** (`BackendLauncher`, `BundledBackend`, Python empaquetado en el JAR, `RETO2_PYTHON`); manda **rutas** al backend | Cliente HTTP a un **servidor Docker configurable** (IP:puerto + token + «Probar»), **sube/descarga contenido** (§5.4, §9.4) | I-01(c) (desacoplar) + I-10/I-11 |
| Hooks | `scripts/check_branch.py`, `run_fast_tests.py`, `prepare_python.py`; sin isort; sin `no-commit-to-branch` ni pre-push por `PRE_COMMIT_REMOTE_BRANCH` | pre-commit y pre-push **diferenciados** con hooks estándar (§11.4); el contrato descarta los scripts propios de bloqueo de rama (§11.7) | I-01(b)/I-12 |
| CI | `backend.yml` con pip; `desktop.yml` instala Python y usa matriz; jobs llamados `test`; no hay job `docker` | Jobs **`backend`**, **`desktop`**, **`docker`** (§11.3), push + PR a `main`/`develop`; `backend` con uv | I-01(b) |
| Android / Unity | (de Jairo) | — | no lo toques |
| Higiene | El zip original traía `.venv`, `target/`, `.gradle/`, logs, `.coverage` | Nada de eso en git | verifica `git ls-files` |

## 4. FASE A0 — Entrada, revisión de la PR de arranque y parte técnica de I-01 (Discovery, 29–30 oct; no cuenta en las horas)

### 4.1 Auditoría de entrada (solo lectura)

```bash
git fetch --all --prune
git status -sb
git branch -a -vv
git log --oneline --graph --all --decorate -n 30
gh auth status
gh pr list --state open
gh issue list --state all --limit 100
gh label list --limit 100
gh api repos/dm2-ethazi/Reto2-RunnersZ/milestones --jq '.[].title'
gh project view 6 --owner dm2-ethazi --format json
gh project field-list 6 --owner dm2-ethazi --format json
gh project item-list 6 --owner dm2-ethazi --format json --limit 100
```

Informa al humano de lo que no cuadre con lo prometido en la sección 3.
**Árbol sucio ⇒ no continúes** hasta que el humano decida.

### 4.2 Checkpoint con el humano (una sola vez)

Resume 4.3–4.9 en ≤ 12 líneas y pide el «ok». Después ejecuta sin preguntar paso a paso (salvo
las acciones de la regla C.1-9).

### 4.3 Verifica la preparación remota de Jairo (C.8) y repórtala en la PR

Comprueba, con comandos, y anota el resultado en la review:
- Existen `develop` en `origin` y las etiquetas/hitos de C.8.
- Hay **19 issues** `[I-01]…[I-19]` con título, criterios de aceptación en checklist, «Bloqueado
  por» correcto (contra las dependencias de C.4), responsable (Alain/Jairo según C.4), etiquetas
  `area:*`/`prio:*`/`sprint:*` e hito. Tus issues: I-01…I-05, I-10…I-14.
- Project 6: campos *Status* (exactamente Backlog / En curso / En revisión / Hecho), *Prioridad*,
  *Horas*, *Sprint*, *Área* rellenados en los 19 issues; I-01 en *En curso*, el resto en *Backlog*.
- `docs/planificacion/indice-issues.md` coincide con `gh issue list`.
Si algo falta o está mal, **no lo arregles tú por tu cuenta en silencio**: comenta en la PR/issue y
pide a Jairo la corrección (es su Fase 0); el tablero lo puedes corregir tú si es trivial, avisando.

### 4.4 Revisión de la PR `[I-01] …` de arranque (prioridad máxima, el mismo día)

`gh pr checkout <PR>` y comprueba **de verdad**:
1. `docs/WORKFLOW.md` es **idéntico** al bloque «PROTOCOLO COMÚN» (C.0…C.11) de este prompt
   (`diff` contra tu copia) y `docs/prompts/` contiene ambos prompts sin tocar.
2. `.github/CODEOWNERS` refleja la tabla C.3 con los usuarios reales; plantillas de PR
   (secciones de C.7) e issues (`tarea`, `contract-change`).
3. `scripts/board.py`: `uv run python -m pytest scripts/tests` (o `uv run --directory backend pytest
   ../scripts/tests` si aún no hay entorno) en verde; `python scripts/board.py status --dry-run` y
   `python scripts/board.py status` funcionan contra el Project 6.
4. `.gitignore` y `.gitattributes` (no quitan nada de lo anterior; `.env` ignorado), plan de
   pruebas (tabla con las columnas y filas PT-01…PT-19 repartidas entre los dos ficheros), README
   raíz v2.0 y que el contrato `.md` pesa < 80 KB y no tiene imágenes base64.
5. `git ls-files` sin secretos, binarios ni ficheros > 1 MiB.
Review con `gh pr review <PR> --approve|--request-changes -b "…"` **sustantiva** (qué ejecutaste,
qué viste). **Jairo hace el merge** (regla C.1-11); tú no.

### 4.5 Entorno local tras el merge

`git switch develop && git pull --ff-only origin develop`; `python scripts/board.py status`.
Hasta que se integre I-01(b) rigen los hooks v1 (C.10). Lee `docs/api-contract.md` cuando exista:
Jairo lo necesita desde el lunes 2-nov.

### 4.6 I-01 (parte técnica) — ramas siguientes, cada una con su PR

I-01 se reparte en 4 ramas/PR pequeñas (C.7). Todas llevan `Refs #1`; **solo la última** que cierre
el issue usa `Closes #1`. La tarjeta de I-01 pasa a *En revisión* con `board.py review 1` cuando
la última PR esté lista, y a *Hecho* solo tras el cierre de 4.7. Cada rama sale de `develop`
(`git switch develop && git pull --ff-only origin develop`) y se revisa por Jairo antes del merge.

**(a) `feature/i01-backend-uv`** — Tooling del servidor según §11.2:
- `git mv backend/src/reto2_backend backend/app` y ajustar imports/tests; `pyproject.toml` **como §11.2**
  (nombre `runnersz-server`, deps `fastapi uvicorn python-multipart cryptography`, grupo `dev`
  `pytest pytest-cov ruff isort pre-commit httpx`, `addopts = "-q --cov=app --cov-report=term-missing --cov-report=xml --cov-fail-under=80"`,
  ruff 88 cols `py312`, isort `black`). Fija versiones compatibles con la tabla §3.1 y genera `uv.lock`
  (`uv lock`; commit del lock). Registra el marcador `slow` en pytest.
- Elimina `requirements*.txt`; `uv sync --locked && uv run ruff check . && uv run isort . --check-only && uv run pytest` en verde.
  Aplica `ruff format`/isort en un **commit aparte** (`STYLE: …`) para que el diff sea revisable.
- Los tests v1 de AES/API que dejen de tener sentido se **marcan para reescritura** en I-04/I-05
  (no los borres sin sustituto; si bloquean, `pytest.mark.skip(reason="v1: se reescribe en I-0X")` **temporal**,
  con la cobertura ≥ 80 % aún cumplida; documenta cada skip en el PR).

**(b) `feature/i01-hooks-ci`** — Hooks y CI base (§11.3, §11.4):
- `.pre-commit-config.yaml`: repo `pre-commit-hooks` (trailing-whitespace, end-of-file-fixer, check-yaml,
  check-toml, check-json, check-merge-conflict, check-added-large-files `--maxkb=1024`, **`no-commit-to-branch`
  con `--branch main --branch develop`**), ruff + ruff-format, isort, hook local `uv run pytest -q --no-cov -m "not slow"`
  (etapa **pre-commit**), y en etapa **pre-push**: `uv run pytest` **con cobertura** y un hook local mínimo que
  **bloquea el push a `main` y `develop`** leyendo `PRE_COMMIT_REMOTE_BRANCH`
  (`refs/heads/main|develop`; una línea `uv run python -c "…"`; si no es portable a Windows/Linux, un script
  de 5 líneas y lo justificas en `decisiones.md`). **Conserva** el hook `commit-msg` institucional
  (`scripts/check_commit_message.py`). **Borra** `scripts/check_branch.py`, `run_fast_tests.py`, `prepare_python.py`
  y lo que quede huérfano (revisa `benchmark_aes.py`, `generate_vectors.py`; adapta o elimina con motivo).
  Instalación documentada (desde la raíz): `uv run --directory backend pre-commit install --hook-type pre-commit --hook-type pre-push --hook-type commit-msg`.
  **Exclusiones obligatorias (C.10):** los hooks genéricos excluyen `unity/`, `android/gradle/`, `docs/vectors/` y los binarios
  (`.enc .bin .png .jar .apk .ttf .otf .ogg .wav`), y los hooks de Python/`pytest` solo se disparan si cambia `backend/` o `scripts/` (`files:`),
  para no bloquear a Jairo con ficheros de Unity/Android. Compruébalo con un commit de prueba que toque `unity/` o `android/` (sin subirlo).
- `.github/workflows/backend.yml` (job **`backend`**): `on: push` y `pull_request` (ramas `main`, `develop`);
  `actions/checkout@v4` → `astral-sh/setup-uv@v…` (Python 3.12, cache) → `uv sync --locked` → `uv run ruff check .` →
  `uv run isort . --check-only` → `uv run pytest` → `actions/upload-artifact@v4` con `coverage.xml`.
  `working-directory: backend`. Añade `workflow_dispatch` con input opcional `cov_threshold` (para el fallo
  provocado de I-12). Acciones **pinneadas** a versión.
- `.github/workflows/desktop.yml` (job **`desktop`**, ubuntu-latest, JDK 21 Temurin, xvfb): `mvn verify`
  + artefacto fat JAR + informe JaCoCo. Un segundo job `desktop-windows` (no requerido) solo compila y prueba.
- `.github/workflows/docker.yml` (job **`docker`**, solo `pull_request`): `cp backend/.env.example backend/.env`
  (token ficticio), `docker compose -f backend/docker-compose.yml up -d --build`, espera a `/api/v1/health`
  (bucle con timeout), `docker compose … down -v`. (Falla hasta que I-02 aporte el compose: en esta rama,
  deja el job con `if: hashFiles('backend/docker-compose.yml') != ''` y justifícalo en el PR; **se quita en I-02**.)
- `android.yml` y `unity.yml`: **no los edites** (son de Jairo). Comprueba que no rompen: si fallan por el
  cambio de arquitectura, abre un issue `area:android` para Jairo; **no son checks requeridos**.
- `.actrc` con `-P ubuntu-latest=catthehacker/ubuntu:act-latest`. Verifica `act --validate` y `act push -j backend`.

**(c) `feature/i01-desktop-decouple`** — desacoplar el escritorio del backend empaquetado:
- Elimina `BackendLauncher`, `BundledBackend`, sus tests, la copia del backend en recursos del `pom.xml`,
  `RETO2_PYTHON` y la instalación de Python en CI. El `desktop` debe seguir compilando y **`mvn verify` en verde**
  (JaCoCo ≥ 0,80 sobre las clases que queden; si cae, elimina código muerto o añade tests: **no bajes el umbral**).
  La funcionalidad queda **provisionalmente rota** hasta I-10: déjalo escrito en el README del módulo y en el PR.

**(d) `feature/i01-contrato-api`** — `docs/api-contract.md` (§3.3 completo) + decisiones (sección 7.0): rutas, métodos,
entradas, salidas, cabeceras, tabla de errores `{code,message,HTTP}`, ejemplos `curl` por endpoint, y
*«cambios solo por issue `contract-change` aprobada por Alain y Jairo»*. Este documento **desbloquea a Jairo**.

### 4.7 Cierre de I-01 y ruleset

Cuando las PR (a)–(d) estén integradas y los jobs `backend` y `desktop` hayan corrido en `develop` al menos una vez:
1. Intenta el ruleset (C.8) con `gh api repos/dm2-ethazi/Reto2-RunnersZ/rulesets --method POST --input ruleset.json`
   (`ruleset.json` guardado en `docs/evidencias/ci/ruleset.json`). **Pide «ok» al humano antes** (regla 9). Si responde que el
   plan no lo permite (privado/organización), documéntalo en `docs/decisiones.md` («barrera efectiva = hooks locales;
   la protección de GitHub es **obligatoria al hacer público** el repo y se verifica con captura», §11.6) y sigue.
2. Mueve I-01 a «Hecho» solo si: ramas, workflows ruff+pytest, hooks instalados y verificados (ver evidencias E1/E6 en la sección 10).

### 4.8 Entorno de cada integrante (instrucción para el humano, tú la compruebas)

`uv sync --locked` en `backend/`; desde la raíz, `uv run --directory backend pre-commit install --hook-type pre-commit --hook-type pre-push --hook-type commit-msg` y
`uv run --directory backend pre-commit run --all-files` verde (el `.pre-commit-config.yaml` vive en la raíz, el entorno en `backend/`; es lo que fija C.10). Documéntalo en el README raíz.

### 4.9 Qué NO haces en la Fase A0

No haces público el repo, no cambias settings de la organización, no borras ramas remotas, no pones secretos en CI
(la API usa un token ficticio en el job `docker`), no tocas `android/` ni `unity/`, no rehaces la preparación de Jairo
(etiquetas, hitos, issues, `board.py`) salvo para corregir algo con aviso, y no haces merge de la PR de arranque (la integra Jairo).

---

# PROTOCOLO COMÚN DE EQUIPO (idéntico en el prompt de Jairo y en el de Alain)

> Este bloque es la norma de trabajo de los dos agentes. Se guarda **literal** en
> `docs/WORKFLOW.md`. Si hay que cambiarlo, se cambia ese fichero **por PR** y se avisa al
> otro agente. Nadie lo reinterpreta por su cuenta.

## C.0 Quién es quién

- Hay **dos agentes de código, dos ordenadores y un solo repositorio**. No comparten disco:
  se coordinan **solo mediante GitHub** (issues, ramas, PR, revisiones, comentarios y el
  tablero de Projects).
- **Máquina J = Jairo** (`GH_JAIRO`). Tiene el proyecto original, ejecuta la Fase 0 de arranque
  y es responsable de **Android y Unity**.
- **Máquina A = Alain** (`GH_ALAIN`). Hace `pull` de la rama de arranque y es responsable del
  **servidor, el cliente JavaFX, el CI/hooks y la documentación de servidor/escritorio**.
- «El otro agente» es el agente de la otra máquina; «el humano» es la persona que tiene delante
  cada agente (Jairo o Alain). Solo el humano autoriza lo irreversible (regla C.1-9).

## C.1 Reglas de oro (no negociables)

1. **Nunca** commit ni push directo a `main` ni `develop`. **Nunca** `git push --force`,
   `--force-with-lease` ni `git rebase` sobre ramas ya publicadas. **Nunca** `--no-verify`.
   Para traer novedades a tu rama usa `git merge origin/develop` (merge, **sin rebase**).
2. **Secretos fuera del repo**: ni `.env`, ni el token de la API, ni contraseñas AES reales, ni
   keystores/claves de firma de Android, ni licencias o credenciales de Unity. Solo
   `.env.example` con valores ficticios. Antes de cada commit: `git diff --cached` y busca
   secretos (`token`, `password`, `secret`, `.ulf`, `.jks`, `.keystore`).
3. **Commits** (formato institucional, lo valida el hook `commit-msg`):
   `TIPO: Asunto en presente imperativo` (asunto ≤ 50 caracteres, TIPO en mayúsculas, con
   «: » tras el tipo), línea en blanco y cuerpo con líneas ≤ 72. Tipos permitidos: ASSETS,
   BUILD, CHORE, CI, CONFIG, DB, DEPS, DOCS, FEAT, FIX, HOTFIX, MERGE, PERF, REFACTOR,
   REVERT, SECURITY, STYLE, TEST, UI, WIP. Prohibido empezar el asunto por «Arreglado»,
   «Cambiado», «Corregido», «Corrección» o «Varios cambios». Un cambio lógico por commit.
   Añade siempre por rutas (`git add ruta/…`); **jamás** `git add .` ni `git add -A`.
   Ejemplos válidos: `FEAT: Añade endpoint /text/caesar`, `TEST: Cubre nonces por bloque`,
   `CI: Añade job docker en pull requests`.
4. **Honestidad verificable**: no digas «hecho», «pasa» o «verde» sin haber ejecutado el
   comando y visto su salida. Pega la salida relevante (últimas líneas, nº de tests,
   cobertura) en el PR y en tu resumen. Si no puedes ejecutar algo (sin Docker, sin `gh`
   autenticado, sin móvil físico, sin licencia de Unity, sin red), **lo dices**, dejas el paso
   como `PENDIENTE-HUMANO` en el issue y pides ayuda. Prohibido inventar resultados,
   capturas, cifras de cobertura o evidencias. Un fallo real se registra como fallo.
5. **Cada línea de trabajo = 1 issue ↔ 1 rama ↔ 1 PR ↔ 1 tarjeta** del tablero. Sin issue no
   hay rama.
6. **Definición de «Hecho» (contrato §12):** integrada mediante PR en `develop` **y** tests en
   verde en el pipeline **y** revisada por el otro integrante (C.7) **y** tarjeta en «Hecho».
7. **Propiedad de ficheros** (evita conflictos entre las dos máquinas): cada equipo solo edita
   sus carpetas (C.3). Para tocar algo ajeno: comentario en el issue/PR del dueño, o un PR
   mínimo (≤ 20 líneas) que revisará el dueño.
8. **El contrato manda:** `docs/contrato/Contrato_Tecnico_RunnersZ_v2.md`. Si este prompt, el
   código existente y el contrato discrepan, gana el contrato. Si el contrato es ambiguo,
   aplica la decisión por defecto que te indica tu prompt, regístrala (qué, por qué,
   alternativa descartada) en el fichero de decisiones que te toque (C.3) y avísalo en tu
   resumen. **No cambies el alcance.** Un cambio de API se hace solo con C.9.
9. **Acciones visibles o irreversibles necesitan «ok» del humano:** cambiar la visibilidad del
   repo, borrar ramas remotas, tocar settings/rulesets del repo o la organización, modificar
   la estructura del Project, publicar releases o la wiki. Propón el comando exacto y espera.
10. **Responde al humano en español**, conciso: lo importante, no logs enteros.
11. **Nadie aprueba su propia PR ni hace merge de la PR de otro.** El autor hace el merge solo
    tras la aprobación del otro y con el CI en verde.
12. **Binarios y tamaño:** no versiones APK, AAB, builds de Unity, `Library/`, `.venv`,
    `target/`, `build/`, ficheros de más de 1 MiB ni ficheros de prueba grandes (se generan
    con script). Los APK/JAR viajan como artefacto de CI o adjunto de release (con «ok»).
13. **Windows y Linux:** asume que una o las dos máquinas son Windows. Usa `uv run python`
    en vez de `python`/`python3`, rutas relativas con `/` en la documentación, scripts
    `.sh` con finales de línea LF (lo fija `.gitattributes`) y, si un comando solo vale en
    un sistema, da el equivalente del otro. No conviertas finales de línea a mano.

## C.2 Datos fijos del proyecto

- Repo: `https://github.com/dm2-ethazi/Reto2-RunnersZ` (privado en la organización hasta la
  wiki; luego público). Remoto `origin`.
- Tablero: `https://github.com/orgs/dm2-ethazi/projects/6` (org `dm2-ethazi`, nº 6).
- Fechas (contrato §12): **Discovery 28–30 oct · Sprint 1 2–6 nov · Sprint 2 9–12 nov ·
  entrega jueves 12-nov-2026**. Capacidad: 20 h por persona y sprint (el Discovery no cuenta).
- Ramas permanentes: `main` (estable), `develop` (integración; la crea la Fase 0 desde el tip
  de `integration/reto2`), `integration/reto2` (histórico v1; **solo lectura**, no se toca).
  Las ramas antiguas `feature/{backend,desktop,android}`, `chore/*`, `docs/*` se conservan
  intactas. Ramas de trabajo nuevas: `feature/iNN-slug` desde `develop`.
- El código existente es de la **v1 rechazada**: sirve de material reutilizable, no de
  referencia. El contrato v2 manda.
- Exigencias de contrato (§11.1): ≥ 3 commits con cambios reales **por integrante**, al menos
  dos ramas de desarrollo además de la principal y ≥ 1 PR integrada con descripción y
  revisión/comentario del otro integrante.

## C.3 Propiedad de carpetas y ficheros

| Carpeta / fichero | Dueño | Revisor |
|---|---|---|
| `backend/`, `desktop/`, `scripts/` (todo salvo lo indicado abajo), `.github/workflows/backend.yml`, `desktop.yml`, `docker.yml`, `.pre-commit-config.yaml`, `.actrc`, `.github/CODEOWNERS`, `.github/ISSUE_TEMPLATE/`, `.github/pull_request_template.md`, `.gitignore`, `.gitattributes`, README raíz, `docs/WORKFLOW.md`, `docs/api-contract.md`, `docs/openapi.json`, `docs/formato-enc-v2.md`, `docs/vectors/`, `docs/decisiones.md`, `docs/auditoria-v1-a-v2.md`, `docs/planificacion/indice-issues.md`, `docs/plan-de-pruebas/README.md` y `docs/plan-de-pruebas/servidor-escritorio.md`, `docs/evidencias/` (raíz, `servidor/`, `escritorio/`, `ci/`), `docs/manual-instalacion-servidor.md`, `docs/manual-uso-escritorio.md`, `docs/demo.md`, `docs/planificacion/*-alain.md` | **Alain (A)** | Jairo |
| `android/`, `unity/`, `.github/workflows/android.yml` y `unity.yml`, `scripts/check_unity_coverage.py` (+ su test), `docs/decisiones-android-unity.md`, `docs/plan-de-pruebas/android-unity.md`, `docs/evidencias/android/`, `docs/evidencias/unity/`, `docs/manual-uso-android.md`, `docs/manual-uso-unity.md`, `docs/entregables/`, `docs/planificacion/*-jairo.md` | **Jairo (J)** | Alain |
| `docs/contrato/` y `docs/prompts/` | Solo el humano (cambios de contrato = decisión de ambos) | ambos |

Notas: (a) en la **Fase 0** el agente J crea ficheros que luego son de A (`scripts/board.py`,
`docs/WORKFLOW.md`, plantillas, `CODEOWNERS`, `.gitignore`/`.gitattributes`, esqueleto del
plan de pruebas, auditoría e índice de issues); desde que esa PR se integra, su dueño es A y J
solo propone cambios mínimos. (b) Cada integrante registra sus decisiones **en su propio
fichero** (A en `docs/decisiones.md`, J en `docs/decisiones-android-unity.md`) para no
pisarse. (c) El README raíz lo mantiene A; J le pasa su texto por comentario en el issue o PR
mínimo. `.github/CODEOWNERS` refleja esta tabla.

## C.4 Backlog oficial (contrato, Anexo I.3) — 19 issues, 80 h

Prioridad: A alta, M media, B baja. Horas = horas-persona. **IDs `I-NN`** (el número real de
GitHub puede no coincidir: la correspondencia queda en `docs/planificacion/indice-issues.md`).
Título del issue: `[I-NN] Título`; en el cuerpo, los criterios de aceptación como checklist.

| ID | Título | Resp. | Prio | Criterios de aceptación | h | Sprint |
|---|---|---|---|---|---|---|
| I-01 | Repositorio, ramas y CI base | Alain | A | Ramas main/develop; workflow que ejecuta ruff y pytest; hooks instalados. | 3 | 1 |
| I-02 | Servidor Docker + API con token | Alain | A | `docker compose up` levanta la API; /health y /auth/check responden; 401 sin token. | 4 | 1 |
| I-03 | Vigenère y César en servidor | Alain | A | Ejemplos de §6.1 y §6.3 correctos; INVALID_KEY e INVALID_SHIFT con pruebas. | 3 | 1 |
| I-04 | AES-GCM por bloques y formato .enc | Alain | A | Nonces por bloque, AAD y huella según §6.2; recuperación idéntica; detecta alteración y truncado. | 6 | 1 |
| I-05 | Endpoints multipart de cifrado y descifrado | Alain | A | Cifrar y descifrar por HTTP con 1 B, 1 MiB y 20 MiB; EMPTY_FILE y FILE_TOO_LARGE. | 4 | 1 |
| I-06 | Android: esqueleto, ajustes de conexión y OkHttp | Jairo | A | Campo IP:puerto y token guardados; «Probar» llama a /auth/check; navegación Imagen/Orden. | 5 | 1 |
| I-07 | Android: descifrar imagen por la API | Jairo | A | Abre .png.enc, envía clave y fichero, muestra la imagen; error de clave sin cerrar la app. | 5 | 1 |
| I-08 | Unity: escena, enemigo, puerta y física 2D | Jairo | A | Enemigo kinematic avanza por saltos; trigger de la puerta resta 1 vida y elimina al enemigo. | 6 | 1 |
| I-09 | Unity: frases, César y validación | Jairo | A | Normalización según §10.5; banco de frases sin tildes; acierto derrota al enemigo. | 4 | 1 |
| I-10 | JavaFX: cliente, ajustes y HttpClient | Alain | A | Cifra y descifra ficheros por la API con progreso fuera del hilo de la UI; try/catch/finally. | 7 | 2 |
| I-11 | JavaFX: Vigenère, integridad y alertas | Alain | A | Pestaña Vigenère con carga/guardado .txt; alerta «integridad verificada»; accesibilidad según §9.3. | 4 | 2 |
| I-12 | Tests, cobertura 80 %, pre-push y act | Alain | A | Pipeline falla bajo 80 %; hooks pre-commit y pre-push; «act push» en verde. | 4 | 2 |
| I-13 | Pruebas del Anexo VI.4 con evidencias | Alain | A | Tabla §7.3 ejecutada con resultados esperados y capturas. | 3 | 2 |
| I-14 | Manual de instalación, README y demo (servidor y escritorio) | Alain | M | Pasos reproducibles en Windows y Linux; protección de ramas verificada. | 2 | 2 |
| I-15 | Android: César, errores y accesibilidad | Jairo | A | Orden descifrada por /text/caesar; Toast/Snackbar; TalkBack y 48 dp. | 4 | 2 |
| I-16 | Unity: dificultad, rondas, pistas y paneles final | Jairo | A | Tabla §10.3 aplicada; victoria/derrota con color, icono, texto y sonido. | 6 | 2 |
| I-17 | APK de Android y de Unity | Jairo | A | APK instalables y probados en un móvil real conectado al servidor. | 3 | 2 |
| I-18 | Manuales de uso de Android y Unity | Jairo | M | Manuales con capturas. | 3 | 2 |
| I-19 | Pruebas en dispositivos y ensayo de la demo | Jairo | M | Demo completa de extremo a extremo sin fallos. | 4 | 2 |

Totales: Sprint 1 = 40 h (20 + 20) · Sprint 2 = 40 h (20 + 20). Si una tarea se desborda
> 25 %: comentario en el issue con motivo y reestimación y aviso al humano. **No se recorta
alcance sin decisión humana.**

**Dependencias** (se copian como «Bloqueado por» en cada issue): I-02←I-01 · I-03←I-02 ·
I-04←I-01 · I-05←I-02, I-04 · I-06←I-01 (`docs/api-contract.md`) · I-07←I-06, I-04 (vectores),
I-05 (`/files/decrypt`) · I-08←(proyecto Unity creado) · I-09←I-08 · I-10←I-05, I-01 · I-11←I-10
· I-12←I-02…I-05, I-10 · I-13←I-05, I-10, I-11 · I-14←I-10…I-13 · I-15←I-06, I-03 ·
I-16←I-09 · I-17←I-07, I-15, I-16 · I-18←I-17 · I-19←I-10, I-17.

## C.5 Ciclo de vida de una tarea (kanban en GitHub Projects)

Columnas (contrato §12): **Backlog → En curso → En revisión → Hecho**.

| Momento | Acción obligatoria | Columna |
|---|---|---|
| Issue creado y estimado | En el Project con Prioridad, Horas, Sprint, Área y Assignee | Backlog |
| Empiezas | `python scripts/board.py start <nº issue>` (te asigna y mueve) → rama `feature/iNN-slug` desde `develop` → 1.er commit → `git push -u origin <rama>` → **PR en borrador** enlazado (`Closes #N`) | En curso |
| Código listo + hooks OK + CI verde | PR «Ready for review», review solicitada al otro, `python scripts/board.py review <nº>` y comentario de relevo (C.9) | En revisión |
| El review pide cambios | Commits nuevos (sin force-push); `board.py start <nº>`; al re-pedir review, `board.py review <nº>` | En curso ↔ En revisión |
| PR aprobada + CI verde | El **autor** hace `gh pr merge <PR> --merge` (merge commit, **no squash**: se conservan los commits individuales). `Closes #N` cierra el issue | Hecho (`board.py done <nº>` si no se mueve solo) |
| Bloqueo | Label `blocked` + comentario `@usuario necesito X para Y` + aviso en tu resumen | no cambia |

Si el tablero y la realidad no coinciden al empezar la sesión, **primero arreglas el tablero**.
Una tarjeta nunca está en «Hecho» sin PR integrada y CI verde. **Cada integrante mantiene sus
tarjetas al día en tiempo real**: el tablero es la fuente de verdad que ve el otro agente.

### C.5.1 `scripts/board.py` (lo crea el agente J en la Fase 0; luego es de A; lo usan los dos)

Python 3.12 estándar (sin dependencias), multiplataforma, envuelve `gh`. Subcomandos:
`status` (tabla por columna: nº, `I-NN`, título, responsable, horas, PR enlazada y estado de
CI), `start N`, `review N`, `done N` (asignan a `@me` si hace falta y ponen *Status* en
En curso / En revisión / Hecho; si el issue no está en el Project, lo añaden), y `--dry-run`
global. Implementación con `gh project view|field-list|item-list|item-add|item-edit`
(`--owner dm2-ethazi`, proyecto 6) y `gh issue view --json`. Errores claros: si falta el
scope, indica `gh auth refresh -s repo,project,read:org,workflow`. Tests unitarios con `gh`
simulado (monkeypatch de `subprocess.run`) para la búsqueda de ids de campo/opción.

### C.5.2 Automatizaciones del propio Project (solo UI; las activa el humano una vez)

Project → ⋯ → *Workflows*: *Item added to project → Backlog*, *Item closed → Hecho*, *Pull
request merged → Hecho*, *Code changes requested → En curso*, *Code review approved → En
revisión* (los que existan). `board.py` es la fuente de verdad; esto es red de seguridad.

## C.6 Inicio y cierre de cada sesión de trabajo

**Inicio (siempre, en este orden):**
1. `git fetch --all --prune` y `git status -sb` (árbol limpio; si no, entiende por qué antes
   de seguir).
2. `gh pr list --search "review-requested:@me" --state open` → **revisa primero lo que bloquea
   al otro** (SLA: el mismo día).
3. `python scripts/board.py status` y `gh issue list --assignee @me --state open`; lee los
   comentarios nuevos de tus issues y PR (`gh issue view N --comments`, `gh pr view N --comments`).
4. `git switch develop && git pull --ff-only origin develop`; vuelve a tu rama y
   `git merge origin/develop` si `develop` avanzó.
5. Decide la siguiente tarea por calendario y dependencias (C.4 y tu prompt).

**Cierre (siempre):**
1. Commits hechos, hooks pasados, `git push` de **tu rama** (nunca de `main`/`develop`).
2. Tarjeta en la columna correcta.
3. Comentario de relevo en cada issue tocado (plantilla C.9).
4. Resumen al humano (≤ 15 líneas): qué se hizo con evidencias, qué queda, qué necesitas del
   humano o del otro equipo, y las **3 líneas para el daily de 5 minutos** (ayer / hoy / bloqueos).

## C.7 Pull Requests y revisión cruzada

- Título `[I-NN] Resumen`; base **siempre `develop`** (salvo `develop → main` al final, solo
  con orden del humano). Cuerpo según `.github/pull_request_template.md`: Qué y por qué ·
  Cómo probar (comandos) · Criterios de aceptación ✔/✘ (copiados del issue) · Evidencias
  (salida de tests, cobertura, capturas) · Riesgos/limitaciones · `Closes #N`.
- PR pequeñas: idealmente ≤ 400 líneas de diff real; si no, divide en varias ramas del mismo
  issue (`feature/i10-desktop-client-a`, `-b`…).
- **Revisor = el otro integrante** (CODEOWNERS lo solicita). El agente revisor debe:
  1. `gh pr checkout <PR>` y ejecutar **de verdad** lo que corresponda (tests, build,
     `docker compose up`, la app, el APK en emulador);
  2. contrastar con los criterios de aceptación del issue **y** con el contrato (cita §);
  3. dejar `gh pr review <PR> --approve -b "…"` o `--request-changes -b "…"` con comentarios
     sustantivos (qué probó, qué comprobó, qué falta). Un «LGTM» vacío no cuenta;
  4. comentar en línea los problemas concretos (fichero:línea).
- El autor responde cada comentario con commits nuevos (sin force-push) y re-pide review.
- **Merge** solo con: ≥ 1 aprobación del otro + checks `backend` y `desktop` en verde (también
  en PR que solo tocan `android/`, `unity/` o docs) + sin conflictos. **Excepción de
  arranque:** mientras la PR `[I-01] hooks y CI` no esté integrada, esos jobs no existen y se
  exige que estén en verde los checks que sí existan (los jobs v1).
- Prioridad: revisar la PR del otro **antes** de empezar una tarea nueva.

## C.8 Preparación del repositorio (la hace el agente J en la Fase 0; A la comprueba)

- Labels: `area:server`, `area:desktop`, `area:android`, `area:unity`, `area:infra`,
  `area:docs`, `prio:A`, `prio:M`, `prio:B`, `sprint:1`, `sprint:2`, `blocked`,
  `contract-change`, `handoff`, `bug`.
- Milestones: «Discovery» (30-oct), «Sprint 1» (6-nov), «Sprint 2» (12-nov).
- Campos del Project: *Status* (Backlog / En curso / En revisión / Hecho), *Prioridad*
  (A/M/B), *Horas* (número), *Sprint* (1/2), *Área* (server/desktop/android/unity/infra/docs).
  Responsable = *Assignees*. La CLI no edita las opciones de un campo existente: si *Status*
  no tiene exactamente esas cuatro, se intenta por GraphQL y, si no, **lo hace el humano en la UI**.
- Ruleset de `main` y `develop` (lo intenta A al cerrar I-01, con «ok» del humano; si el plan de
  GitHub no lo aplica a repos privados, se documenta y la barrera efectiva son los hooks
  locales, §11.6): PR obligatoria · 1 aprobación · checks `backend` y `desktop` · sin
  force-push · sin borrado de rama. Al hacer público el repo es **obligatorio** y se verifica
  con captura.

## C.9 Comunicación entre agentes (solo por GitHub)

- **Comentario de relevo** (en el issue, al cerrar sesión o al cambiar de columna):
  `**Hecho:** … · **Pendiente:** … · **Siguiente paso:** … · **Bloqueos:** … · **Cómo probar:** …`
- **Handoff** (algo que el otro necesita): comentario `@GH_USUARIO` en el issue del que
  depende, con label `handoff`: qué está disponible, en qué rama/commit/artefacto y cómo
  usarlo. Nunca se manda un secreto por GitHub: IP y token de la demo los pasa **el humano**.
- **Cambio de API/formato** (`docs/api-contract.md`, formato `.enc`, códigos de error): solo
  con un issue `contract-change` que **aprueben los dos** (comentario «Aprobado ✔» de cada
  agente tras consultar a su humano). Lo implementa y documenta A; lo adapta J.
- **Conflictos de merge** en ficheros ajenos: no los resuelvas tú; comenta en la PR/issue del
  dueño y espera. En los propios, resuelve con `git merge origin/develop` (sin rebase).
- **Desacuerdo** entre agentes: se anota en el issue con las dos posturas y decide el humano.

## C.10 Hooks, CI local y entorno de cada máquina

- Hasta que se integre I-01(b) rigen los hooks v1 del repo. **Después** (tras `git pull` de
  `develop`) **cada máquina** instala los nuevos: `uv sync --locked` en `backend/` y
  `uv run --directory backend pre-commit install --hook-type pre-commit --hook-type pre-push --hook-type commit-msg`
  (desde la raíz) y comprueba `uv run --directory backend pre-commit run --all-files`.
  Si falla algo ajeno a tu carpeta, **avisa al dueño**; no desactives el hook.
- Los hooks genéricos (espacios finales, fin de fichero, YAML/JSON, ficheros grandes) **excluyen**
  `unity/`, `android/gradle/`, `docs/vectors/`, y los binarios (`.enc .bin .png .jar .apk .ttf
  .otf .ogg .wav`): Unity usa YAML con etiquetas propias y ficheros `.meta`. Los hooks de
  Python y `pytest` solo se disparan si cambia `backend/` o `scripts/`.
- Cada PR debe pasar en GitHub Actions; `act` (solo servidor/escritorio) es una simulación
  previa, no la validación final.
- Android/Unity no se prueban con `act`: `./gradlew` (Windows: `gradlew.bat`) y Unity en modo
  batch, con evidencias en `docs/evidencias/android|unity/`.

## C.11 Lo que ningún agente puede hacer solo (marcar `PENDIENTE-HUMANO`)

Móvil físico (instalar el APK, TalkBack, capturas reales), activar la licencia de Unity y los
secretos de CI, activar workflows del Project y editar sus opciones si la CLI no puede,
cambiar settings/rulesets, hacer público el repo, publicar releases/wiki, firmar el contrato.
Cuando toque, el agente deja las instrucciones exactas (≤ 5 líneas) y sigue con otra tarea.

---

# 7. BACKLOG DETALLADO DE ALAIN (servidor, JavaFX, CI y documentación de servidor/escritorio)

> Para cada issue: crea la rama `feature/iNN-slug` desde `develop`, trabaja con commits pequeños, PR a `develop`,
> review de Jairo y merge del autor. Sigue C.5–C.7. Los apartados `§` son del contrato.

## 7.0 Decisiones de interpretación por defecto (regístralas en `docs/decisiones.md` en `docs/decisiones.md` y avisa a Jairo si le afectan)

| # | Punto ambiguo del contrato | Decisión por defecto | Motivo |
|---|---|---|---|
| D1 | `/health` devuelve «{estado, versión}» | `{"estado":"ok","version":"1.0.0"}` (clave sin tilde) | claves JSON ASCII; los clientes lo ignoran salvo `estado` |
| D2 | Códigos de error para peticiones mal formadas no listados en §3.3 (falta `file`, JSON inválido, `mode` inválido) | Se añade `BAD_REQUEST` (400) además de los del contrato; falta de `password`/vacía ⇒ `EMPTY_PASSWORD` | FastAPI devolvería 422 `{detail}`; el contrato exige `{code,message}` en **todo** error |
| D3 | Cabeceras de `/files/encrypt` «nombre sugerido y tamaño» | `Content-Disposition: attachment; filename="<base>.enc"; filename*=UTF-8''…` y `Content-Length` | estándar HTTP |
| D4 | Nombre sugerido al descifrar | `<nombre sin .enc>` en `Content-Disposition`; el **cliente de escritorio** guarda por defecto con sufijo `.recuperado` (§4) y nunca sobrescribe | el `.enc` no guarda el nombre original |
| D5 | Parámetros del lector de `.enc` | Acepta solo versión 1, KDF 1, iteraciones 100 000…2 000 000, bloque 64 KiB…16 MiB (acota CPU/memoria); emite siempre 600 000 y 1 MiB | evita DoS con cabeceras hostiles |
| D6 | Texto vacío en Vigenère/César | Se acepta y devuelve `""`; clave/desplazamiento inválidos siguen rechazándose | no hay código de error definido |
| D7 | Config de `mode` | `"encrypt"` / `"decrypt"` (inglés, §3.3) | literal del contrato |

## 7.1 I-02 · Servidor Docker + API con token (`feature/i02-server-docker-api`) — 4 h

**Estructura objetivo (`backend/`):**
```
backend/
├─ pyproject.toml  uv.lock  Dockerfile  docker-compose.yml  .env.example  .dockerignore  README.md
├─ app/
│  ├─ __init__.py (__version__="1.0.0")  main.py (create_app + app)  config.py  errors.py  security.py  storage.py
│  ├─ routers/ health.py auth.py files.py text.py
│  └─ crypto/ classic.py (Vigenère/César)  rzen.py (formato .enc, I-04)
└─ tests/
```
**Requisitos:**
- Todas las rutas bajo **`/api/v1`**. `GET /health` **sin token** → 200 (D1). El resto exige
  `Authorization: Bearer <token>`: `GET /auth/check` → **204** si válido, **401** si no.
- Token en `RZ_API_TOKEN` (`.env`). **Si falta o está vacío el servidor no arranca** (mensaje claro).
  Comparación **en tiempo constante** (`hmac.compare_digest`). **Valida el token ANTES de leer el cuerpo**
  (middleware ASGI propio, no `Depends`: FastAPI parsea el multipart antes de ejecutar dependencias) para no procesar
  ni almacenar contenido de peticiones no autorizadas (§7.2).
- `RZ_MAX_UPLOAD_MIB` (def. 100; 20 MiB **siempre** debe funcionar), `RZ_TMP_DIR` (def. `/tmp/rz`, volumen efímero).
- **Errores**: JSON `{code, message}` (§3.3 + D2). Mapea `RequestValidationError`, `HTTPException`, 404/405 y excepciones
  no controladas (→ 500 `INTERNAL_ERROR`) con handlers propios. **Ningún error, log ni traza contiene la contraseña ni el token**
  (revisa el *access log* de uvicorn y desactiva el volcado de cuerpos). Mensajes en español:
  `UNAUTHORIZED` «Token no válido. Revisa los ajustes», `EMPTY_FILE` «El archivo está vacío», `DECRYPT_FAILED` «clave incorrecta o fichero dañado»…
- **Docker (§3.2):** `Dockerfile` basado en `python:3.12-slim`; instala `uv` (imagen `ghcr.io/astral-sh/uv:<versión fija>` copiada al
  contenedor), `uv sync --locked --no-dev`, **usuario sin privilegios**, `HEALTHCHECK` sobre `/api/v1/health` con `python -c "urllib…"`
  (la imagen slim no trae curl), `CMD uvicorn app.main:app --host 0.0.0.0 --port 8000` (**1 worker**).
  `docker-compose.yml`: puerto `8000:8000`, `env_file: .env`, `tmpfs: /tmp/rz` (volumen **efímero** para los `.part`),
  `restart: unless-stopped`, `read_only: true` (+ tmpfs necesarios), `security_opt: [no-new-privileges:true]`, healthcheck.
  `.env.example` con `RZ_API_TOKEN=cambia-este-token-de-ejemplo` y `RZ_MAX_UPLOAD_MIB=100`; **`.env` en `.gitignore`/`.dockerignore`**.
- `backend/README.md`: los 3 pasos de §3.2 en Windows y Linux (copiar `.env`, `docker compose up -d --build`, probar
  `http://IP-del-PC:8000/api/v1/health`, **abrir el puerto 8000 en el cortafuegos** con el comando de cada SO, cómo averiguar la IP).
- En esta PR **quita la condición `hashFiles`** del job `docker`.

**Tests (pytest + `httpx`/TestClient):** health sin token; `auth/check` 204/401 (sin cabecera, esquema incorrecto, token erróneo);
401 antes de leer cuerpo (sube 5 MiB con token malo y comprueba que no se crea nada en `RZ_TMP_DIR`); formato de error en 404/405/422/500;
arranque falla sin `RZ_API_TOKEN`; el token no aparece en logs (`caplog`).
**DoD extra:** `docker compose up -d --build` real, `curl` a health/auth (200/204/401) y `docker compose ps` *healthy* pegados en la PR.

## 7.2 I-03 · Vigenère y César en servidor (`feature/i03-text-ciphers`) — 3 h

- `app/crypto/classic.py` con funciones **puras**; endpoints `POST /api/v1/text/vigenere` (`{text,key,mode}`) y
  `POST /api/v1/text/caesar` (`{text,shift,mode}`), respuesta `{result}`.
- **Vigenère (§6.1):** alfabeto A–Z; conserva mayúsculas/minúsculas; todo lo demás (espacios, dígitos, signos, **Ñ, tildes, diéresis**) pasa
  **sin cifrar y no avanza la posición de la clave**; clave solo letras A–Z (se normaliza a mayúsculas); vacía o con otros caracteres ⇒ **400 `INVALID_KEY`**.
  Ejemplo obligatorio: clave `LUNA`, «Hola Mundo, ¿qué tal?» → «Siya Xoadz, ¿khé tlf?» y vuelta exacta.
- **César (§6.3):** desplazamiento **entero 1–25**; 0, 26, −3, «abc», decimales, `null` ⇒ **400 `INVALID_SHIFT`** (valida tipo y rango tú mismo;
  que no lo haga Pydantic con 422). Equivale a Vigenère de clave de una letra. Ejemplo: +3, «Ataque al amanecer» → «Dwdtxh do dpdqhfhu».
- `mode` fuera de `encrypt|decrypt` ⇒ 400 `BAD_REQUEST` (D2).
**Tests:** ejemplos exactos; ida y vuelta para los 25 desplazamientos y claves de 1/4/20 letras; casos de §7.3 (mayúsc./minúsc., Ñ/tildes/diéresis, espacios y signos);
errores por endpoint con el JSON correcto; propiedad (hypothesis opcional) *descifrar(cifrar(x)) == x*.

## 7.3 I-04 · AES-GCM por bloques y formato `.enc` — formato **RZEN v1** (`feature/i04-aes-gcm-rzen`) — 6 h

Reescribe el motor (no reutilices HGAE v1; sí puedes reutilizar ideas/tests). Módulo `app/crypto/rzen.py`, **sin dependencias de FastAPI**.

**Cabecera (34 bytes, enteros big-endian)** — documéntala en `docs/formato-enc-v2.md` con tabla y diagrama:

| Offset | Bytes | Campo | Valor |
|---|---|---|---|
| 0 | 4 | Magic | ASCII `RZEN` |
| 4 | 1 | Versión | `1` |
| 5 | 1 | KDF | `1` = PBKDF2-HMAC-SHA256 |
| 6 | 4 | Iteraciones | `600000` |
| 10 | 16 | Sal | aleatoria **por fichero** (`os.urandom`) |
| 26 | 4 | Prefijo de nonce | aleatorio **por fichero** |
| 30 | 4 | Tamaño de bloque | `1048576` (en claro por bloque) |

**Cuerpo:** bloques `ciphertext || etiqueta GCM (16 B)`. **Datos en claro = original ‖ SHA-256(original) (32 B)**, troceados en bloques de
`BS` bytes (el último puede ser menor; nunca vacío).
- **Clave:** `PBKDF2-HMAC-SHA256(password UTF-8 sin normalizar, sal, 600000, 32 B)` ⇒ clave distinta por fichero.
- **Nonce 96 bits por bloque:** `prefijo(4 B) ‖ contador de bloque (8 B big-endian)` empezando en 0.
- **AAD por bloque:** `cabecera completa (34 B) ‖ índice de bloque (8 B BE) ‖ indicador de último bloque (1 B: 0x00/0x01)`.
- **Cifrado en streaming (memoria acotada):** lee del origen de `BS` en `BS`, actualiza SHA-256 y mantén un búfer; **emite un bloque solo
  cuando el búfer supera `BS`** (así sabes que no es el último); al EOF añade el digest y emite el resto como bloque **último**
  (si el original es múltiplo de `BS`, el digest queda en un último bloque de 32 B). Hay que **conocer cuál es el último antes de cifrarlo** (por el AAD).
- **Descifrado:** valida cabecera (D5), calcula nº de bloques **por longitud** (`L = total − 34`; cada bloque completo ocupa `BS+16`; el último ocupa el resto y **debe medir ≥ 17 B**), 
  **autentica cada bloque antes de escribir nada**, retiene los últimos 32 B del flujo en claro como digest y, al final, compara
  `hmac.compare_digest(sha256(escrito), digest)`. **Cualquier** fallo (contraseña incorrecta, byte alterado, bloques reordenados/eliminados,
  fichero truncado —incluso en frontera de bloque—, digest distinto, cabecera inválida, fichero demasiado corto) ⇒ `DecryptFailed` y **no se entrega nada**.
- Contraseña vacía ⇒ `EMPTY_PASSWORD` antes de cualquier trabajo; fichero de 0 B ⇒ `EMPTY_FILE` (se rechaza, no se genera nada).
- PBKDF2 (~0,3–0,5 s) bloquea: los endpoints que lo usan deben ser `def` (hilo) o usar `run_in_threadpool`, **nunca** bloquear el *event loop*.
**Tests (`tests/test_rzen.py`):** ida y vuelta con 1 B, 31/32/33 B, `BS−32`, `BS−1`, `BS`, `BS+1`, 2·`BS`, 20 MiB (`@pytest.mark.slow`); misma entrada ⇒ **salidas distintas** (sal y prefijo
distintos) y **ningún nonce repetido** en un fichero; alteración de 1 byte en cabecera/cuerpo/etiqueta; reordenar dos bloques; eliminar un bloque intermedio y el último;
truncar en frontera de bloque y a mitad; añadir bytes al final; contraseña incorrecta; digest manipulado con etiqueta válida (re-cifra a mano para simularlo);
cabeceras hostiles (iteraciones/bloque fuera de rango); memoria acotada (mide `tracemalloc` pico < p. ej. 16 MiB con 20 MiB de entrada).
**Genera vectores para Jairo** (`docs/vectors/`): `image.png.enc` (PNG pequeño real, ≤ 2 KB), `one.bin.enc` (1 B), `manifest.json` con nombre, SHA-256 del original y **contraseña de prueba no secreta**, y `README.md`. Script reproducible `scripts/gen_vectors.py` (determinista salvo sal/prefijo → fíjalos solo en el script de vectores, no en producción).
Comunícaselo a Jairo (label `handoff`, `@GH_JAIRO`) con un comentario en I-07 cuando se integre.

## 7.4 I-05 · Endpoints multipart de cifrado y descifrado (`feature/i05-files-endpoints`) — 4 h

- `POST /api/v1/files/encrypt` (multipart: `file`, `password`) → 200 `application/octet-stream` con el `.enc` (D3).
- `POST /api/v1/files/decrypt` (multipart: `file`, `password`) → 200 con los bytes originales **solo si** todos los bloques autentican **y** la huella coincide;
  cabecera **`X-Integrity: verified`**. La contraseña viaja **solo** en el formulario, nunca en URL/cabeceras.
- **Flujo seguro (§5.1, §7.2, §4):** token → validar `Content-Length` frente a `RZ_MAX_UPLOAD_MIB` (413 `FILE_TOO_LARGE` si lo supera; el 20 MiB pasa) → leer
  el upload **en bloques de 1 MiB** (nunca `await file.read()` entero) → procesar a `RZ_TMP_DIR/<uuid>.part` → solo si todo es correcto, responder con
  `FileResponse`/stream y **borrar el `.part` en `BackgroundTask`** (también en caso de error: `try/except/finally`, `with`). Comprueba tamaño real al recibir (por si no hay `Content-Length`).
  El servidor **no conserva ficheros de usuario**.
- Sanea `UploadFile.filename` (solo basename, longitud máxima, sin saltos de línea → sin inyección de cabeceras).
- Fichero 0 B ⇒ **400 `EMPTY_FILE`**; falta `password` o vacía ⇒ **400 `EMPTY_PASSWORD`**; falta `file` ⇒ **400 `BAD_REQUEST`**; fallo de descifrado ⇒ **422 `DECRYPT_FAILED`**;
  disco lleno/imprevisto ⇒ **500 `INTERNAL_ERROR`** y limpieza del `.part`.
**Tests (`tests/test_files_api.py`, ficheros generados en `tmp_path`):** 1 B, 1 MiB, **20 MiB** (slow), **21 MiB con límite a 20 MiB ⇒ 413** y `RZ_TMP_DIR` vacío después, 0 B ⇒ 400,
contraseña incorrecta ⇒ 422 y **sin `.part` residual**, `.enc` alterado/reordenado/truncado ⇒ 422, sin token ⇒ 401, multipart sin `password` ⇒ 400,
SHA-256 idéntico byte a byte (texto, binario, PNG), `X-Integrity: verified`, el `.part` desaparece también tras cliente que corta, contraseña/token ausentes de respuestas y logs.
**Prueba manual real** contra el contenedor con `curl -F` (pégala en la PR) incluido el de 20 MiB y su tiempo.
**Notifica a Jairo** en I-07 (label `handoff`) cuando el endpoint de **descifrado** esté en `develop` (es su dependencia crítica; priorízalo sobre el de cifrado).

## 7.5 I-10 · JavaFX: cliente, ajustes y HttpClient (`feature/i10-desktop-client`) — 7 h (Sprint 2)

Arquitectura §5.4: **controladores** (solo UI) → **servicios** (HTTP, ficheros, ajustes) → **modelo** (datos). Dominio y servicios **testeables sin UI**.
- `java.net.http.HttpClient` (sin librerías extra). **Multipart manual en streaming** (`BodyPublisher` propio con *boundary* aleatorio, parte `password` y parte `file`,
  contador de bytes para el progreso). Descarga en streaming a `<destino>.part` y `Files.move(…, ATOMIC_MOVE)` al terminar bien; en error se borra el `.part`.
  Errores no-2xx: parsea `{code,message}` (reutiliza `util/Json` si sirve) y mapea a mensajes de usuario. Tiempo máximo de espera **120 s** (§7.2).
- **Ajustes de conexión (§9.4, CU-17):** campo «Dirección del servidor» (IP:puerto, **no fija en código**), token (oculto por defecto, con botón mostrar), botón **«Probar»**
  (`GET /api/v1/auth/check`: 204 ⇒ ok; 401 ⇒ «Token no válido. Revisa los ajustes»; sin respuesta ⇒ aviso de conexión) y guardado local
  (`~/.runnersz/desktop.properties`, permisos 600 en POSIX; documenta que el token queda en claro en disco). Normaliza `host:puerto` ⇒ `http://host:puerto`.
- **Pestaña «Ficheros (AES)»** (§5.2, §4, CU-01/02/03/05/06): elegir entrada y salida; salida por defecto `<entrada>.enc` (cifrar) o `<sin .enc>.recuperado` (descifrar, D4);
  **rechazar** salida = entrada; si la salida existe, **confirmación explícita** o rechazo; el **original nunca se modifica ni se mueve**; contraseña vacía ⇒ aviso **antes** de llamar a la API;
  entrada inexistente/sin permisos ⇒ alerta y **sin llamada ni fichero de salida**. Operación en `javafx.concurrent.Task` **fuera del hilo de la UI**, con barra de progreso y botón cancelar;
  `try/catch/finally` con cierre de flujos en `finally`; la app **no se cierra** ante errores. Alertas de conexión con **«Reintentar»** e **«Ir a Ajustes»**.
- **Fat JAR** con Shade (`Main-Class` = `Launcher`), **el mismo JAR en Windows y Linux** (incluye los nativos JavaFX de ambas plataformas); flag `--smoke-test` que construye la escena y sale con código 0
  (úsalo en CI con xvfb). Comprueba con `jar tf` que están ambos nativos.
- **JaCoCo** `check` con mínimo **0,80** de líneas sobre las clases de **dominio y servicios** (§11.2); excluye controladores y `App/Launcher`.
**Tests (JUnit 5):** servidor falso con `com.sun.net.httpserver.HttpServer` (cifrado/descifrado OK, 401, 400, 413, 422, 500, timeout, corte de conexión); política de salida (misma ruta, existente, `.enc`/`.recuperado`);
construcción multipart (boundary, orden, codificación UTF-8 de `password`); limpieza del `.part`; ajustes (persistencia, normalización, validación); mensajes por `code`.

## 7.6 I-11 · JavaFX: Vigenère, integridad, alertas y accesibilidad (`feature/i11-desktop-vigenere-a11y`) — 4 h (Sprint 2)

- Pestaña **«Mensajes (Vigenère)»** (CU-04): texto de entrada, clave, cifrar/descifrar vía `POST /text/vigenere`; **cargar `.txt`** (UTF-8) y **guardar resultado `.txt`** con las mismas reglas de no sobrescritura.
  Errores `INVALID_KEY` ⇒ alerta clara. Resultado en fuente monoespaciada.
- **Integridad (CU-05):** al descifrar con éxito y `X-Integrity: verified` ⇒ alerta/estado «**Integridad verificada**» (con icono ✓ y texto, no solo color).
- **Accesibilidad (§9.3, equivalente a ARIA/A11Y en JavaFX):** `setAccessibleRole` + `setAccessibleText` en cada control y botón de icono; `setAccessibleHelp` en contraseña, token y dirección;
  alertas modales con foco automático; **orden de tabulación lógico**, foco visible, **mnemónicos** (`_Cifrar`, `_Descifrar`); botones **≥ 32 px** de alto; estados correcto/error con **icono + texto**.
- **Diseño y tipografía (§9, §9.2):** paleta (Azul Hogwarts `#1B2A41`, Pergamino `#E8DCC6`, Oro mágico `#C9A96A`, Piedra `#2E2E33`, Madera `#4A3520`) en `styles.css` con variables;
  botones principales oro con **texto azul**; **jamás oro como texto sobre pergamino**. Fuentes **Inter** (títulos 20 pt Bold; cuerpo y botones 12 pt) y **Open Sans** (textos largos 12 pt) **empaquetadas** en
  `src/main/resources/fonts` (licencia OFL: incluye el texto de licencia) con `Font.loadFont`; respaldo `Segoe UI`/`Roboto`/`sans-serif`; texto cifrado monoespaciado 14 pt.
  Si no puedes descargar las fuentes (sin red), **pide al humano** los `.ttf`; no uses otras.
- **Contraste WCAG 2.1 (4.5:1):** script pequeño (Python o Java) que calcule la razón de **cada pareja texto/fondo usada** y guarda la tabla en `docs/evidencias/escritorio/contraste.md`.
- Escalado: la UI no se rompe con fuente del sistema aumentada (prueba manual a 125 % y 150 %).
- Comprobación **solo con teclado** de todo el flujo (§9.3) y su registro en `docs/evidencias/escritorio/`. (Lo repetís en pantalla en el Sprint 2.)

## 7.7 I-12 · Tests, cobertura 80 %, pre-push y act (`feature/i12-quality-gates`) — 4 h (Sprint 2)

- Cobertura servidor **≥ 80 % bloqueante** (`--cov-fail-under=80` ya en `addopts`) y JaCoCo ≥ 0,80 desktop; revisa **huecos reales** (ramas de error, limpieza de `.part`), no solo el número.
- Verifica y documenta en el README: `uv run pre-commit run --all-files`; hook pre-commit **bloquea** commit en `main`/`develop`; hook pre-push **bloquea** push a `main`/`develop` y **falla** si cobertura < 80 %.
- **act (§11.5):** `act --validate` al tocar workflows; `act push -j backend` (y `-j desktop` si tocaste Java) antes de cada push; la PR manda GitHub Actions (act es simulación).
  Documenta limitaciones (jobs Windows y el job `docker` no se ejecutan bien bajo act).
- **Fallo provocado (§11.8-4):** (a) local: `uv run pytest --cov-fail-under=100` ⇒ salida y código ≠ 0; (b) en GitHub: lanzar `workflow_dispatch` de `backend.yml` con `cov_threshold=100` ⇒ job en rojo (captura). **No se hace en una PR a develop.**
- Informe de cobertura publicado como **artefacto** (§11.8-5).

## 7.8 I-13 · Pruebas del Anexo VI.4 con evidencias (`feature/i13-test-evidence`) — 3 h (Sprint 2)

Ejecuta **toda** la tabla §7.3 del lado servidor/escritorio y rellena `docs/plan-de-pruebas/servidor-escritorio.md` (filas PT-01…PT-13; resultado **obtenido**, no copiado del esperado; evidencia = ruta a fichero en `docs/evidencias/servidor|escritorio|ci/` o enlace a ejecución de Actions).
Ficheros de prueba: **0 B, 1 B, 1 MiB, 20 MiB, 21 MiB y un PNG** (se generan con script; **no se versionan** los grandes). Marca como **fallo real** lo que falle y abre issue `bug`, no maquilles.
Las filas Android/Unity (PT-14…PT-19, UN-xx) las rellena Jairo en `docs/plan-de-pruebas/android-unity.md`. Cuando el escritorio genere un `.png.enc`, **entrégaselo a Jairo** para la prueba «descifrar desde Android» (vía el humano o un adjunto en el issue I-19; **sin contraseña real en el repo**).

## 7.9 I-14 · Manual, README y demo (`feature/i14-docs-demo`) — 2 h (Sprint 2)

- `docs/manual-instalacion-servidor.md` (Windows y Linux, Docker, `.env`, firewall, IP, comprobación de `/health`, problemas típicos) y `docs/manual-uso-escritorio.md` (con capturas reales de la app) — ambos con pasos **reproducibles**: pruébalos siguiéndolos literalmente.
- README raíz final: arquitectura, requisitos, inicio rápido, cómo ejecutar tests/hooks/act, enlaces a tablero, contrato, manuales, plan de pruebas y evidencias.
- Guion de demo (`docs/demo.md`): orden, datos de prueba, plan B si falla la red.
- **Protección de ramas verificada** (§11.6, I.3): checklist para el humano (captura del ruleset cuando el repo sea público). Prepara el borrador de la **wiki** (copia de `docs/`); **no** hagas público el repo ni publiques la wiki sin orden humana.

---

# 8. CALENDARIO Y ORDEN DE TRABAJO DE ALAIN

| Cuándo | Qué | Entregable / dependencia |
|---|---|---|
| **Jue 29-oct (hoy)** | Esperar «FASE 0 COMPLETA» de Jairo; secciones 2 y 4.1–4.4 | **Review sustantiva (ejecutada de verdad) de la PR `feature/i01-workflow`**; verificación del tablero, issues y etiquetas |
| **Vie 30-oct** | Tras el merge de la PR de arranque: 4.5, 4.6 (a)(b)(c)(d), 4.7 | Hooks, CI (`backend`, `desktop`, `docker`), `docs/api-contract.md` (**Jairo lo necesita**), ruleset si procede. Revisar PR de Jairo |
| **Lun 2-nov** | I-02 | Servidor en Docker con health y auth ⇒ **Jairo ya puede probar «Probar»** |
| **Mar 3-nov** | I-03 (mañana) → I-04 (inicio) | Vigenère/César; formato RZEN |
| **Mié 4-nov** | I-04 | Motor RZEN + vectores en `docs/vectors/` ⇒ avisar a Jairo |
| **Jue 5-nov** | I-05 (**descifrado primero**) | `/files/decrypt` en `develop` ⇒ **Jairo integra I-07** |
| **Vie 6-nov** | I-05 cierre, **revisión de Sprint 1** | `docs/planificacion/sprint-1-alain.md` (hecho/no hecho/horas reales/ajustes) y demo mínima (Docker + Android + Unity) |
| **Lun 9–Mar 10** | I-10 | Cliente JavaFX funcional (Jairo necesita su `.enc` para I-19) |
| **Mié 11-nov** | I-11, I-12 | Vigenère/a11y/contraste; calidad y fallo provocado |
| **Jue 12-nov** | I-13, I-14, cierre | Evidencias, manuales, demo, **entrega**. Revisión final de Sprint 2 |

Horas: S1 = I-01 3 + I-02 4 + I-03 3 + I-04 6 + I-05 4 = **20 h**; S2 = I-10 7 + I-11 4 + I-12 4 + I-13 3 + I-14 2 = **20 h**.
El Discovery (hoy y mañana) **no cuenta**. **Revisar las PR de Jairo tiene prioridad sobre empezar tareas nuevas** (SLA: el mismo día).
Daily de 5 min (lo hace el humano): prepárale 3 líneas — ayer / hoy / bloqueos.

---

# 9. LO QUE ALAIN ENTREGA A JAIRO (handoffs) Y LO QUE ESPERA DE ÉL

| Entrega de Alain | Cuándo | Cómo se comunica |
|---|---|---|
| Normas, `board.py`, CODEOWNERS, plantillas | Jue 29 | PR `feature/i01-workflow` |
| `docs/api-contract.md` (+ `docs/openapi.json` generado) | Vie 30 | PR + comentario en I-06, I-07 |
| Servidor Docker con `/health` y `/auth/check` | Lun 2 | comentario en I-06 + el humano pasa **IP y token** (nunca por el repo) |
| Vectores `.enc` v2 | Mié 4 | comentario en I-07 |
| `/files/decrypt` en `develop` | Jue 5 | comentario en I-07 |
| `/text/caesar` (ya desde Mar 3) | Mar 3 | comentario en I-15 |
| `.png.enc` generado con el escritorio | Mar 10 | comentario en I-19 |
| Revisión de cada PR de Jairo | ≤ mismo día | `gh pr review` |

**Espera de Jairo:** PR de Android/Unity con CI verde; resultados reales de las filas Android/Unity del plan de pruebas; capturas de TalkBack y del móvil; APK. Si Jairo se bloquea por algo tuyo, es **prioridad máxima**.

---

# 10. EVIDENCIAS QUE DEBES IR GUARDANDO (contrato §11.8, §7.3)

Carpetas `docs/evidencias/ci/` (E1–E7), `docs/evidencias/servidor/` (E8) y `docs/evidencias/escritorio/` (E10); E9 vive en `docs/plan-de-pruebas/servidor-escritorio.md` (cada fichero: comando exacto, fecha, salida real, entorno; **sin secretos**).

| ID | Evidencia | Cuándo |
|---|---|---|
| E1 | Salida de `uv run pre-commit run --all-files` (verde) y bloqueo de commit en `main`/`develop` | I-01/I-12 |
| E2 | `act push -j backend` terminando en «Job succeeded» (ruff, isort, tests, cobertura) | I-12 |
| E3 | Captura/enlace de **GitHub Actions** con `backend` y `desktop` en verde en una PR | cada PR |
| E4 | Fallo provocado (cobertura) local y en Actions (`workflow_dispatch`) | I-12 |
| E5 | `coverage.xml` como artefacto y captura del resumen | I-12 |
| E6 | Bloqueo de `git push origin HEAD:develop` por el hook pre-push | I-12 |
| E7 | Ruleset (`ruleset.json`) y captura de la protección de ramas | I-01/I-14 |
| E8 | `docker compose ps` *healthy* + `curl` de health/auth | I-02 |
| E9 | Tabla de pruebas §7.3 con resultados reales | I-13 |
| E10 | `contraste.md` y registro de navegación solo con teclado | I-11 |

---

# 11. DEFINICIÓN DE «TERMINADO» DE ALAIN (checklist final antes del 12-nov)

- [ ] I-01…I-05, I-10…I-14 en «Hecho», cada uno con PR integrada, CI verde y review de Jairo.
- [ ] `develop` verde; `main` actualizado por **PR `develop → main`** con orden del humano.
- [ ] Servidor: `docker compose up` limpio en Windows y Linux; 20 MiB OK; 21 MiB ⇒ 413; todos los códigos de error con `{code,message}`.
- [ ] Cobertura servidor ≥ 80 % y JaCoCo ≥ 0,80 (reales, bloqueantes).
- [ ] Fat JAR único para Windows y Linux, ejecutado en ambos.
- [ ] Evidencias E1–E10 y plan de pruebas completos y **honestos** (fallos incluidos).
- [ ] Sin secretos en el repo (`git log -p | grep -iE 'token|password|secret'` revisado) ni ficheros grandes.
- [ ] ≥ 3 commits propios con cambios reales y ≥ 1 PR integrada con tu review a Jairo.
- [ ] README, manuales, demo, `docs/decisiones.md` y `docs/planificacion/*-alain.md` al día; wiki preparada.

# 12. FORMATO DE TUS RESPUESTAS AL HUMANO

Al cerrar cada sesión: **(1)** estado del tablero (qué issue en qué columna), **(2)** qué hiciste con evidencia (comando + resultado), **(3)** PR abiertas/pendientes de review (enlaces),
**(4)** bloqueos y qué necesitas de Jairo/del humano (`PENDIENTE-HUMANO` explícitos), **(5)** siguiente paso, y **(6)** las 3 líneas del daily (ayer / hoy / bloqueos). Si algo no se pudo verificar, dilo explícitamente.

**Empieza ahora:** comprueba que estás en un clon con `feature/i01-workflow` (o `develop` si ya se integró), lee `docs/contrato/…` y `docs/WORKFLOW.md`, haz la pregunta única de la sección 2 y ejecuta la auditoría 4.1.
