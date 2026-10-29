"""Autenticación por token."""
import secrets
from .config import Settings
from .errors import ApiError
def require_token(settings: Settings, authorization: str | None) -> None:
    """Valida Bearer usando comparación constante."""
    expected=f"Bearer {settings.api_token}" if settings.api_token else ""
    if not settings.api_token or not authorization or not secrets.compare_digest(authorization,expected): raise ApiError("UNAUTHORIZED",401,"Token no válido")
