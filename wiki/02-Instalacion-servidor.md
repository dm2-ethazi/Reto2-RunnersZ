# 02 · Instalación del servidor (API)

El servidor concentra **toda** la criptografía: Vigenère, César y AES-256-GCM por bloques de 1 MiB. Los clientes solo hablan con él por HTTP.

Tienes dos formas de arrancarlo. Empieza por la **A** (desarrollo y demo local); la **B** es el despliegue reproducible con Docker.

---

## Antes de empezar

- Haber completado [01 · Requisitos](01-Requisitos.md) (`git`, `uv`).
- Trabajar desde la carpeta `backend/` del repositorio.
- Elegir un **token** propio: una cadena larga y sin espacios, por ejemplo `demo-runnersz-2026-4f7a`. No lo compartas ni lo subas a Git.

---

## Opción A · Arranque local con uv (recomendado)

### Paso 1 — Instalar dependencias

```powershell
cd <ruta-del-repositorio>\backend
uv sync --locked
```

`uv sync --locked` instala Python 3.12 y las dependencias exactas de `uv.lock`. Si el lock no está al día, **falla a propósito**, igual que en el CI.

✅ Debe terminar con `Installed ...` / `Audited ...` y crear `backend/.venv`.

### Paso 2 — Crear `backend/.env`

Crea el archivo a mano con **exactamente estas dos líneas**:

```dotenv
RZ_API_TOKEN=demo-runnersz-2026-4f7a
RZ_MAX_UPLOAD_MIB=100
```

> `.env.example` ya trae solo las dos variables con valores ficticios (`change-me` y `100`). Al copiarlo, cambia `RZ_API_TOKEN` por un valor propio y no lo compartas.

Variables disponibles:

| Variable | Obligatoria | Por defecto | Para qué |
|---|---|---|---|
| `RZ_API_TOKEN` | Sí | vacío (todo devuelve 401) | Token Bearer que deben enviar los clientes |
| `RZ_MAX_UPLOAD_MIB` | No | `100` | Límite de subida en MiB |
| `RZ_TMP_DIR` | No | carpeta temporal del sistema | Dónde se escriben los `.part` |

La configuración se lee **en cada petición**, así que cambiar el token o el límite no requiere reiniciar… salvo cuando se cambia el archivo `.env` que se pasa con `--env-file`: ahí sí hay que reiniciar el proceso.

### Paso 3 — Arrancar el servidor

```powershell
uv run --python 3.12 uvicorn runnersz.main:app --app-dir src --env-file .env --host 127.0.0.1 --port 8000 --reload
```

Linux/macOS: el mismo comando. Si tienes una variable de entorno con otro token, límpiala antes con `unset RZ_API_TOKEN`.

Deja la terminal abierta. Para parar: **Ctrl+C**.

- `--reload` reinicia al guardar código (solo desarrollo).
- `127.0.0.1` permite probar desde este PC. Para otro dispositivo de la red local cambia a `--host 0.0.0.0` y abre el puerto 8000 en el cortafuegos.

✅ Debes ver `Uvicorn running on http://127.0.0.1:8000` y `Application startup complete`.

### Paso 4 — Comprobar la salud

Abre en el navegador:

<http://127.0.0.1:8000/api/v1/health>

✅ Respuesta esperada:

```json
{"status":"ok","version":"2.0.0"}
```

Esta ruta **no** pide token.

### Paso 5 — Probar la autenticación

```powershell
curl.exe -i http://127.0.0.1:8000/api/v1/auth/check -H "Authorization: Bearer demo-runnersz-2026-4f7a"
```

✅ Debe responder `HTTP/1.1 204 No Content` (sin cuerpo).

Sin la cabecera debe responder `401` con:

```json
{"code":"UNAUTHORIZED","message":"Token no válido. Revisa los ajustes"}
```

### Paso 6 — Usar Swagger (opcional pero útil)

1. Abre <http://127.0.0.1:8000/docs>.
2. Pulsa **Authorize**.
3. Escribe **solo el token** (`demo-runnersz-2026-4f7a`), **sin** `Bearer`. Swagger añade el prefijo solo.
4. Ejecuta `GET /api/v1/auth/check` → 204.
5. Prueba `POST /api/v1/files/encrypt`: **Try it out**, elige un archivo no vacío y escribe la contraseña.

