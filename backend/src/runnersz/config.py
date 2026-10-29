"""Configuración centralizada del servidor."""
from dataclasses import dataclass
import os
@dataclass(frozen=True)
class Settings:
    """Valores de ejecución del servidor."""
    api_token: str
    max_upload_mib: int = 100
    version: str = "2.0.0"
    @property
    def max_upload_bytes(self) -> int:
        """Devuelve el límite de subida en bytes."""
        return self.max_upload_mib * 1024 * 1024
def load_settings() -> Settings:
    """Carga configuración sin exponer secretos en logs."""
    return Settings(os.getenv("RZ_API_TOKEN", ""), int(os.getenv("RZ_MAX_UPLOAD_MIB", "100")))
