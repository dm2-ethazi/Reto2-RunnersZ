"""Servicio AES-256-GCM: formato RZEN, tamaños y detección de daños."""

from __future__ import annotations

import struct

import pytest

from runnersz.crypto import (
    BLOCK_SIZE,
    CryptoError,
    FormatError,
    IntegrityError,
    decrypt_file,
    encrypt_file,
)

HEADER_SIZE = 34
FULL_BLOCK = BLOCK_SIZE + 16
PAYLOAD = "secreto"


def _roundtrip(tmp_path, data: bytes) -> bytes:
    """Cifra y descifra un contenido y devuelve lo recuperado."""
    source = tmp_path / "original.bin"
    source.write_bytes(data)
    encrypted = tmp_path / "original.bin.enc"
    recovered = tmp_path / "original.bin.out"
    encrypt_file(source, encrypted, PAYLOAD)
    decrypt_file(encrypted, recovered, PAYLOAD)
    return recovered.read_bytes()


def test_cabecera_del_formato(tmp_path):
    """La cabecera declara magic, versión, KDF, iteraciones y bloque."""
    source = tmp_path / "a"
    source.write_bytes(b"x")
    encrypted = tmp_path / "a.enc"
    encrypt_file(source, encrypted, PAYLOAD)
    header = encrypted.read_bytes()[:HEADER_SIZE]
    assert header[:6] == b"RZEN\x01\x01"
    assert struct.unpack(">I", header[6:10])[0] == 600_000
    assert struct.unpack(">I", header[30:34])[0] == BLOCK_SIZE


