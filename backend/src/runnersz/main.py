"""Composición de la aplicación; las rutas residen en routes.

Autor: Alain. Versión: 2.0.
"""
from fastapi import FastAPI
from .routes import router

# La composición queda separada de HTTP y de los algoritmos.
app = FastAPI(title="RunnersZ API", version="2.0.0")
app.include_router(router)
