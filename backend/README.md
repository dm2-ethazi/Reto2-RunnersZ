# RunnersZ · API de cifrado

API REST en Python 3.12 con FastAPI. JavaFX y Android envían texto JSON o
contenido de ficheros multipart; los algoritmos residen en el servidor y el
contrato técnico v2 manda sobre cualquier decisión de implementación.

## Estado actual

Rama: `feature/api`. PR: https://github.com/dm2-ethazi/Reto2-RunnersZ/pull/30.

El backend cubre los apartados 4.3, 4.4, 4.5, 4.7, 4.12 y 4.13 del pliego y las
secciones 3, 5.3, 6, 7 y 11 del contrato: Vigenère, César, AES-256-GCM por
bloques, errores `{code, message}`, formato RZEN con nonce de 96 bits, tests con
cobertura bloqueante y hooks de pre-commit y pre-push. Lo que sigue sin
acreditarse está en «Pendientes».

## Requisitos e instalación

- Python 3.12 gestionado mediante uv.
- uv disponible en la terminal.
- Ejecutar los comandos desde esta carpeta `backend/`.

```powershell
cd backend
uv sync --locked
```

No hace falta activar manualmente `.venv`. `uv.lock` fija las dependencias y
`uv sync --locked` falla si el lock no está al día, igual que en el CI.

## Configuración y arranque en Windows

Crea `.env` dentro de `backend/`. El valor mostrado es ficticio: sustitúyelo por
un token local propio, sin compartirlo ni versionarlo.

```dotenv
RZ_API_TOKEN=CAMBIA_ESTE_VALOR_LOCAL
RZ_MAX_UPLOAD_MIB=100
```

Opcional: `RZ_TMP_DIR` cambia la carpeta de los temporales `.part` (por defecto
la del sistema; Docker usa `/var/tmp/runnersz`). La configuración se lee en cada
petición, así que no hay que reiniciar al cambiar el token o el límite.

```powershell
uv run --python 3.12 uvicorn runnersz.main:app --app-dir src --env-file .env --host 127.0.0.1 --port 8000 --reload
```

Deja la terminal abierta. Para detener el servidor, pulsa **Ctrl+C**.
`--reload` es para desarrollo. `127.0.0.1` permite probar desde el mismo PC.
Para probar desde otro dispositivo de la red local, cambia el host a `0.0.0.0`
y usa la IP del PC; el cortafuegos debe permitir el puerto 8000.
HTTP transmite el token y la contraseña sin TLS: el contrato lo limita a la demo
en una red local controlada (§3.4).

En Linux, desde `backend/`, el comando de uvicorn es el mismo. Si existe una
variable temporal con otro token, elimínala antes con `unset RZ_API_TOKEN`.

## Despliegue con Docker Compose

```bash
cd backend
cp .env.example .env      # y define RZ_API_TOKEN
docker compose up -d --build
```

La imagen usa `python:3.12-slim`, instala con `uv sync --locked`, corre con el
usuario `appuser`, guarda los `.part` en un `tmpfs` efímero y declara un
healthcheck sobre `/api/v1/health`. El token entra desde `.env`; nunca se
versiona (solo `.env.example`).

## Probar en el navegador

1. Abre http://127.0.0.1:8000/api/v1/health: debe responder 200.
2. Abre http://127.0.0.1:8000/docs y pulsa **Authorize**.
3. Introduce **solo el token** de `.env`, sin `Bearer`. Swagger añade el prefijo.
4. Ejecuta `GET /api/v1/auth/check`: con token válido responde **204**, sin cuerpo.
5. Para cifrar un fichero, abre `POST /api/v1/files/encrypt`, pulsa **Try it out**,
   selecciona un fichero no vacío e introduce la contraseña en `password`.
6. Para recuperarlo, envía el resultado a `/files/decrypt` con la misma contraseña.
   Comprueba que la respuesta incluye `X-Integrity: verified`.

La contraseña del fichero y el token de acceso son campos distintos.
Al actualizar la autenticación, recarga `/docs` con **Ctrl+F5** y vuelve a autorizar.

