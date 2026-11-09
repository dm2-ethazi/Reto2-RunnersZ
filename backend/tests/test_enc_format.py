import pytest

from reto2_backend.enc_format import (
    DEFAULT_BLOCK_SIZE,
    DEFAULT_ITERATIONS,
    HEADER_SIZE,
    Header,
    build_aad,
    validate_block_size,
    validate_iterations,
)
from reto2_backend.errors import FormatError


def _header() -> Header:
    return Header(
        salt=b"S" * 16,
        iterations=DEFAULT_ITERATIONS,
        nonce_prefix=b"N" * 8,
        block_size=DEFAULT_BLOCK_SIZE,
    )


def test_cabecera_tiene_37_bytes_y_campos_esperados() -> None:
    raw = _header().to_bytes()
    assert len(raw) == HEADER_SIZE == 37
    assert raw[:4] == b"HGAE"
    assert raw[4] == 1
    assert raw[5:21] == b"S" * 16
    assert int.from_bytes(raw[21:25], "big") == 600_000
    assert raw[25:33] == b"N" * 8
    assert int.from_bytes(raw[33:37], "big") == 1024 * 1024


def test_parse_cabecera_valida() -> None:
    original = _header()
    parsed = Header.parse(original.to_bytes())
    assert parsed == original


def test_aad_distingue_ultimo_bloque() -> None:
    raw = _header().to_bytes()
    assert build_aad(raw, False) == raw + b"\x00"
    assert build_aad(raw, True) == raw + b"\x01"


def test_cabecera_por_defecto() -> None:
    header = Header.parse(_header().to_bytes())
    assert header.iterations == 600_000
    assert header.block_size == 1_048_576


def test_cabecera_incompleta() -> None:
    with pytest.raises(FormatError):
        Header.parse(b"HGAE")


def test_marca_invalida() -> None:
    raw = bytearray(_header().to_bytes())
    raw[:4] = b"NOPE"
    with pytest.raises(FormatError, match="HGAE"):
        Header.parse(bytes(raw))


def test_version_invalida() -> None:
    raw = bytearray(_header().to_bytes())
    raw[4] = 2
    with pytest.raises(FormatError, match="Versión"):
        Header.parse(bytes(raw))


@pytest.mark.parametrize("iterations", [0, 99_999, 2_000_001])
def test_iteraciones_fuera_de_rango(iterations: int) -> None:
    with pytest.raises(FormatError):
        validate_iterations(iterations)


@pytest.mark.parametrize("iterations", [100_000, 600_000, 2_000_000])
def test_iteraciones_en_rango(iterations: int) -> None:
    validate_iterations(iterations)


@pytest.mark.parametrize("block_size", [0, 1, 65_535, 16 * 1024 * 1024 + 1])
def test_bloque_fuera_de_rango(block_size: int) -> None:
    with pytest.raises(FormatError):
        validate_block_size(block_size)


@pytest.mark.parametrize("block_size", [64 * 1024, 1_048_576, 16 * 1024 * 1024])
def test_bloque_en_rango(block_size: int) -> None:
    validate_block_size(block_size)
