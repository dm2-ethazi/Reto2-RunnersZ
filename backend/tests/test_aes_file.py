import hashlib
from pathlib import Path

import pytest
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

from reto2_backend.aes_file import (
    _block_count_and_last_length,
    _nonce,
    cifrar_fichero,
    descifrar_fichero,
)
from reto2_backend.enc_format import (
    DEFAULT_BLOCK_SIZE,
    DIGEST_SIZE,
    HEADER_SIZE,
    TAG_SIZE,
    Header,
    build_aad,
)
from reto2_backend.errors import (
    AuthenticationError,
    EmptyPasswordError,
    FormatError,
    InputNotFileError,
    InputNotFoundError,
    IntegrityError,
    InvalidPasswordError,
    OutputExistsError,
    OutputParentError,
    OutputSameAsInputError,
)


def _pair(tmp_path: Path, content: bytes) -> tuple[Path, Path, str]:
    source = tmp_path / "origen.bin"
    source.write_bytes(content)
    destination = tmp_path / "salida.bin.enc"
    return source, destination, "clave-segura"


def _roundtrip(tmp_path: Path, content: bytes) -> None:
    source, encrypted, password = _pair(tmp_path, content)
    original_hash = hashlib.sha256(content).hexdigest()
    enc_result = cifrar_fichero(str(source), str(encrypted), password)
    recovered = tmp_path / "recuperado.bin"
    dec_result = descifrar_fichero(str(encrypted), str(recovered), password)
    assert recovered.read_bytes() == content
    assert hashlib.sha256(source.read_bytes()).hexdigest() == original_hash
    assert enc_result.sha256 == original_hash
    assert enc_result.integridad_verificada is None
    assert dec_result.sha256 == original_hash
    assert dec_result.integridad_verificada is True
    assert dec_result.bytes_salida == len(content)
    assert enc_result.bytes_salida == encrypted.stat().st_size
    assert not Path(str(encrypted) + ".part").exists()
    assert not Path(str(recovered) + ".part").exists()


def test_ida_y_vuelta_texto(tmp_path: Path) -> None:
    _roundtrip(tmp_path, "Hola RunnersZ, ñandú y pingüino.\n".encode())


def test_fichero_vacio(tmp_path: Path) -> None:
    _roundtrip(tmp_path, b"")


def test_fichero_de_un_byte(tmp_path: Path) -> None:
    _roundtrip(tmp_path, b"X")


def test_fichero_de_un_mib_exacto(tmp_path: Path) -> None:
    _roundtrip(tmp_path, b"A" * DEFAULT_BLOCK_SIZE)


def test_fichero_de_bloque_y_pico(tmp_path: Path) -> None:
    _roundtrip(tmp_path, b"B" * (DEFAULT_BLOCK_SIZE + 123))


@pytest.mark.slow
def test_fichero_de_20_mib(tmp_path: Path) -> None:
    _roundtrip(tmp_path, b"C" * (20 * 1024 * 1024))