## Rutas

Todas las rutas usan el prefijo `/api/v1`. Salvo `health`, requieren la cabecera
`Authorization: Bearer <token>`. En Swagger se introduce solamente el valor del token.

| Método | Ruta | Entrada | Respuesta correcta |
|---|---|---|---|
| GET | `/health` | Ninguna | 200 con `status` y `version` |
| GET | `/auth/check` | Token Bearer | 204 sin cuerpo |
| POST | `/text/vigenere` | JSON: `text`, `key`, `mode` | 200 con `result` |
| POST | `/text/caesar` | JSON: `text`, `shift`, `mode` | 200 con `result` |
| POST | `/files/encrypt` | Multipart: `file`, `password` | Bytes cifrados, `Content-Disposition` con nombre sugerido y `Content-Length` |
| POST | `/files/decrypt` | Multipart: `file`, `password` | Bytes recuperados, `X-Integrity: verified` y nombre sugerido |

Ejemplo Vigenère:

```json
{"text":"Hola Mundo, ¿qué tal?","key":"LUNA","mode":"encrypt"}
```

Devuelve `{"result":"Siya Xoadz, ¿khé tlf?"}`, el ejemplo del contrato §6.1.

Ejemplo César:

```json
{"text":"Ataque al amanecer","shift":3,"mode":"encrypt"}
```

`mode` solo admite `encrypt` o `decrypt`. César admite enteros de 1 a 25,
también enviados como texto (`"3"`); `0`, `26`, negativos y valores no enteros
devuelven `INVALID_SHIFT`. Vigenère exige una clave de letras ASCII. Las letras
con tildes, la Ñ, los signos y los espacios se conservan sin avanzar la clave.

## Errores

Todo error responde JSON `{code, message}`, nunca con la contraseña ni el token.
Los 422 de validación de FastAPI se convierten en 400 con la misma forma.

| code | HTTP | Mensaje y cuándo ocurre |
|---|---|---|
| `UNAUTHORIZED` | 401 | «Token no válido. Revisa los ajustes». |
| `EMPTY_FILE` | 400 | «El archivo está vacío». |
| `EMPTY_PASSWORD` | 400 | «La contraseña está vacía». |
| `INVALID_KEY` | 400 | «La clave no es válida: usa solo letras A-Z». |
| `INVALID_SHIFT` | 400 | «El desplazamiento debe ser un entero entre 1 y 25». |
| `INVALID_REQUEST` | 400 | Cuerpo o formulario incompletos; `mode` fuera de `encrypt\|decrypt`. |
| `FILE_TOO_LARGE` | 413 | Supera `RZ_MAX_UPLOAD_MIB` (100 MiB por defecto). |
| `DECRYPT_FAILED` | 422 | «clave incorrecta o fichero dañado»: contraseña distinta, bloque alterado, reordenado, truncado o huella que no cuadra. |
| `INTERNAL_ERROR` | 500 | Fallo inesperado; se borran los temporales. |

## Formato del fichero .enc (§5.3 y §6.2)

| Campo | Bytes | Contenido |
|---|---|---|
| Magic | 4 | `RZEN` |
| Versión | 1 | `1` |
| KDF | 1 | `1` = PBKDF2-HMAC-SHA256 |
| Iteraciones | 4 | 600 000, big-endian |
| Sal | 16 | Aleatoria por fichero |
| Prefijo de nonce | 4 | Aleatorio por fichero |
| Tamaño de bloque | 4 | 1 048 576 B (1 MiB) en claro |
| Bloques | variable | Texto cifrado + etiqueta GCM de 16 B; el último incluye el SHA-256 del original (32 B) |

- Clave de 256 bits por fichero: `PBKDF2-HMAC-SHA256(contraseña, sal, 600 000)`.
- Nonce de 96 bits: prefijo de 4 B + contador de bloque de 8 B, siempre distinto.
- AAD de cada bloque: cabecera completa + índice de bloque + indicador de último
  bloque. Reordenar, borrar o truncar bloques rompe la autenticación.
