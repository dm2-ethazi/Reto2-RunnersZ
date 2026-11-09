"""Contrato de los endpoints de ficheros (§3.3, §4.5 y §7.2)."""

from __future__ import annotations

import pytest

PNG = b"\x89PNG\r\n\x1a\n" + b"datos" * 1000


def test_cifrar_y_descifrar_por_api(client, auth):
    """El .enc se descarga con nombre sugerido y vuelve idéntico byte a byte."""
    encrypted = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("imagen.png", PNG, "image/png")},
        data={"password": "clave-aes"},
    )
    assert encrypted.status_code == 200
    assert encrypted.content.startswith(b"RZEN")
    assert "imagen.png.enc" in encrypted.headers["content-disposition"]
    assert "X-Integrity" not in encrypted.headers
    assert int(encrypted.headers["content-length"]) == len(encrypted.content)

    recovered = client.post(
        "/api/v1/files/decrypt",
        headers=auth,
        files={
            "file": ("imagen.png.enc", encrypted.content, "application/octet-stream")
        },
        data={"password": "clave-aes"},
    )
    assert recovered.status_code == 200
    assert recovered.content == PNG
    assert recovered.headers["X-Integrity"] == "verified"
    assert "imagen.png" in recovered.headers["content-disposition"]


def test_descifrado_sin_extension_enc(client, auth):
    """Si el nombre no acaba en .enc se propone un sufijo .recuperado."""
    encrypted = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("nota", b"hola", "text/plain")},
        data={"password": "clave-aes"},
    )
    recovered = client.post(
        "/api/v1/files/decrypt",
        headers=auth,
        files={"file": ("nota", encrypted.content)},
        data={"password": "clave-aes"},
    )
    assert recovered.status_code == 200
    assert "nota.recuperado" in recovered.headers["content-disposition"]


def test_fichero_vacio(client, auth):
    """0 B devuelve 400 EMPTY_FILE con el mensaje del contrato."""
    response = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("vacio.txt", b"")},
        data={"password": "clave-aes"},
    )
    assert response.status_code == 400
    assert response.json() == {
        "code": "EMPTY_FILE",
        "message": "El archivo está vacío",
    }


def test_password_vacia(client, auth):
    """La contraseña vacía se rechaza antes de procesar nada."""
    response = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("a.txt", b"contenido")},
        data={"password": ""},
    )
    assert response.status_code == 400
    assert response.json()["code"] == "EMPTY_PASSWORD"


def test_fichero_demasiado_grande(client, auth, monkeypatch):
    """Supera el límite configurado y devuelve 413 FILE_TOO_LARGE."""
    monkeypatch.setenv("RZ_MAX_UPLOAD_MIB", "1")
    response = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("grande.bin", b"x" * (2 * 1024 * 1024))},
        data={"password": "clave-aes"},
    )
    assert response.status_code == 413
    assert response.json()["code"] == "FILE_TOO_LARGE"


def test_password_incorrecta(client, auth):
    """Contraseña incorrecta devuelve 422 DECRYPT_FAILED sin entregar nada."""
    encrypted = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("a.txt", b"contenido")},
        data={"password": "clave-aes"},
    )
    response = client.post(
        "/api/v1/files/decrypt",
        headers=auth,
        files={"file": ("a.txt.enc", encrypted.content)},
        data={"password": "otra"},
    )
    assert response.status_code == 422
    assert response.json() == {
        "code": "DECRYPT_FAILED",
        "message": "clave incorrecta o fichero dañado",
    }


def test_fichero_danado(client, auth):
    """Un byte alterado en el .enc también acaba en DECRYPT_FAILED."""
    encrypted = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("a.txt", b"contenido" * 10)},
        data={"password": "clave-aes"},
    )
    data = bytearray(encrypted.content)
    data[40] ^= 0xFF
    response = client.post(
        "/api/v1/files/decrypt",
        headers=auth,
        files={"file": ("a.txt.enc", bytes(data))},
        data={"password": "clave-aes"},
    )
    assert response.status_code == 422
    assert response.json()["code"] == "DECRYPT_FAILED"


def test_sin_password_en_el_formulario(client, auth):
    """Falta el campo password: 400 con forma {code, message}."""
    response = client.post(
        "/api/v1/files/encrypt",
        headers=auth,
        files={"file": ("a.txt", b"contenido")},
    )
    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_REQUEST"


@pytest.mark.parametrize("path", ["/api/v1/files/encrypt", "/api/v1/files/decrypt"])
def test_ficheros_sin_token(client, path):
    """Sin token no se procesa ningún contenido."""
    response = client.post(
        path,
        files={"file": ("a.txt", b"contenido")},
        data={"password": "clave-aes"},
    )
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"
