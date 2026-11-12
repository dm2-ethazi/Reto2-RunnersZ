# RunnersZ · Reto 2

Proyecto del Reto 2 para el cliente **Hogwarts**. Entrega: **12 de noviembre de 2026**.

RunnersZ cifra y descifra texto y ficheros. Toda la criptografía vive en el servidor; los tres
clientes solo hablan con su API por HTTP.

- **Servidor** (`backend/`): API REST en Python con FastAPI. Implementa Vigenère, César,
  AES-256-GCM por bloques de 1 MiB y el formato de fichero `.enc` (RZEN).
- **Cliente de escritorio** (`desktop/`): aplicación JavaFX 21 para cifrar y descifrar ficheros,
  con progreso real y cancelación.
- **App Android** (`android/`): CipherSprint, cliente Kotlin para descifrar texto (César) e
  imágenes.
- **Minijuego Unity** (`unity/`): juego de descifrado César para Windows y Android, autónomo, no
  usa la API.

El pliego, el contrato técnico v2.0 y las normas del equipo se mantienen fuera del repositorio, en
`C:\Users\dm2\ia\reglas`, por indicación del responsable.

## Contenido del repositorio

| Carpeta | Qué contiene | Manual |
|---|---|---|
| `backend/` | API FastAPI (Python 3.12), tests, Dockerfile y `docker-compose.yml` | [`backend/README.md`](backend/README.md) |
| `desktop/` | Cliente JavaFX con Maven Wrapper, `pom.xml`, `src/` y `docs/` | [`desktop/README.md`](desktop/README.md) |
| `android/` | Proyecto Gradle de CipherSprint (`app/`), tests y capturas en `docs/` | [`android/README.md`](android/README.md) |
| `unity/` | Proyecto Unity 6000.3.24f1: escena, scripts, tests y builds | [`unity/README.md`](unity/README.md) |
| `scripts/` | `board.py` (tablero), `check_branch.py`, `check_commit_message.py`, `generate_vectors.py` | [`scripts/README.md`](scripts/README.md) |
| `.github/` | `CODEOWNERS`, plantillas de issue y PR, workflows `backend.yml` y `desktop.yml` | [`.github/COLABORACION.md`](.github/COLABORACION.md) |
| `release/` | Binarios de la entrega **v1.0.0**: los dos APK y el fat JAR del escritorio | — |

## Release v1.0.0

El release de entrega del Reto 2 se etiqueta **v1.0.0** y sus binarios están versionados en `release/`:

| Fichero | Qué es | Versión interna |
|---|---|---|
| `release/CipherSprint-android_v1.0.0.apk` | App Android (CipherSprint) | `versionName 2.0.0` (`versionCode 2`) |
| `release/RunnersZ-unity_v1.0.0.apk` | Minijuego Unity para Android | `bundleVersion 1.0` |
| `release/runnersz-desktop_v1.0.0.jar` | Cliente de escritorio (fat JAR único) | `2.1.0` en `pom.xml` |

**v1.0.0 es la versión de la entrega, no la de cada aplicación.** Los dos APK y el JAR se generan
desde el código de `develop` con las versiones internas de la tabla; el nombre del fichero no
cambia las versiones de `build.gradle` ni del `pom.xml`.

## Cómo arrancar cada pieza

**Servidor** (requiere uv y, para Docker, Docker Compose):

```bash
cd backend
uv sync --locked
docker compose up -d --build          # o: uv run uvicorn runnersz.main:app --app-dir src --port 8000
```

La API queda en `http://127.0.0.1:8000`, con documentación interactiva en `/docs`.

**Escritorio** (requiere JDK 21; el wrapper descarga Maven):

```bash
cd desktop
./mvnw clean verify                    # compila, pasa pruebas y aplica el umbral de cobertura
java -jar target/runnersz-desktop.jar  # el mismo JAR sirve en Windows y en Linux
```

Después, en **Ajustes**, se introduce la dirección del servidor y el token; no hay credenciales en
el código.

**Android** (requiere JDK 21 y SDK Android 35):

```bash
cd android
sh ./gradlew --no-daemon testDebugUnitTest assembleRelease
```

El APK queda en `android/app/build/outputs/apk/release/app-release.apk`.

**Unity**: abre la carpeta `unity/` con Unity 6000.3.24f1 y carga `Assets/Scenes/Game.unity`.
Los ejecutables se generan en `unity/Builds/` (Windows y Android) y no se versionan.

## Cómo trabajamos

- **Ramas**: `main` para entregas, `develop` para integración y `feature/*` por apartado
  (`feature/api`, `feature/desktop`, `feature/android`, `feature/unity-v2`). Nada se commitea
  directamente en `main` ni en `develop`.
- **Commits**: `TIPO: descripción` en imperativo, asunto de 50 caracteres como máximo y cuerpo a
  72; `python scripts/check_commit_message.py <fichero>` valida el formato.
- **Hooks**: `.pre-commit-config.yaml` ejecuta estilo, tests rápidos y la protección de rama antes
  del commit y del push.
- **Tablero**: [Proyecto 14 «RunnersZ»](https://github.com/orgs/dm2-ethazi/projects/14);
  `python scripts/board.py status|start|review|done <n>`.
- **Integración continua**: `backend.yml` ejecuta ruff, isort y pytest con cobertura mínima del
  80 %; `desktop.yml` ejecuta `mvn verify` con Xvfb y publica el informe JaCoCo y el fat JAR.

## Estado

| Pieza | Pruebas | Cobertura | Artefacto |
|---|---|---|---|
| `backend/` | 62 pruebas, 0 fallos | 97,7 % (falla por debajo del 80 %) | Imagen Docker |
| `desktop/` | 83 pruebas, 0 fallos | umbral 80 % en `mvn verify` | `target/runnersz-desktop.jar` |
| `android/` | 19 pruebas en 6 suites, 0 fallos | no medida | APK firmado con clave debug |
| `unity/` | 24 EditMode y 2 PlayMode, 0 fallos | no medida | Builds de Windows y Android |

Límites conocidos, detallados en cada manual: el APK de Android y el de Unity no se han probado en
un teléfono físico, el JAR de escritorio se ha ejecutado en Windows (falta la prueba en Linux con
GTK 3) y el token del cliente Android viaja dentro del APK, como exige la demo.

## Equipo (roles del Anexo I.2)

- **Alain** (`@alain8123`): servidor, cliente JavaFX, CI, hooks y documentación.
- **Jairo** (`@jairoben`): aplicación Android, minijuego Unity y arranque del repositorio.