- Escritura en `.part` y renombrado atómico; el original nunca se modifica.
- El número de bloques se deduce del tamaño total, así que un fichero múltiplo
  exacto de 1 MiB (1 MiB, 20 MiB…) se recupera sin ambigüedad.

## Pruebas y calidad

```bash
uv run pytest          # cobertura bloqueante: falla por debajo del 80 %
uv run ruff check .
uv run isort . --check-only
```

Última ejecución: **62 pruebas, 0 fallos, 97,7 % de cobertura**. Cubren formato y
cabecera, tamaños de 1 B, 1 MiB exacto, 2 MiB, 20 MiB, 0 B rechazado, contraseña
vacía e incorrecta, byte alterado, bloques reordenados, ficheros truncados, los
ejemplos de Vigenère y César del contrato, los 401 y las cabeceras de descarga.

CI: `.github/workflows/backend.yml` ejecuta `uv sync --locked`, ruff, isort y
pytest con cobertura en cada push a `main`/`develop` y en cada PR, y publica
`coverage.xml` como artefacto. En local, el mismo pipeline se simula con
`.actrc` y `act push -j backend`.

## Hooks

```bash
uv run pre-commit install --hook-type pre-commit --hook-type pre-push
```

- **pre-commit**: espacios finales, fin de fichero, YAML/TOML/JSON, conflictos de
  merge, ficheros grandes, ruff, ruff-format, isort, tests rápidos sin cobertura
  y bloqueo de commits directos en `main` y `develop`.
- **pre-push**: `uv run pytest` con cobertura mínima del 80 % y bloqueo del push
  directo a `main` y `develop` leyendo `PRE_COMMIT_REMOTE_BRANCH`.

## Organización

```text
backend/
  src/runnersz/
    main.py       # Compone FastAPI y registra los handlers de error.
    routes.py     # Endpoints, esquema Bearer, streaming y cabeceras.
    crypto.py     # AES-256-GCM, PBKDF2 y formato RZEN.
    text.py       # Vigenère y César, independientes de HTTP.
    config.py     # Token, límite de subida y carpeta temporal.
    auth.py       # Validación del Bearer en tiempo constante.
    errors.py     # ApiError y handlers {code, message}.
    models.py     # Pydantic: TextRequest.
  tests/          # Algoritmos, formato, API y errores.
  pyproject.toml  # Dependencias, pytest, ruff e isort.
  uv.lock         # Versiones resueltas.
  Dockerfile
  docker-compose.yml
```

La subida se guarda por bloques de 1 MiB en un `.part` con el límite aplicado
mientras se lee, y la respuesta se sirve como fichero, sin cargar el contenido
completo en memoria. Los temporales se borran en éxito y en error.

## Pendientes

- Probar el despliegue Docker en un equipo con Docker (en esta máquina no hay
  motor disponible): `docker compose up -d --build` y `/api/v1/health`.
- Ejecutar `act push -j backend` y adjuntar la evidencia al apartado 11.8.
- Añadir las capturas de `pre-commit run --all-files` en verde.
- Probar 21 MiB con `RZ_MAX_UPLOAD_MIB=20` desde los clientes reales.

## Mantenimiento de esta guía

Por indicación de Alain, **cada cambio de código, configuración o comportamiento
debe actualizar este `backend/README.md` en el mismo commit**, con uso, decisiones,
pruebas y pendientes afectados. El README de la raíz se mantiene vacío por petición
expresa. No añadir funcionalidades fuera de contrato sin indicarlas al humano.

## Historial

- 2026-10-29: inicia API y separa composición, HTTP y algoritmos.
- 2026-10-29: corrige autenticación Bearer de Swagger (`59aba86`).
- 2026-10-29: concentra aquí la guía de uso y mantenimiento; vacía el README raíz.
- 2026-11-09: corrige el último bloque en múltiplos exactos de 1 MiB, nonce de
  96 bits con AAD por índice, errores `{code, message}` del contrato, subida y
  descarga por bloques, cobertura bloqueante al 80 %, workflow de CI, hooks,
  Docker con `uv sync --locked` y `docker-compose` leyendo `.env`.
