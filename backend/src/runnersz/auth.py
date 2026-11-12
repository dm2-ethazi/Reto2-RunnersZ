"""Autenticación por token Bearer con comparación en tiempo constante (§3.4)."""

from __future__ import annotations

import secrets

from .config import Settings
from .errors import ApiError

UNAUTHORIZED_MESSAGE = "Token no válido. Revisa los ajustes"


def require_token(settings: Settings, authorization: str | None) -> None:
    """Valida la cabecera Authorization frente al token configurado.

    Args:
        settings: Configuración con el token esperado.
        authorization: Cabecera recibida, por ejemplo «Bearer abc».

    Raises:
        ApiError: Si falta el token configurado, no llega la cabecera o no
            coincide; siempre con estado 401 y código UNAUTHORIZED.
    """
    expected = f"Bearer {settings.api_token}" if settings.api_token else ""
    if (
        not settings.api_token
        or not authorization
        or not secrets.compare_digest(
            authorization.encode("utf-8"), expected.encode("utf-8")
        )
    ):
        raise ApiError("UNAUTHORIZED", 401, UNAUTHORIZED_MESSAGE)
