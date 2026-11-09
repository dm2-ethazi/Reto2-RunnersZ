"""Configuración centralizada del servidor (§3.2)."""

from __future__ import annotations

import os
import tempfile
from dataclasses import dataclass
from pathlib import Path

DEFAULT_MAX_UPLOAD_MIB = 100
VERSION = "2.0.0"


def _upload_limit_mib() -> int:
    """Lee RZ_MAX_UPLOAD_MIB admitiendo solo enteros positivos."""
    try:
        value = int(os.getenv("RZ_MAX_UPLOAD_MIB", str(DEFAULT_MAX_UPLOAD_MIB)))
    except ValueError:
        return DEFAULT_MAX_UPLOAD_MIB
    return value if value > 0 else DEFAULT_MAX_UPLOAD_MIB


def _temporary_directory() -> Path:
    """Carpeta de los temporales .part; configurable para el volumen Docker."""
    return Path(os.getenv("RZ_TMP_DIR") or tempfile.gettempdir())


@dataclass(frozen=True)
class Settings:
    """Valores de ejecución del servidor."""

    api_token: str
    max_upload_mib: int = DEFAULT_MAX_UPLOAD_MIB
    version: str = VERSION
    tmp_dir: Path = Path(tempfile.gettempdir())

    @property
    def max_upload_bytes(self) -> int:
        """Devuelve el límite de subida en bytes."""
        return self.max_upload_mib * 1024 * 1024


def load_settings() -> Settings:
    """Carga la configuración sin registrar ni devolver secretos."""
    return Settings(
        api_token=os.getenv("RZ_API_TOKEN", ""),
        max_upload_mib=_upload_limit_mib(),
        tmp_dir=_temporary_directory(),
    )
