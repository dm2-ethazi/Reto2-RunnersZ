"""Errores HTTP homogéneos."""
from fastapi import Request
from fastapi.responses import JSONResponse
class ApiError(Exception):
    """Error controlado convertido a JSON."""
    def __init__(self, code: str, status: int, message: str): self.code,self.status,self.message=code,status,message
async def api_error_handler(request: Request, exc: ApiError) -> JSONResponse:
    """Serializa el error sin credenciales."""
    return JSONResponse(exc.status, {"code": exc.code, "message": exc.message})
