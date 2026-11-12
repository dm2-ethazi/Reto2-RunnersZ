# 13 · Flujo de trabajo, hooks y CI

Cómo se trabaja en este repositorio: ramas, mensajes de commit, hooks, comprobadores, CI y tablero. Es lo que permite que dos personas (y sus agentes) no se pisen.

> Esta página recoge lo que se ejecuta de verdad en el repositorio y señala una discrepancia real que debes conocer.

---

## 1. Ramas

| Rama | Uso |
|---|---|
| `main` | Estable. **No se commitea ni se empuja directo** |
| `develop` | Integración. **Tampoco se commitea directo** |
| `feature/<tema>` | Trabajo diario, una rama por tarea |

- Rama actual del repositorio: `develop`.
- Para traer novedades a tu rama: `git merge origin/develop` (merge, **sin rebase**).
- Prohibido `git push --force`, `--force-with-lease`, `--no-verify` y el rebase sobre ramas publicadas.
- Integración por **PR**; el autor hace el merge tras la aprobación del otro y con el CI en verde.

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/mi-tarea
```

---

## 2. Mensajes de commit (lo que valida el hook)

Formato:

```text
tipo(ámbito opcional): descripción en minúscula, sin punto final

Cuerpo opcional separado por una línea en blanco.
```

Reglas que aplica `scripts/check_commit_message.py` (verificadas):

- Tipo en **minúsculas** de esta lista: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`, `build`, `ci`, `revert`.
- La descripción **empieza en minúscula**.
- **No** termina en punto.
- Asunto de **72 caracteres como máximo**.
- Cuerpo separado por una línea en blanco.
- Se rechazan descripciones vagas: «arreglado», «cambiado», «corregido», «corrección», «varios cambios».

Ejemplos:

```text
feat(desktop): añade aviso de integridad      ✅
docs(wiki): documenta la instalación          ✅
FEAT: Añade endpoint                          ❌ tipo en mayúsculas
feat: añade endpoint.                         ❌ punto final
```

> ⚠️ **Discrepancia real:** la norma de equipo escrita pide `TIPO: Asunto` en mayúsculas y ≤ 50 caracteres, pero el hook `commit-msg` ejecuta el script anterior, que exige **minúsculas y ≤ 72**. Manda el script: si escribes el formato en mayúsculas, el commit se rechaza. Conviene unificar la norma; no lo hace esta wiki.

Comprobar un mensaje sin commitear:

```bash
printf 'feat(wiki): añade guía de instalación\n' > mensaje.txt
python scripts/check_commit_message.py mensaje.txt
```

✅ Sin salida y código 0. Con errores, imprime el motivo y devuelve 1.

---

## 3. Protección de ramas

`scripts/check_branch.py` bloquea commit y push directos en `main` y `develop`:

```bash
python scripts/check_branch.py
```

✅ En una rama `feature/...` devuelve 0. En `main`/`develop`, imprime el bloqueo y devuelve 1.

En GitHub, los rulesets de `main` y `develop` exigen PR, aprobación y checks. Si el plan no aplica rulesets a repos privados, **la barrera efectiva son los hooks locales** (§11.6 del contrato).

---

## 4. Instalar los hooks

Desde la raíz, una vez por máquina:

```bash
cd backend
uv sync --locked
cd ..
uv run --directory backend pre-commit install --hook-type pre-commit --hook-type pre-push --hook-type commit-msg
```

Comprobar que funcionan:

```bash
uv run --directory backend pre-commit run --all-files
```

Hooks declarados en `.pre-commit-config.yaml`:

| Hook | Momento | Qué hace |
|---|---|---|
| `protected-branches` | commit y push | Bloquea `main` y `develop` (`scripts/check_branch.py`) |
| `board-tests` | commit y push | Ejecuta las pruebas de `scripts/tests` |
| `commit-message` | commit-msg | Valida el formato del mensaje |

---

## 5. Qué se ejecuta en cada momento (y qué no)

| Momento | Comprobaciones efectivas |
|---|---|
| `git commit` | rama protegida, pruebas de herramientas, formato del mensaje |
| `git push` | rama protegida, pruebas de herramientas |
| GitHub Actions | backend (ruff, isort, pytest con 80 %) y desktop (Xvfb + `mvn verify`, fat JAR) |

---

## 6. Aviso conocido sobre los hooks

`.pre-commit-config.yaml` declara la clave **`repos:` dos veces**. YAML conserva la última, así que **hoy solo se ejecutan los tres hooks locales** de arriba.

