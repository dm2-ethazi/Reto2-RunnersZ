"""Contrato de autenticación y declaración OpenAPI (§3.4)."""
import pytest
from fastapi.testclient import TestClient
from runnersz.main import app
from runnersz import routes

@pytest.fixture
def client(monkeypatch):
    """Usa credenciales ficticias; nunca lee el entorno privado."""
    monkeypatch.setattr(routes, "TOKEN", "fixture-local")
    with TestClient(app) as session:
        yield session

@pytest.mark.parametrize("value,status", [(None,401),("Bearer incorrecto",401),("Bearer fixture-local",204),("fixture-local",401)])
def test_authentication(client,value,status):
    """Acepta solo Bearer válido y conserva el estado contractual."""
    headers = {} if value is None else {"Authorization":value}
    response=client.get("/api/v1/auth/check",headers=headers)
    assert response.status_code == status
    if status == 401:
        assert response.json()["code"] == "UNAUTHORIZED"

def test_swagger_security(client):
    """Swagger declara Bearer en todos los endpoints protegidos."""
    schema=client.get("/openapi.json").json()
    assert schema["components"]["securitySchemes"]["HTTPBearer"] == {"type":"http","scheme":"bearer"}
    for path,methods in schema["paths"].items():
        if path != "/api/v1/health":
            for operation in methods.values():
                assert {"HTTPBearer":[]} in operation["security"]

def test_authorized_text(client):
    """El token válido alcanza el servicio de texto."""
    response=client.post("/api/v1/text/caesar",headers={"Authorization":"Bearer fixture-local"},json={"text":"Ataque","shift":3})
    assert response.status_code == 200
    assert response.json() == {"result":"Dwdtxh"}
