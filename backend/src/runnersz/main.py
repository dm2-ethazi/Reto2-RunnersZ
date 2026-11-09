"""Composición de la aplicación; las rutas residen en routes.

Autor: Alain. Versión: 2.0.
"""

from __future__ import annotations

from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError

from .config import VERSION
from .errors import (
    ApiError,
    api_error_handler,
    internal_error_handler,
    validation_error_handler,
)
from .routes import router

# La composición queda separada de HTTP y de los algoritmos.
app = FastAPI(title="RunnersZ API", version=VERSION)
app.add_exception_handler(ApiError, api_error_handler)
app.add_exception_handler(RequestValidationError, validation_error_handler)
app.add_exception_handler(Exception, internal_error_handler)
app.include_router(router)
