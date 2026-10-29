"""Capa routes.py de RunnersZ.

Autor: Alain. Versión: 2.0."""
import os, tempfile
import secrets
from pathlib import Path
from fastapi import APIRouter, Depends, File, Form, UploadFile
from fastapi.responses import Response, JSONResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel
from .crypto import CryptoError, encrypt_file, decrypt_file
from .text import vigenere, caesar
router = APIRouter()
TOKEN = os.getenv('RZ_API_TOKEN', '')
LIMIT = int(os.getenv('RZ_MAX_UPLOAD_MIB', '100')) * 1048576


bearer_scheme = HTTPBearer(auto_error=False)

def authorization_header(credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme)) -> str | None:
    """Extrae Bearer y declara la autenticación en OpenAPI.

    Args:
        credentials: Credenciales enviadas por el cliente, si existen.
    Returns:
        Cabecera normalizada o None; auth conserva el error 401 contractual.
    """
    if credentials is None:
        return None
    return f"Bearer {credentials.credentials}"

class Text(BaseModel):
    """Datos de entrada de los endpoints textuales."""
    text: str
    key: str | None = None
    shift: int | None = None
    mode: str = 'encrypt'

def auth(a):
    """Valida la cabecera de autorización.

Args:
    a: Cabecera Bearer recibida.
Raises:
    ValueError: Si falta el token o es incorrecto."""
    # §3.4: se comparan bytes para admitir UTF-8 en tiempo constante.
    if not TOKEN or a is None or not secrets.compare_digest(a.encode('utf-8'), f'Bearer {TOKEN}'.encode('utf-8')):
        raise ValueError('UNAUTHORIZED')

def fail(code, status):
    """Construye un error JSON uniforme.

Args:
    code: Código público de error.
    status: Estado HTTP.
Returns:
    Respuesta JSON sin credenciales."""
    return JSONResponse({'code': code, 'message': code}, status_code=status)

@router.get('/api/v1/health')
def health():
    """Devuelve el estado y la versión del servicio público."""
    return {'status': 'ok', 'version': '2.0.0'}

@router.get('/api/v1/auth/check', status_code=204)
def check(authorization: str | None=Depends(authorization_header)):
    """Comprueba el token.

Args:
    authorization: Cabecera Bearer.
Returns:
    Respuesta vacía o error de autorización."""
    try:
        auth(authorization)
    except ValueError:
        return fail('UNAUTHORIZED', 401)

async def upload(file):
    """Recibe un fichero temporal.

Args:
    file: Fichero multipart recibido.
Returns:
    Ruta temporal y error opcional.
Nota:
    Pendiente migrar la recepción a streaming acotado (§5.1)."""
    data = await file.read(LIMIT + 1)
    if not data:
        return (None, 'EMPTY_FILE')
    if len(data) > LIMIT:
        return (None, 'FILE_TOO_LARGE')
    p = Path(tempfile.mktemp())
    p.write_bytes(data)
    return (p, None)

@router.post('/api/v1/text/vigenere')
def vig(x: Text, authorization: str | None=Depends(authorization_header)):
    """Atiende la transformación Vigenère (§6.1).

Args:
    x: Texto, clave y modo.
    authorization: Cabecera Bearer.
Returns:
    Resultado o error JSON."""
    try:
        auth(authorization)
        return {'result': vigenere(x.text, x.key or '', x.mode == 'decrypt')}
    except ValueError as e:
        return fail(str(e), 401 if str(e) == 'UNAUTHORIZED' else 400)

@router.post('/api/v1/text/caesar')
def ces(x: Text, authorization: str | None=Depends(authorization_header)):
    """Atiende la transformación César (§6.3).

Args:
    x: Texto, desplazamiento y modo.
    authorization: Cabecera Bearer.
Returns:
    Resultado o error JSON."""
    try:
        auth(authorization)
        return {'result': caesar(x.text, x.shift or 0, x.mode == 'decrypt')}
    except ValueError as e:
        return fail(str(e), 401 if str(e) == 'UNAUTHORIZED' else 400)

async def fileop(file, password, authorization, dec):
    """Orquesta el servicio de ficheros y libera temporales.

Args:
    file: Entrada multipart.
    password: Contraseña de la operación.
    authorization: Cabecera Bearer.
    dec: Selecciona descifrado.
Returns:
    Bytes procesados o error público."""
    try:
        auth(authorization)
    except ValueError:
        return fail('UNAUTHORIZED', 401)
    src, err = await upload(file)
    if err:
        return fail(err, 413 if err == 'FILE_TOO_LARGE' else 400)
    dst = src.with_suffix('.out')
    try:
        (decrypt_file if dec else encrypt_file)(src, dst, password)
        return Response(dst.read_bytes(), media_type='application/octet-stream', headers={'X-Integrity': 'verified'})
    except CryptoError as e:
        return fail(str(e), 400 if str(e) in ('EMPTY_PASSWORD', 'EMPTY_FILE') else 422)
    # Se liberan recursos tanto en éxito como en error (§4).
    finally:
        src.unlink(missing_ok=True)
        dst.unlink(missing_ok=True)
        dst.with_name(dst.name + '.part').unlink(missing_ok=True)

@router.post('/api/v1/files/encrypt')
async def enc(file: UploadFile=File(...), password: str=Form(...), authorization: str | None=Depends(authorization_header)):
    """Expone el cifrado multipart; delega en fileop."""
    return await fileop(file, password, authorization, False)

@router.post('/api/v1/files/decrypt')
async def dec(file: UploadFile=File(...), password: str=Form(...), authorization: str | None=Depends(authorization_header)):
    """Expone el descifrado multipart; delega en fileop."""
    return await fileop(file, password, authorization, True)
