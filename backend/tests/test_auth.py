"""Contrato de estado, autenticación y declaración OpenAPI (§3.3 y §3.4)."""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from runnersz.main import app


def test_health_no_requiere_token(client):
    """El estado es público y no expone detalles internos."""
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "version": "2.0.0"}


@pytest.mark.parametrize(
    ("value", "status"),
    [
        (None, 401),
        ("Bearer incorrecto", 401),
        ("token-de-prueba", 401),
        ("Bearer token-de-prueba", 204),
    ],
)
def test_auth_check(client, value, status):
    """Solo acepta el Bearer exacto; el resto responde 401 UNAUTHORIZED."""
    headers = {} if value is None else {"Authorization": value}
    response = client.get("/api/v1/auth/check", headers=headers)
    assert response.status_code == status
    if status == 401:
        body = response.json()
        assert body["code"] == "UNAUTHORIZED"
        assert body["message"] == "Token no válido. Revisa los ajustes"


def test_auth_sin_token_configurado(monkeypatch):
    """Sin RZ_API_TOKEN el servicio rechaza todo, también con cabecera."""
    monkeypatch.delenv("RZ_API_TOKEN", raising=False)
    with TestClient(app, raise_server_exceptions=False) as session:
        response = session.get(
            "/api/v1/auth/check", headers={"Authorization": "Bearer cualquiera"}
        )
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_swagger_declara_bearer(client):
    """Swagger declara el esquema Bearer en todas las rutas protegidas."""
    schema = client.get("/openapi.json").json()
    assert schema["components"]["securitySchemes"]["HTTPBearer"] == {
        "type": "http",
        "scheme": "bearer",
    }
    for path, operations in schema["paths"].items():
        if path == "/api/v1/health":
            continue
        for operation in operations.values():
            assert {"HTTPBearer": []} in operation["security"]