Recorrido completo de rutas y ejemplos: [06 · Uso de la API](06-Uso-servidor.md).

---

## Opción B · Docker Compose

### Paso 1 — Preparar el entorno

```bash
cd backend
cp .env.example .env
```

Edita `.env` y pon tu token:

```dotenv
RZ_API_TOKEN=demo-runnersz-2026-4f7a
RZ_MAX_UPLOAD_MIB=100
```

> `.env` es **opcional** en Docker: `docker-compose.yml` trae valores de reserva (`RZ_API_TOKEN=change-me`) para que el servicio arranque recién clonado. Para la demo, define siempre tu token: el de reserva es público.

### Paso 2 — Construir y levantar

```bash
docker compose up -d --build
```

Qué hace la imagen (`backend/Dockerfile`):

- Base `python:3.12-slim`.
- Instala `uv` y ejecuta `uv sync --locked` (dependencias fijadas por el contrato).
- Corre como usuario sin privilegios `appuser` (UID 10001).
- Guarda los temporales en un `tmpfs` efímero (`/var/tmp/runnersz`).
- Expone el puerto **8000** y declara un `HEALTHCHECK` contra `/api/v1/health` cada 10 s.

### Paso 3 — Comprobar

```bash
docker compose ps
docker compose logs -f server
```

✅ El servicio `server` debe aparecer como `running` / `healthy`.

```bash
curl http://127.0.0.1:8000/api/v1/health
```

✅ `{"status":"ok","version":"2.0.0"}`

### Paso 4 — Parar y limpiar

```bash
docker compose down          # para y elimina el contenedor
docker compose down --rmi local   # además borra la imagen construida
```

> Si cambias `.env`, recrea el contenedor: `docker compose up -d --force-recreate`.

---

## Publicar la API en Internet (como la demo)

La app Android apunta a una instancia pública: `https://runnersz-api.onrender.com`. Para tener una propia necesitas un host que ejecute el `Dockerfile` (Render, Fly.io, Railway, un servidor con Docker…):

1. Sube el repositorio a tu cuenta (o conecta el host al repo).
2. Configura el servicio como **Docker**, contexto `backend/`.
3. Define la variable de entorno `RZ_API_TOKEN` **en el panel del host**, nunca en el repositorio.
4. Publica y comprueba `https://<tu-dominio>/api/v1/health`.
5. Si vas a usar la app Android con tu servidor, tendrás que recompilarla con tu URL y tu token: [12 · Personalizar el backend y el token](12-Personalizar-backend-y-token.md).

> El despliegue en Render se hizo **fuera** del repositorio: no hay `render.yaml` versionado. El contrato solo exige que la API esté disponible para la demo.

---

## Pruebas y calidad del servidor

```bash
cd backend
uv run pytest                 # 62 pruebas + cobertura mínima del 80 %
uv run ruff check .           # estilo
uv run isort . --check-only   # orden de imports
```

✅ Resultado verificado el 12/11/2026:

```text
TOTAL                        349      8    98%
Required test coverage of 80% reached. Total coverage: 97.71%
62 passed, 1 warning in 8.57s
```

El informe HTML/XML queda en `backend/coverage.xml`. El CI (`.github/workflows/backend.yml`) ejecuta lo mismo en cada push y PR, y publica el XML como artefacto.

---

## Qué hay dentro de `backend/`

```text
backend/
  src/runnersz/
    main.py      # compone FastAPI y registra los manejadores de error
    routes.py    # endpoints, esquema Bearer, streaming y cabeceras
    crypto.py    # AES-256-GCM, PBKDF2 y formato RZEN
    text.py      # Vigenère y César, sin dependencias de HTTP
    config.py    # token, límite de subida y carpeta temporal
    auth.py      # validación del Bearer en tiempo constante
    errors.py    # ApiError y respuestas {code, message}
    models.py    # Pydantic: TextRequest
  tests/         # 62 pruebas
  Dockerfile
  docker-compose.yml
  pyproject.toml
  uv.lock
```

---

## Siguiente paso

- Usar la API: [06 · Uso de la API](06-Uso-servidor.md)
- Conectar el escritorio: [03 · Instalación del escritorio](03-Instalacion-escritorio.md)
- Conectar la app Android: [04 · Instalación de Android](04-Instalacion-android.md)

[Volver al índice](Home.md)
