"""Errores HTTP homogéneos con forma {code, message} (§3.3)."""

from __future__ import annotations

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

INTERNAL_MESSAGE = "Error interno del servidor"
VALIDATION_MESSAGE = "La petición no es válida"
# Campos que el contrato asocia a un código concreto cuando llegan mal.
FIELD_CODES = {"shift": "INVALID_SHIFT", "key": "INVALID_KEY"}


class ApiError(Exception):
    """Error controlado que se serializa sin credenciales ni detalles internos."""

    def __init__(self, code: str, status: int, message: str) -> None:
        """Guarda el código del contrato, el estado HTTP y el mensaje visible."""
        super().__init__(code)
        self.code = code
        self.status = status
        self.message = message


def _payload(code: str, message: str, status: int) -> JSONResponse:
    """Construye la respuesta JSON homogénea {code, message}."""
    return JSONResponse({"code": code, "message": message}, status_code=status)


async def api_error_handler(request: Request, exc: ApiError) -> JSONResponse:
    """Serializa un ApiError con su estado y mensaje."""
    return _payload(exc.code, exc.message, exc.status)


async def validation_error_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    """Convierte los 422 de FastAPI en 400 {code, message} del contrato."""
    code = "INVALID_REQUEST"
    for error in exc.errors():
        field = str(error.get("loc", ("",))[-1])
        if field in FIELD_CODES:
            code = FIELD_CODES[field]
            break
    return _payload(code, VALIDATION_MESSAGE, 400)


async def internal_error_handler(request: Request, exc: Exception) -> JSONResponse:
    """Oculta el detalle técnico de un fallo inesperado."""
    return _payload("INTERNAL_ERROR", INTERNAL_MESSAGE, 500)