@pytest.mark.parametrize(
    "size",
    [1, BLOCK_SIZE, BLOCK_SIZE + 3, 2 * BLOCK_SIZE, 2 * BLOCK_SIZE + 7],
)
def test_recuperacion_bytes_exactos(tmp_path, size):
    """Se recuperan exactamente los bytes, incluidos múltiplos de 1 MiB."""
    data = bytes(range(256)) * (size // 256) + b"x" * (size % 256)
    assert _roundtrip(tmp_path, data) == data


def test_recuperacion_veinte_mebibytes(tmp_path):
    """El tamaño de aceptación del pliego (20 MiB) se recupera íntegro."""
    data = b"z" * (20 * 1024 * 1024)
    assert _roundtrip(tmp_path, data) == data


def test_salidas_distintas_con_la_misma_entrada(tmp_path):
    """Cada cifrado usa sal y prefijo nuevos, así que difiere el fichero."""
    source = tmp_path / "a"
    source.write_bytes(b"contenido")
    first, second = tmp_path / "1.enc", tmp_path / "2.enc"
    encrypt_file(source, first, PAYLOAD)
    encrypt_file(source, second, PAYLOAD)
    assert first.read_bytes() != second.read_bytes()


def test_fichero_vacio(tmp_path):
    """0 B se rechaza de forma controlada."""
    source = tmp_path / "vacio"
    source.write_bytes(b"")
    with pytest.raises(CryptoError, match="EMPTY_FILE"):
        encrypt_file(source, tmp_path / "vacio.enc", PAYLOAD)


def test_password_vacia(tmp_path):
    """No se deriva clave con contraseña vacía."""
    source = tmp_path / "a"
    source.write_bytes(b"x")
    with pytest.raises(CryptoError, match="EMPTY_PASSWORD"):
        encrypt_file(source, tmp_path / "a.enc", "")


def test_destino_ocupado(tmp_path):
    """No se sobrescribe un fichero existente ni el original."""
    source = tmp_path / "a"
    source.write_bytes(b"x")
    destination = tmp_path / "a.enc"
    destination.write_bytes(b"ocupado")
    with pytest.raises(CryptoError, match="OUTPUT_EXISTS"):
        encrypt_file(source, destination, PAYLOAD)
    with pytest.raises(CryptoError, match="OUTPUT_EXISTS"):
        decrypt_file(source, source, PAYLOAD)


@pytest.mark.parametrize(
    ("offset", "value", "message"),
    [
        (0, b"XXXX", "INVALID_FORMAT"),
        (4, b"\x02", "INVALID_FORMAT"),
        (5, b"\x02", "INVALID_FORMAT"),
        (30, struct.pack(">I", 2048), "INVALID_BLOCK_SIZE"),
        (6, struct.pack(">I", 3_000_000), "INVALID_ITERATIONS"),
    ],
)
def test_cabecera_invalida(tmp_path, offset, value, message):
    """Magic, versión, KDF, bloque e iteraciones se validan al leer."""
    source = tmp_path / "a"
    source.write_bytes(b"contenido")
    encrypted = tmp_path / "a.enc"
    encrypt_file(source, encrypted, PAYLOAD)
    data = bytearray(encrypted.read_bytes())
    data[offset : offset + len(value)] = value
    encrypted.write_bytes(bytes(data))
    with pytest.raises(FormatError, match=message):
        decrypt_file(encrypted, tmp_path / "out", PAYLOAD)


@pytest.mark.parametrize("cut", [20, HEADER_SIZE, HEADER_SIZE + 10])
def test_fichero_truncado(tmp_path, cut):
    """Un fichero cortado no se entrega como válido."""
    source = tmp_path / "a"
    source.write_bytes(b"contenido" * 10)
    encrypted = tmp_path / "a.enc"
    encrypt_file(source, encrypted, PAYLOAD)
    encrypted.write_bytes(encrypted.read_bytes()[:cut])
    with pytest.raises(CryptoError):
        decrypt_file(encrypted, tmp_path / "out", PAYLOAD)


def test_byte_alterado(tmp_path):
    """Alterar el contenido rompe la etiqueta GCM."""
    source = tmp_path / "a"
    source.write_bytes(b"contenido" * 100)
    encrypted = tmp_path / "a.enc"
    encrypt_file(source, encrypted, PAYLOAD)
    data = bytearray(encrypted.read_bytes())
    data[HEADER_SIZE + 5] ^= 0xFF
    encrypted.write_bytes(bytes(data))
    with pytest.raises(IntegrityError, match="DECRYPT_FAILED"):
        decrypt_file(encrypted, tmp_path / "out", PAYLOAD)


def test_bloques_reordenados(tmp_path):
    """Reordenar bloques rompe el nonce y el AAD con el índice de bloque."""
    source = tmp_path / "a"
    source.write_bytes(b"a" * BLOCK_SIZE + b"b" * BLOCK_SIZE + b"c" * 10)
    encrypted = tmp_path / "a.enc"
    encrypt_file(source, encrypted, PAYLOAD)
    data = bytearray(encrypted.read_bytes())
    first = bytes(data[HEADER_SIZE : HEADER_SIZE + FULL_BLOCK])
    second = bytes(data[HEADER_SIZE + FULL_BLOCK : HEADER_SIZE + 2 * FULL_BLOCK])
    data[HEADER_SIZE : HEADER_SIZE + FULL_BLOCK] = second
    data[HEADER_SIZE + FULL_BLOCK : HEADER_SIZE + 2 * FULL_BLOCK] = first
    encrypted.write_bytes(bytes(data))
    with pytest.raises(IntegrityError, match="DECRYPT_FAILED"):
        decrypt_file(encrypted, tmp_path / "out", PAYLOAD)


def test_password_incorrecta(tmp_path):
    """Una contraseña distinta no autentica ningún bloque."""
    source = tmp_path / "a"
    source.write_bytes(b"contenido")
    encrypted = tmp_path / "a.enc"
    encrypt_file(source, encrypted, PAYLOAD)
    with pytest.raises(IntegrityError, match="DECRYPT_FAILED"):
        decrypt_file(encrypted, tmp_path / "out", "otra")


def test_no_quedan_temporales(tmp_path):
    """Tras un error no queda ningún .part en el directorio."""
    source = tmp_path / "a"
    source.write_bytes(b"contenido")
    encrypted = tmp_path / "a.enc"
    encrypt_file(source, encrypted, PAYLOAD)
    with pytest.raises(IntegrityError):
        decrypt_file(encrypted, tmp_path / "out", "otra")
    assert sorted(p.name for p in tmp_path.iterdir()) == ["a", "a.enc"]