def test_cabecera_generada(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    cifrar_fichero(str(source), str(encrypted), password)
    raw = encrypted.read_bytes()
    header = Header.parse(raw[:HEADER_SIZE])
    assert raw[:4] == b"HGAE"
    assert raw[4] == 1
    assert header.iterations == 600_000
    assert header.block_size == 1_048_576
    assert len(header.salt) == 16
    assert len(header.nonce_prefix) == 8


def test_fichero_cifrado_vacio_tiene_cabecera_y_bloque_final(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"")
    cifrar_fichero(str(source), str(encrypted), password)
    assert encrypted.stat().st_size == HEADER_SIZE + DIGEST_SIZE + TAG_SIZE


def test_original_no_se_modifica(tmp_path: Path) -> None:
    content = b"contenido original"
    source, encrypted, password = _pair(tmp_path, content)
    before = source.stat().st_mtime_ns
    cifrar_fichero(str(source), str(encrypted), password)
    assert source.read_bytes() == content
    assert source.stat().st_mtime_ns == before


def test_salida_existente_no_se_sobrescribe(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    encrypted.write_bytes(b"no tocar")
    with pytest.raises(OutputExistsError):
        cifrar_fichero(str(source), str(encrypted), password)
    assert encrypted.read_bytes() == b"no tocar"


def test_salida_igual_a_entrada(tmp_path: Path) -> None:
    source = tmp_path / "origen.txt"
    source.write_bytes(b"datos")
    with pytest.raises(OutputSameAsInputError):
        cifrar_fichero(str(source), str(source), "clave")
    assert source.read_bytes() == b"datos"


def test_salida_igual_a_entrada_con_ruta_relativa(tmp_path: Path) -> None:
    source = tmp_path / "origen.txt"
    source.write_bytes(b"datos")
    relative = Path(source.name)
    with pytest.raises(OutputSameAsInputError):
        cifrar_fichero(str(source), str(tmp_path / relative), "clave")


def test_entrada_inexistente(tmp_path: Path) -> None:
    with pytest.raises(InputNotFoundError):
        cifrar_fichero(str(tmp_path / "no-existe"), str(tmp_path / "salida.enc"), "clave")


def test_entrada_es_directorio(tmp_path: Path) -> None:
    with pytest.raises(InputNotFileError):
        cifrar_fichero(str(tmp_path), str(tmp_path / "salida.enc"), "clave")


def test_carpeta_de_salida_inexistente(tmp_path: Path) -> None:
    source = tmp_path / "origen.txt"
    source.write_bytes(b"datos")
    with pytest.raises(OutputParentError):
        cifrar_fichero(str(source), str(tmp_path / "no" / "salida.enc"), "clave")


def test_temporal_existente(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    part = Path(str(encrypted) + ".part")
    part.write_bytes(b"resto")
    with pytest.raises(OutputExistsError):
        cifrar_fichero(str(source), str(encrypted), password)
    assert part.read_bytes() == b"resto"


@pytest.mark.parametrize("password", ["", None])
def test_contrasena_vacia(tmp_path: Path, password: str | None) -> None:
    source, encrypted, _ = _pair(tmp_path, b"datos")
    with pytest.raises(EmptyPasswordError):
        cifrar_fichero(str(source), str(encrypted), password)  # type: ignore[arg-type]
    assert not encrypted.exists()


def test_contrasena_utf8_invalida(tmp_path: Path) -> None:
    source, encrypted, _ = _pair(tmp_path, b"datos")
    with pytest.raises(InvalidPasswordError):
        cifrar_fichero(str(source), str(encrypted), "\ud800")


def test_descifrado_con_clave_incorrecta(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos secretos")
    cifrar_fichero(str(source), str(encrypted), password)
    recovered = tmp_path / "recuperado.bin"
    with pytest.raises(AuthenticationError):
        descifrar_fichero(str(encrypted), str(recovered), "otra-clave")
    assert not recovered.exists()
    assert not Path(str(recovered) + ".part").exists()


def test_descifrado_con_contrasena_vacia(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    cifrar_fichero(str(source), str(encrypted), password)
    with pytest.raises(EmptyPasswordError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), "")


def test_fichero_truncado(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    cifrar_fichero(str(source), str(encrypted), password)
    raw = encrypted.read_bytes()
    encrypted.write_bytes(raw[:-10])
    with pytest.raises(FormatError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)


def test_bloque_final_eliminado(tmp_path: Path) -> None:
    content = b"D" * (DEFAULT_BLOCK_SIZE + 10)
    source, encrypted, password = _pair(tmp_path, content)
    cifrar_fichero(str(source), str(encrypted), password)
    raw = encrypted.read_bytes()
    encrypted.write_bytes(raw[: HEADER_SIZE + DEFAULT_BLOCK_SIZE + TAG_SIZE])
    with pytest.raises(AuthenticationError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)


def test_datos_sobrantes(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    cifrar_fichero(str(source), str(encrypted), password)
    encrypted.write_bytes(encrypted.read_bytes() + b"x")
    with pytest.raises((FormatError, AuthenticationError)):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)


def test_cifrado_alterado(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos secretos")
    cifrar_fichero(str(source), str(encrypted), password)
    raw = bytearray(encrypted.read_bytes())
    raw[-1] ^= 0xFF
    encrypted.write_bytes(bytes(raw))
    with pytest.raises(AuthenticationError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)


def test_marca_alterada(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    cifrar_fichero(str(source), str(encrypted), password)
    raw = bytearray(encrypted.read_bytes())
    raw[:4] = b"XXXX"
    encrypted.write_bytes(bytes(raw))
    with pytest.raises(FormatError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)


def test_iteraciones_alteradas(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    cifrar_fichero(str(source), str(encrypted), password)
    raw = bytearray(encrypted.read_bytes())
    raw[21:25] = (0).to_bytes(4, "big")
    encrypted.write_bytes(bytes(raw))
    with pytest.raises(FormatError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)


def test_bloque_alterado_fuera_de_rango(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    cifrar_fichero(str(source), str(encrypted), password)
    raw = bytearray(encrypted.read_bytes())
    raw[33:37] = (1).to_bytes(4, "big")
    encrypted.write_bytes(bytes(raw))
    with pytest.raises(FormatError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)


def test_fichero_enc_demasiado_pequeno(tmp_path: Path) -> None:
    encrypted = tmp_path / "pequeno.enc"
    encrypted.write_bytes(b"HGAE")
    with pytest.raises(FormatError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), "clave")


def test_huella_manipulada_pero_autenticada(tmp_path: Path) -> None:
    content = b"contenido con huella manipulada"
    source, encrypted, password = _pair(tmp_path, content)
    cifrar_fichero(str(source), str(encrypted), password)
    raw = bytearray(encrypted.read_bytes())
    header_bytes = bytes(raw[:HEADER_SIZE])
    header = Header.parse(header_bytes)
    payload_size = len(raw) - HEADER_SIZE
    block_count, last_length = _block_count_and_last_length(payload_size, header.block_size)
    assert block_count == 1
    counter = 0
    offset = HEADER_SIZE
    ciphertext_length = last_length + DIGEST_SIZE + TAG_SIZE
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=header.salt,
        iterations=header.iterations,
    )
    aesgcm = AESGCM(kdf.derive(password.encode()))
    ciphertext = bytes(raw[offset : offset + ciphertext_length])
    plaintext = aesgcm.decrypt(
        _nonce(header.nonce_prefix, counter),
        ciphertext,
        build_aad(header_bytes, True),
    )
    assert plaintext[-DIGEST_SIZE:] == hashlib.sha256(content).digest()
    altered = (
        plaintext[:-DIGEST_SIZE]
        + bytes([plaintext[-DIGEST_SIZE] ^ 1])
        + plaintext[-DIGEST_SIZE + 1 :]
    )
    new_ciphertext = aesgcm.encrypt(
        _nonce(header.nonce_prefix, counter),
        altered,
        build_aad(header_bytes, True),
    )
    raw[offset : offset + ciphertext_length] = new_ciphertext
    encrypted.write_bytes(bytes(raw))
    with pytest.raises(IntegrityError):
        descifrar_fichero(str(encrypted), str(tmp_path / "recuperado.bin"), password)
    assert not (tmp_path / "recuperado.bin").exists()
    assert not Path(str(tmp_path / "recuperado.bin") + ".part").exists()


def test_rutas_con_espacios_y_acentos(tmp_path: Path) -> None:
    source = tmp_path / "origen con espacios ñ á.bin"
    source.write_bytes(b"contenido")
    encrypted = tmp_path / "salida con acentos ó.enc"
    recovered = tmp_path / "recuperado con ñ.recuperado"
    cifrar_fichero(str(source), str(encrypted), "clave")
    descifrar_fichero(str(encrypted), str(recovered), "clave")
    assert recovered.read_bytes() == b"contenido"


def test_salida_es_directorio(tmp_path: Path) -> None:
    source, encrypted, password = _pair(tmp_path, b"datos")
    encrypted.mkdir()
    with pytest.raises(OutputExistsError):
        cifrar_fichero(str(source), str(encrypted), password)
