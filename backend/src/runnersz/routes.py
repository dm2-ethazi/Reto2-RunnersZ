"""Endpoints HTTP de RunnersZ (§3.3).

Los algoritmos viven en text y crypto; aquí solo se valida, se autentica y se
orquesta el tratamiento de ficheros con temporales que se borran siempre.
"""

from __future__ import annotations

import re
import tempfile
from pathlib import Path

from fastapi import APIRouter, Depends, File, Form, UploadFile
from fastapi.responses import FileResponse, Response
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from starlette.background import BackgroundTask

from .auth import require_token
from .config import Settings, load_settings
from .crypto import CryptoError, decrypt_file, encrypt_file
from .errors import ApiError
from .models import TextRequest
from .text import caesar, vigenere

router = APIRouter()
bearer_scheme = HTTPBearer(auto_error=False)

CHUNK_SIZE = 1024 * 1024
MESSAGES = {
    "EMPTY_FILE": "El archivo está vacío",
    "EMPTY_PASSWORD": "La contraseña está vacía",
    "FILE_TOO_LARGE": "El archivo supera el tamaño máximo permitido",
    "INVALID_KEY": "La clave no es válida: usa solo letras A-Z",
    "INVALID_SHIFT": "El desplazamiento debe ser un entero entre 1 y 25",
    "DECRYPT_FAILED": "clave incorrecta o fichero dañado",
}
_SHIFT_PATTERN = re.compile(r"^[+-]?\d+$")


def authorization_header(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> str | None:
    """Extrae la cabecera Bearer para declararla en OpenAPI.

    Args:
        credentials: Credenciales que envía el cliente, si existen.

    Returns:
        Cabecera normalizada o None; la validación real la hace require_token.
    """
    if credentials is None:
        return None
    return f"Bearer {credentials.credentials}"


def _error(code: str, status: int) -> ApiError:
    """Construye un ApiError con el mensaje del contrato."""
    return ApiError(code, status, MESSAGES[code])


def _shift_value(value: object) -> int:
    """Convierte y valida el desplazamiento de César.

    Args:
        value: Valor recibido en JSON, número o texto.

    Returns:
        Desplazamiento entre 1 y 25.

    Raises:
        ApiError: Si no es numérico o queda fuera de rango (INVALID_SHIFT).
    """
    if isinstance(value, bool) or value is None:
        raise _error("INVALID_SHIFT", 400)
    if isinstance(value, int):
        shift = value
    elif isinstance(value, str) and _SHIFT_PATTERN.match(value.strip()):
        shift = int(value.strip())
    else:
        raise _error("INVALID_SHIFT", 400)
    if not 1 <= shift <= 25:
        raise _error("INVALID_SHIFT", 400)
    return shift


def _suggested_name(filename: str | None, decrypting: bool) -> str:
    """Propone un nombre de descarga sin rutas ni sobrescrituras."""
    name = Path(filename).name if filename else "archivo"
    if decrypting:
        return name[:-4] if name.lower().endswith(".enc") else f"{name}.recuperado"
    return f"{name}.enc"


async def _spool(file: UploadFile, settings: Settings) -> Path:
    """Guarda la subida por bloques en un temporal .part acotado.

    Args:
        file: Fichero multipart recibido.
        settings: Configuración con el límite y la carpeta temporal.

    Returns:
        Ruta del .part ya escrito.

    Raises:
        ApiError: Fichero vacío (EMPTY_FILE) o excesivo (FILE_TOO_LARGE).
    """
    descriptor, name = tempfile.mkstemp(
        prefix="rz-", suffix=".part", dir=settings.tmp_dir
    )
    path = Path(name)
    total = 0
    try:
        with open(descriptor, "wb") as target:
            while chunk := await file.read(CHUNK_SIZE):
                total += len(chunk)
                if total > settings.max_upload_bytes:
                    raise _error("FILE_TOO_LARGE", 413)
                target.write(chunk)
    except BaseException:
        path.unlink(missing_ok=True)
        raise
    if total == 0:
        path.unlink(missing_ok=True)
        raise _error("EMPTY_FILE", 400)
    return path


async def _file_operation(
    file: UploadFile, password: str, authorization: str | None, decrypting: bool
) -> FileResponse:
    """Orquesta token, límite, cifrado o descifrado y limpieza de temporales.

    Args:
        file: Fichero multipart.
        password: Contraseña AES de la operación.
        authorization: Cabecera Bearer.
        decrypting: Selecciona descifrado frente a cifrado.

    Returns:
        Respuesta de descarga con nombre sugerido y tamaño.

    Raises:
        ApiError: Cualquier error de autenticación o de la operación.
    """
    settings = load_settings()
    require_token(settings, authorization)
    if not password:
        raise _error("EMPTY_PASSWORD", 400)

    source = await _spool(file, settings)
    destination = source.with_suffix(".out")
    try:
        (decrypt_file if decrypting else encrypt_file)(source, destination, password)
    except CryptoError as exc:
        if decrypting:
            raise _error("DECRYPT_FAILED", 422) from exc
        raise ApiError("INTERNAL_ERROR", 500, "Error interno del servidor") from exc
    finally:
        source.unlink(missing_ok=True)

    headers = {"X-Integrity": "verified"} if decrypting else {}
    return FileResponse(
        destination,
        media_type="application/octet-stream",
        filename=_suggested_name(file.filename, decrypting),
        headers=headers,
        background=BackgroundTask(destination.unlink, missing_ok=True),
    )


@router.get("/api/v1/health")
def health() -> dict[str, str]:
    """Estado público del servicio; no exige token."""
    settings = load_settings()
    return {"status": "ok", "version": settings.version}


@router.get("/api/v1/auth/check", status_code=204)
def check_token(
    authorization: str | None = Depends(authorization_header),
) -> Response:
    """Comprueba el token: 204 si es válido, 401 si no."""
    require_token(load_settings(), authorization)
    return Response(status_code=204)


@router.post("/api/v1/text/vigenere")
def vigenere_text(
    payload: TextRequest,
    authorization: str | None = Depends(authorization_header),
) -> dict[str, str]:
    """Cifra o descifra texto con Vigenère (§6.1)."""
    require_token(load_settings(), authorization)
    try:
        result = vigenere(payload.text, payload.key or "", payload.mode == "decrypt")
    except ValueError as exc:
        raise _error("INVALID_KEY", 400) from exc
    return {"result": result}


@router.post("/api/v1/text/caesar")
def caesar_text(
    payload: TextRequest,
    authorization: str | None = Depends(authorization_header),
) -> dict[str, str]:
    """Cifra o descifra texto con César (§6.3)."""
    require_token(load_settings(), authorization)
    shift = _shift_value(payload.shift)
    return {"result": caesar(payload.text, shift, payload.mode == "decrypt")}


@router.post("/api/v1/files/encrypt")
async def encrypt_endpoint(
    file: UploadFile = File(...),
    password: str = Form(...),
    authorization: str | None = Depends(authorization_header),
) -> FileResponse:
    """Cifra un fichero con AES-256-GCM y devuelve el .enc."""
    return await _file_operation(file, password, authorization, decrypting=False)


@router.post("/api/v1/files/decrypt")
async def decrypt_endpoint(
    file: UploadFile = File(...),
    password: str = Form(...),
    authorization: str | None = Depends(authorization_header),
) -> FileResponse:
    """Descifra un .enc verificando integridad y devuelve los bytes originales."""
    return await _file_operation(file, password, authorization, decrypting=True)
