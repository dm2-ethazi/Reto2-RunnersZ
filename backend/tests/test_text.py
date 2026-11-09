"""Contrato de los endpoints textuales (§6.1, §6.3 y §3.3)."""

from __future__ import annotations

import pytest


def test_vigenere_ejemplo_del_contrato(client, auth):
    """El ejemplo «Hola Mundo, ¿qué tal?» con LUNA coincide con el contrato."""
    response = client.post(
        "/api/v1/text/vigenere",
        headers=auth,
        json={"text": "Hola Mundo, ¿qué tal?", "key": "LUNA", "mode": "encrypt"},
    )
    assert response.status_code == 200
    assert response.json() == {"result": "Siya Xoadz, ¿khé tlf?"}


def test_vigenere_ida_y_vuelta(client, auth):
    """Descifrar devuelve el texto original, tildes y Ñ incluidas."""
    original = "Ataque Ñandú, señales 42!"
    encrypted = client.post(
        "/api/v1/text/vigenere",
        headers=auth,
        json={"text": original, "key": "Clave"},
    ).json()["result"]
    response = client.post(
        "/api/v1/text/vigenere",
        headers=auth,
        json={"text": encrypted, "key": "Clave", "mode": "decrypt"},
    )
    assert response.json() == {"result": original}


@pytest.mark.parametrize("payload", [{"text": "x"}, {"text": "x", "key": "A1"}])
def test_vigenere_clave_invalida(client, auth, payload):
    """Clave vacía o con caracteres no válidos devuelve 400 INVALID_KEY."""
    response = client.post("/api/v1/text/vigenere", headers=auth, json=payload)
    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_KEY"


def test_caesar_ejemplo_del_contrato(client, auth):
    """«Ataque al amanecer» con desplazamiento 3 coincide con el contrato."""
    response = client.post(
        "/api/v1/text/caesar",
        headers=auth,
        json={"text": "Ataque al amanecer", "shift": 3},
    )
    assert response.json() == {"result": "Dwdtxh do dpdqhfhu"}


def test_caesar_descifra_y_acepta_texto_numerico(client, auth):
    """El desplazamiento puede llegar como texto y se descifra igual."""
    response = client.post(
        "/api/v1/text/caesar",
        headers=auth,
        json={"text": "Dwdtxh do dpdqhfhu", "shift": "3", "mode": "decrypt"},
    )
    assert response.json() == {"result": "Ataque al amanecer"}


@pytest.mark.parametrize("shift", [0, 26, -3, "abc", True, 1.5, None])
def test_caesar_desplazamiento_invalido(client, auth, shift):
    """0, 26, negativos y valores no enteros devuelven 400 INVALID_SHIFT."""
    response = client.post(
        "/api/v1/text/caesar", headers=auth, json={"text": "x", "shift": shift}
    )
    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_SHIFT"


@pytest.mark.parametrize("path", ["/api/v1/text/vigenere", "/api/v1/text/caesar"])
def test_texto_sin_token(client, path):
    """Sin cabecera no se procesa ningún texto."""
    response = client.post(path, json={"text": "x", "key": "A", "shift": 1})
    assert response.status_code == 401
    assert response.json()["code"] == "UNAUTHORIZED"


def test_texto_ausente(client, auth):
    """Un cuerpo incompleto devuelve 400 con forma {code, message}."""
    response = client.post("/api/v1/text/caesar", headers=auth, json={"shift": 1})
    assert response.status_code == 400
    body = response.json()
    assert body["code"] == "INVALID_REQUEST"
    assert set(body) == {"code", "message"}


def test_modo_invalido(client, auth):
    """mode solo admite encrypt o decrypt."""
    response = client.post(
        "/api/v1/text/caesar",
        headers=auth,
        json={"text": "x", "shift": 1, "mode": "rotar"},
    )
    assert response.status_code == 400
    assert response.json()["code"] == "INVALID_REQUEST"
