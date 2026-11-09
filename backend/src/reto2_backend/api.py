from typing import Literal

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

from . import __version__
from .aes_file import FileResult, cifrar_fichero, descifrar_fichero
from .errors import Reto2Error
from .vigenere import transformar

SERVICE_NAME = "reto2-backend"


class AesRequest(BaseModel):
    ruta_entrada: str = Field(min_length=1)
    ruta_salida: str = Field(min_length=1)
    contrasena: str


class AesResponse(BaseModel):
    ruta_salida: str
    bytes_entrada: int
    bytes_salida: int
    sha256: str
    integridad_verificada: bool | None = None


class VigenereRequest(BaseModel):
    texto: str
    clave: str
    modo: Literal["cifrar", "descifrar"] = "cifrar"


class VigenereResponse(BaseModel):
    resultado: str


class HealthResponse(BaseModel):
    estado: str
    servicio: str
    version: str


def _file_response(result: FileResult) -> AesResponse:
    return AesResponse(
        ruta_salida=result.ruta_salida,
        bytes_entrada=result.bytes_entrada,
        bytes_salida=result.bytes_salida,
        sha256=result.sha256,
        integridad_verificada=result.integridad_verificada,
    )


def create_app() -> FastAPI:
    app = FastAPI(
        title="Reto2 RunnersZ Backend",
        version=__version__,
        docs_url="/docs",
        redoc_url=None,
    )

    @app.exception_handler(Reto2Error)
    async def reto2_error_handler(_: Request, exc: Reto2Error) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": {"codigo": exc.code, "mensaje": exc.message}},
        )

    @app.get("/health", response_model=HealthResponse)
    def health() -> HealthResponse:
        return HealthResponse(estado="ok", servicio=SERVICE_NAME, version=__version__)

    @app.post("/api/aes/cifrar", response_model=AesResponse)
    def aes_cifrar(payload: AesRequest) -> AesResponse:
        return _file_response(
            cifrar_fichero(payload.ruta_entrada, payload.ruta_salida, payload.contrasena)
        )

    @app.post("/api/aes/descifrar", response_model=AesResponse)
    def aes_descifrar(payload: AesRequest) -> AesResponse:
        return _file_response(
            descifrar_fichero(payload.ruta_entrada, payload.ruta_salida, payload.contrasena)
        )

    @app.post("/api/vigenere", response_model=VigenereResponse)
    def vigenere(payload: VigenereRequest) -> VigenereResponse:
        return VigenereResponse(resultado=transformar(payload.texto, payload.clave, payload.modo))

    return app


app = create_app()
