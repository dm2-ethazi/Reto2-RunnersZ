"""Fixtures compartidas: cliente HTTP con token de prueba."""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from runnersz.main import app

TOKEN = "token-de-prueba"


@pytest.fixture
def client(monkeypatch):
    """Cliente con token y límite de subida controlados por el test."""
    monkeypatch.setenv("RZ_API_TOKEN", TOKEN)
    monkeypatch.setenv("RZ_MAX_UPLOAD_MIB", "100")
    with TestClient(app, raise_server_exceptions=False) as session:
        yield session


@pytest.fixture
def auth():
    """Cabecera Authorization válida."""
    return {"Authorization": f"Bearer {TOKEN}"}
