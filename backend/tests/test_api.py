from pathlib import Path

from fastapi.testclient import TestClient

from reto2_backend.api import app

client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {
        "estado": "ok",
        "servicio": "reto2-backend",
        "version": "1.0.0",
    }


def test_vigenere_cifrar() -> None:
    response = client.post(
        "/api/vigenere",
        json={"texto": "Hola Mundo, ¿qué tal?", "clave": "LUNA", "modo": "cifrar"},
    )
    assert response.status_code == 200
    assert response.json() == {"resultado": "Siya Xoadz, ¿khé tlf?"}


def test_vigenere_descifrar() -> None:
    response = client.post(
        "/api/vigenere",
        json={"texto": "Siya Xoadz, ¿khé tlf?", "clave": "LUNA", "modo": "descifrar"},
    )
    assert response.status_code == 200
    assert response.json() == {"resultado": "Hola Mundo, ¿qué tal?"}


def test_vigenere_clave_invalida() -> None:
    response = client.post(
        "/api/vigenere",
        json={"texto": "Hola", "clave": "LU NA", "modo": "cifrar"},
    )
    assert response.status_code == 422
    body = response.json()
    assert body["error"]["codigo"] == "CLAVE_INVALIDA"
    assert "A-Z" in body["error"]["mensaje"]


def test_aes_cifrar_y_descifrar(tmp_path: Path) -> None:
    source = tmp_path / "origen.txt"
    source.write_bytes(b"Hola RunnersZ")
    encrypted = tmp_path / "origen.txt.enc"
    recovered = tmp_path / "origen.recuperado"

    response = client.post(
        "/api/aes/cifrar",
        json={
            "ruta_entrada": str(source),
            "ruta_salida": str(encrypted),
            "contrasena": "clave",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["integridad_verificada"] is None
    assert body["bytes_salida"] == encrypted.stat().st_size

    response = client.post(
        "/api/aes/descifrar",
        json={
            "ruta_entrada": str(encrypted),
            "ruta_salida": str(recovered),
            "contrasena": "clave",
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["integridad_verificada"] is True
    assert recovered.read_bytes() == source.read_bytes()


def test_aes_contrasena_vacia(tmp_path: Path) -> None:
    source = tmp_path / "origen.txt"
    source.write_bytes(b"datos")
    response = client.post(
        "/api/aes/cifrar",
        json={
            "ruta_entrada": str(source),
            "ruta_salida": str(tmp_path / "salida.enc"),
            "contrasena": "",
        },
    )
    assert response.status_code == 422
    assert response.json()["error"]["codigo"] == "CONTRASENA_VACIA"


def test_aes_salida_existente(tmp_path: Path) -> None:
    source = tmp_path / "origen.txt"
    source.write_bytes(b"datos")
    output = tmp_path / "salida.enc"
    output.write_bytes(b"intacto")
    response = client.post(
        "/api/aes/cifrar",
        json={
            "ruta_entrada": str(source),
            "ruta_salida": str(output),
            "contrasena": "clave",
        },
    )
    assert response.status_code == 409
    assert response.json()["error"]["codigo"] == "SALIDA_EXISTE"
    assert output.read_bytes() == b"intacto"


def test_peticion_incompleta() -> None:
    response = client.post("/api/vigenere", json={"texto": "Hola"})
    assert response.status_code == 422