Consecuencia práctica: `pre-commit run --all-files` **no** ejecuta los hooks genéricos (espacios finales, fin de fichero, YAML/JSON), ni `ruff`, ni `isort`, ni `pytest`. Esas comprobaciones hay que lanzarlas a mano:

```bash
cd backend
uv run ruff check .
uv run isort . --check-only
uv run pytest
```

Si quieres que se ejecuten automáticamente, hay que fusionar las dos listas `repos` en una sola. Es un cambio de configuración del responsable de CI: no se hace desde la wiki.

---

## 7. Integración continua (GitHub Actions)

### `backend.yml` — job `backend`

En cada push y PR:

1. `uv sync --locked`
2. `uv run ruff check .`
3. `uv run isort . --check-only`
4. `uv run pytest` (falla por debajo del 80 % de cobertura)
5. Publica `coverage.xml` como artefacto

### `desktop.yml` — jobs `desktop` y `desktop-windows`

- **desktop** (Ubuntu): JDK 21, `xvfb-run --auto-servernum mvn --batch-mode verify` (las pruebas cargan el FXML real y necesitan motor gráfico), sube el informe JaCoCo y el fat JAR.
- **desktop-windows** (Windows): JDK 21, `mvn --batch-mode -DskipTests package`, sube `runnersz-desktop.jar` (el mismo JAR único para Windows y Linux, comprobando que también se genera en Windows).

Android y Unity **no** se compilan en CI: se validan en local (`gradlew`, Unity batch) y la evidencia se adjunta al PR.

### Simular el CI en local con `act`

`.actrc` fija la imagen `catthehacker/ubuntu:act-latest` y `linux/amd64`:

```bash
act push -j backend
act push -j desktop
```

`act` es una simulación previa, no la validación final: la verdad es la pestaña *Actions* del PR. Requiere Docker.

---

## 8. Herramientas de `scripts/`

| Script | Qué hace | Ejemplo |
|---|---|---|
| `check_branch.py` | Bloquea `main` y `develop` | `python scripts/check_branch.py` |
| `check_commit_message.py` | Valida el mensaje | `python scripts/check_commit_message.py mensaje.txt` |
| `board.py` | Tablero kanban vía `gh` (Project 14 de `dm2-ethazi`) | `python scripts/board.py status` |
| `scripts/tests/` | 43 pruebas de las herramientas | `uv run --python 3.12 --with pytest python -m pytest scripts/tests -q` |

`board.py` necesita la CLI de GitHub autenticada y con scopes:

```bash
gh auth refresh -s repo,project,read:org,workflow
python scripts/board.py status
python scripts/board.py start 42      # mueve el issue a «En curso»
python scripts/board.py review 42     # «En revisión»
python scripts/board.py done 42       # «Hecho»
python scripts/board.py --dry-run done 42   # sin tocar nada
```

> Los subcomandos que mueven tarjetas modifican GitHub: se usan con autorización del responsable.

---

## 9. Pull requests y propiedad de ficheros

Plantilla: `.github/pull_request_template.md` con estas secciones:

1. **Qué y por qué** (cita el apartado del contrato).
2. **Cómo probar** (comandos exactos).
3. **Criterios de aceptación** (copiados del issue, ✔/✘).
4. **Evidencias** (salida de pruebas, cobertura, capturas).
5. **Riesgos y limitaciones** (`PENDIENTE-HUMANO` si aplica).

Propiedad (`CODEOWNERS`):

| Ruta | Revisor principal |
|---|---|
| `backend/`, `desktop/`, `scripts/`, CI de backend/desktop, `.gitignore`, README raíz | Alain (`@alain8123`) |
| `android/`, `unity/` | Jairo (`@jairoben`) |

Todo PR necesita **≥ 1 aprobación del otro integrante**, checks en verde y sin conflictos. Nadie aprueba su propia PR.

---

## 10. Definición de «Hecho» (§12)

Una tarea está hecha cuando:

1. Está integrada por PR en `develop`.
2. Los tests pasan (local y CI).
3. La revisó el otro integrante.
4. La tarjeta está en «Hecho».

Cada línea de trabajo: **1 issue ↔ 1 rama ↔ 1 PR ↔ 1 tarjeta**.

---

## 11. Antes de cada commit

```bash
git diff --cached            # revisa lo que entra
git status -sb               # rama y ficheros
```

Y busca secretos en el diff: `token`, `password`, `secret`, `.env`, `.jks`, `.keystore`. Nunca `git add .` ni `git add -A`: añade por rutas.

---

[Volver al índice](Home.md)
