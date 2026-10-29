"""Modelos de entrada."""
from typing import Literal
from pydantic import BaseModel
class TextRequest(BaseModel):
    """Petición de transformación textual."""
    text: str
    key: str|None=None
    shift: int|None=None
    mode: Literal["encrypt","decrypt"]="encrypt"
