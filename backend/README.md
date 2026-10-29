# RunnersZ · API de cifrado

API REST de desarrollo en Python 3.12 con FastAPI. JavaFX y Android envían texto
JSON o contenido de ficheros multipart; los algoritmos residen en el servidor.
El contrato técnico v2 manda sobre las decisiones de implementación.

## Estado actual

Rama: `feature/i02-api-base`. PR: https://github.com/dm2-ethazi/Reto2-RunnersZ/pull/30.
Hay rutas de estado, autenticación, Vigenère, César y ficheros AES.
La API sigue en desarrollo: no se acredita todavía cumplimiento completo del
contrato ni del pliego. No usar este formato `.enc` como formato definitivo.

## Requisitos e instalación

- Python 3.12 gestionado mediante uv.
- uv disponible en la terminal.
- Ejecutar los comandos desde esta carpeta `backend/`.

```powershell
cd C:\Users\dm2\Documents\Reto2-RunnersZ\backend
uv sync --locked
```

No hace falta activar manualmente `.venv`. `uv.lock` fija las dependencias.

## Configuración y arranque en Windows

Crea `.env` dentro de `backend/`, con las siguientes variables. El valor mostrado
es ficticio: sustitúyelo por un token local propio, sin compartirlo ni versionarlo.

```dotenv
RZ_API_TOKEN=CAMBIA_ESTE_VALOR_LOCAL
RZ_MAX_UPLOAD_MIB=100
```

El límite por defecto es 100 MiB. Reinicia la API si cambias el token o el límite:
las rutas actuales cargan estas variables al importar el módulo.

```powershell
# Evita que un token temporal de PowerShell prevalezca sobre .env.
Remove-Item Env:RZ_API_TOKEN -ErrorAction SilentlyContinue

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
| POST | `/files/encrypt` | Multipart: `file`, `password` | Bytes cifrados |
| POST | `/files/decrypt` | Multipart: `file`, `password` | Bytes recuperados y cabecera de integridad |

Ejemplo Vigenère:

```json
{"text":"Hola Mundo, ¿qué tal?","key":"LUNA","mode":"encrypt"}
```

Ejemplo César:

```json
{"text":"Ataque al amanecer","shift":3,"mode":"encrypt"}
```

`mode` selecciona `encrypt` o `decrypt`. César admite enteros de 1 a 25;
Vigenère requiere una clave no vacía formada por letras ASCII. Las letras con
tildes, la Ñ, signos y espacios se conservan sin avanzar la clave (§6.1/§6.3).
La validación exhaustiva del modo y tipos sigue pendiente.

## Organización y decisiones

```text
backend/
  src/runnersz/
    main.py       # Compone FastAPI y registra las rutas.
    routes.py     # Endpoints, esquema Bearer y orquestación HTTP actual.
    crypto.py     # Servicios AES, PBKDF2 y lectura/escritura del formato.
    text.py       # Algoritmos Vigenère y César independientes de HTTP.
    config.py     # Configuración auxiliar pendiente de integrar.
    auth.py       # Autenticación auxiliar pendiente de integrar.
    errors.py     # Errores auxiliares pendientes de integrar.
    models.py     # Modelos auxiliares pendientes de integrar.
  tests/          # Pruebas de algoritmos y autenticación/OpenAPI.
  pyproject.toml  # Dependencias y configuración de pytest.
  uv.lock         # Versiones resueltas.
  Dockerfile
  docker-compose.yml
```

`main.py` se mantiene pequeño para separar la composición de la aplicación de las
rutas y los algoritmos. Los servicios pueden probarse sin arrancar un servidor.
La API comparte la implementación entre clientes y evita duplicar cifrado en ellos.
Los docstrings en español documentan parámetros, resultados y errores; los
comentarios explican decisiones de seguridad y liberación de recursos.

La autenticación activa está en `routes.py`: `HTTPBearer` declara correctamente
el esquema OpenAPI y la comparación del token utiliza `secrets.compare_digest`.
Los módulos auxiliares listados arriba aún no son la implementación activa;
no cambies solo `auth.py` esperando modificar el comportamiento del servidor.

## Errores y diagnóstico

| Caso | Comportamiento actual o comprobación |
|---|---|
| 401 `UNAUTHORIZED` | Revisa que `.env` esté en `backend/`, reinicia con `--env-file .env`, elimina la variable temporal y vuelve a autorizar. |
| 400 `EMPTY_FILE` | Elige un fichero que tenga contenido. |
| 413 `FILE_TOO_LARGE` | El fichero supera el límite configurado. |
| Error al descifrar | Revisa contraseña y fichero; la normalización completa a `DECRYPT_FAILED` está pendiente. |
| 422 por campos ausentes | La validación predeterminada de FastAPI sigue pendiente de adaptar al JSON y estados del contrato. |
| Puerto ocupado | Detén la instancia anterior antes de volver a arrancar. |

Nunca pegues tokens ni contraseñas en incidencias, capturas o comentarios de PR.

## Pruebas y límites conocidos

```powershell
uv run --python 3.12 pytest -q
```

Última verificación del código (commit `59aba86`): **15 passed**, con un aviso de
obsolescencia de Starlette/AnyIO. Incluye autenticación válida/inválida/ausente y
esquema Bearer en OpenAPI. No acredita todos los criterios de aceptación.

Pendientes contractuales que no deben darse por hechos:

- Recepción y respuesta de ficheros en streaming con memoria acotada (§5.1/§7.1).
- Revisión del formato RZEN, nonce, AAD, bloques exactos y truncados (§5.3/§6.2).
- Pruebas reales de 20 MiB, 21 MiB con límite de 20 MiB, y todos los casos de §7.3.
- Gestión uniforme de errores y limpieza ante disco lleno o desconexión (§4/§7.2).
- Integración de módulos auxiliares y configuración completa de calidad (§11.2–§11.6).
- Validación del despliegue Docker; el comando local anterior es el camino verificado.

## Mantenimiento de esta guía

Por indicación de Alain, **cada cambio de código, configuración o comportamiento
debe actualizar este `backend/README.md` en el mismo commit**, con uso, decisiones,
pruebas y pendientes afectados. El README de la raíz se mantiene vacío por petición
expresa. No añadir funcionalidades fuera de contrato sin indicarlas al humano.

## Historial

- 2026-10-29: inicia API y separa composición, HTTP y algoritmos.
- 2026-10-29: corrige autenticación Bearer de Swagger (`59aba86`).
- 2026-10-29: concentra aquí la guía de uso y mantenimiento; vacía el README raíz.
