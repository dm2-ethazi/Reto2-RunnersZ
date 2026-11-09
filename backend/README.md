# Backend — Python 3.12

API FastAPI local. Solo escucha en 127.0.0.1 y exige puerto por argumento.
El código está en `src/reto2_backend`; las pruebas en `tests`.

## Preparar y probar (desde la raíz)

```powershell
py -3.12 scripts/prepare_python.py
backend\.venv\Scripts\python.exe -m pytest -c backend/pyproject.toml backend/tests --cov=backend/src/reto2_backend --cov-report=xml --cov-fail-under=80
backend\.venv\Scripts\python.exe -m ruff check backend
$env:PYTHONPATH = (Resolve-Path backend/src).Path
backend\.venv\Scripts\python.exe -m reto2_backend --port 8765
```

GET `/health`; contratos interactivos en `/docs` mientras el proceso está activo.
POST `/api/cifrar` y `/api/descifrar` reciben JSON con `ruta_entrada`,
`ruta_salida` y `contrasena`. POST `/api/vigenere` recibe `texto`, `clave`
y `modo` (`cifrar`/`descifrar`). No se envían contenidos de ficheros: el servidor
abre rutas del mismo equipo. No exponer esta API en una interfaz pública.

## Organización

- `aes_file.py`: streaming, temporales exclusivos y verificación de SHA-256.
- `enc_format.py`: cabecera, límites, nonce y AAD comunes con Android.
- `vigenere.py`: solo ASCII A-Z/a-z consume clave; conserva caso y demás signos.
- `caesar.py`: desplazamiento entero 1..25; rechaza bool, cero y valores inválidos.
- `api.py`/`errors.py`: validación JSON y errores controlados sin revelar claves.

AES-256-GCM: bloques de 1 MiB, PBKDF2-HMAC-SHA256 de 600000 iteraciones.
Una contraseña vacía es inválida; una corta sigue siendo débil. Los errores
eliminan solo el temporal de la operación. No se toca el original ni se
reemplazan salidas existentes, incluso si aparecen durante la publicación.
Windows usa rename sin reemplazo; POSIX enlace duro atómico y retirada del temporal.

Formato exacto y vectores: `../docs/formato-enc-v1.md`.
Las pruebas incluyen 0 B, bloques exactos, multibloque, contraseña incorrecta,
truncado, corrupción y carrera de escritores. La prueba grande está marcada slow.
