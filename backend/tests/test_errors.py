"""Handlers de error y configuración centralizada."""

from __future__ import annotations

import asyncio

from runnersz.config import Settings, load_settings
from runnersz.errors import (
    ApiError,
    api_error_handler,
    internal_error_handler,
    validation_error_handler,
)


def test_api_error_handler():
    """Un ApiError se serializa con su estado, código y mensaje."""
    response = asyncio.run(
        api_error_handler(None, ApiError("EMPTY_FILE", 400, "vacío"))
    )
    assert response.status_code == 400


def test_internal_error_handler():
    """Un fallo inesperado no filtra el detalle técnico."""
    response = asyncio.run(internal_error_handler(None, RuntimeError("disco lleno")))
    assert response.status_code == 500
    assert b"disco lleno" not in response.body


def test_validation_error_handler_prioriza_el_campo():
    """El campo inválido decide el código: shift -> INVALID_SHIFT."""

    class FakeValidationError(Exception):
        def errors(self):
            return [{"loc": ("body", "shift"), "msg": "no es un entero"}]

    response = asyncio.run(validation_error_handler(None, FakeValidationError()))
    assert response.status_code == 400
    assert b"INVALID_SHIFT" in response.body


def test_settings_y_limites(monkeypatch):
    """El límite cae al valor por defecto si la variable no es válida."""
    monkeypatch.setenv("RZ_API_TOKEN", "abc")
    monkeypatch.setenv("RZ_MAX_UPLOAD_MIB", "no-es-un-numero")
    settings = load_settings()
    assert settings.api_token == "abc"
    assert settings.max_upload_mib == 100
    assert settings.max_upload_bytes == 100 * 1024 * 1024

    monkeypatch.setenv("RZ_MAX_UPLOAD_MIB", "0")
    assert load_settings().max_upload_mib == 100

    monkeypatch.setenv("RZ_MAX_UPLOAD_MIB", "20")
    assert load_settings().max_upload_bytes == 20 * 1024 * 1024


def test_settings_por_defecto(monkeypatch):
    """Sin variables de entorno no hay token y se usa la carpeta temporal."""
    monkeypatch.delenv("RZ_API_TOKEN", raising=False)
    monkeypatch.delenv("RZ_MAX_UPLOAD_MIB", raising=False)
    monkeypatch.delenv("RZ_TMP_DIR", raising=False)
    settings = load_settings()
    assert settings.api_token == ""
    assert settings.max_upload_mib == 100
    assert Settings("").max_upload_bytes == 100 * 1024 * 1024
