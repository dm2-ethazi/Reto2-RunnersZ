"""Modelos de entrada de la API (§3.3)."""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


class TextRequest(BaseModel):
    """Petición de transformación textual para Vigenère y César."""

    text: str = Field(description="Texto que se va a transformar")
    key: str | None = Field(default=None, description="Clave de Vigenère (A-Z)")
    # Any deja que el validador devuelva INVALID_SHIFT en lugar de un 422.
    shift: Any = Field(default=None, description="Desplazamiento de César entre 1 y 25")
    mode: Literal["encrypt", "decrypt"] = Field(
        default="encrypt", description="Sentido de la transformación"
    )
