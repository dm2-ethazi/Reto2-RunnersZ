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

## Excepción de arranque DJ0 (PR #23, revisión de Alain)

El tablero efectivo es Project 14 de dm2-ethazi; sustituye a Project 6.
La rama de arranque es prototype/runnersz y su PR se dirige a develop.
integration/reto2 conserva solo el histórico v1; no se restaura.
Backend y Desktop están vacíos en este arranque. I-01 entrega estructura,
documentación y herramientas; los builds y CI de módulos se incorporan
con sus tareas técnicas. No se acredita CI verde sin ejecutarlo.
