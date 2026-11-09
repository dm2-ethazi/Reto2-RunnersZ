"""Cifrado AES-256-GCM por bloques del formato RZEN (§5.3 y §6.2).

Autor: Alain. Versión: 2.0.

El formato .enc es único y solo lo genera este módulo: cabecera RZEN de 34 B,
bloques de 1 MiB de texto cifrado más su etiqueta GCM, y la huella SHA-256 del
original dentro del último bloque en claro.
"""

from __future__ import annotations

import hashlib
import math
import secrets
import struct
from pathlib import Path

from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

MAGIC = b"RZEN"
VERSION = b"\x01"
KDF_PBKDF2_SHA256 = b"\x01"
BLOCK_SIZE = 1_048_576
TAG_SIZE = 16
DIGEST_SIZE = 32
SALT_SIZE = 16
NONCE_PREFIX_SIZE = 4
NONCE_SIZE = 12
COUNTER_SIZE = 8
ITERATIONS = 600_000
MAX_ITERATIONS = 2_000_000

# Cabecera: magic(4) + versión(1) + KDF(1) + iteraciones(4) + sal(16)
# + prefijo de nonce(4) + tamaño de bloque(4).
SALT_OFFSET = 10
PREFIX_OFFSET = SALT_OFFSET + SALT_SIZE
BLOCK_SIZE_OFFSET = PREFIX_OFFSET + NONCE_PREFIX_SIZE
HEADER_SIZE = BLOCK_SIZE_OFFSET + 4

# Un bloque completo de cifrado: 1 MiB en claro más la etiqueta de 16 B.
FULL_CIPHER_BLOCK = BLOCK_SIZE + TAG_SIZE


class CryptoError(ValueError):
    """Error controlado del servicio criptográfico."""


class FormatError(CryptoError):
    """Cabecera o estructura de fichero inválida."""


class IntegrityError(CryptoError):
    """Fallo de autenticación o de huella."""


def _derive_key(password: str, salt: bytes, iterations: int) -> bytes:
    """Deriva la clave AES de 32 B con PBKDF2-HMAC-SHA256 (§6.2).

    Args:
        password: Contraseña UTF-8 del usuario.
        salt: Sal de 16 B leída de la cabecera o generada al cifrar.
        iterations: Iteraciones de PBKDF2 que declara el fichero.

    Returns:
        Clave de 32 B.

    Raises:
        CryptoError: Contraseña vacía.
        FormatError: Iteraciones fuera del rango admitido.
    """
    if not password:
        raise CryptoError("EMPTY_PASSWORD")
    if not 1 <= iterations <= MAX_ITERATIONS:
        raise FormatError("INVALID_ITERATIONS")
    kdf = PBKDF2HMAC(hashes.SHA256(), 32, salt, iterations)
    return kdf.derive(password.encode("utf-8"))


def _header(salt: bytes, prefix: bytes) -> bytes:
    """Serializa la cabecera RZEN de 34 B."""
    return (
        MAGIC
        + VERSION
        + KDF_PBKDF2_SHA256
        + struct.pack(">I", ITERATIONS)
        + salt
        + prefix
        + struct.pack(">I", BLOCK_SIZE)
    )


def _nonce(prefix: bytes, index: int) -> bytes:
    """Nonce de 96 bits: prefijo aleatorio de 4 B y contador de bloque de 8 B."""
    return prefix + struct.pack(">Q", index)


def _aad(header: bytes, index: int, last: bool) -> bytes:
    """Datos asociados: cabecera completa, índice de bloque y último bloque."""
    return header + struct.pack(">Q", index) + (b"\x01" if last else b"\x00")


def _part_path(destination: Path) -> Path:
    """Ruta temporal hermana usada para el renombrado atómico."""
    return destination.with_name(destination.name + ".part")


def _check_destination(source: Path, destination: Path) -> None:
    """Impide sobrescribir la entrada o un fichero ya existente."""
    if destination.exists() or source.resolve() == destination.resolve():
        raise CryptoError("OUTPUT_EXISTS")


def encrypt_file(source_path, destination_path, password: str) -> None:
    """Cifra un fichero completo escribiendo a un temporal y renombrando.

    Args:
        source_path: Fichero original, que no se modifica.
        destination_path: Ruta del .enc que se generará.
        password: Contraseña UTF-8 usada para derivar la clave.

    Raises:
        CryptoError: Entrada vacía, contraseña vacía o destino ocupado.
    """
    source, destination = Path(source_path), Path(destination_path)
    size = source.stat().st_size
    if size == 0:
        raise CryptoError("EMPTY_FILE")
    _check_destination(source, destination)

    salt = secrets.token_bytes(SALT_SIZE)
    prefix = secrets.token_bytes(NONCE_PREFIX_SIZE)
    header = _header(salt, prefix)
    key = _derive_key(password, salt, ITERATIONS)
    part = _part_path(destination)
    digest = hashlib.sha256()
    processed = 0
    index = 0
    try:
        with source.open("rb") as stream, part.open("wb") as target:
            target.write(header)
            while True:
                data = stream.read(BLOCK_SIZE)
                if not data:  # defensa: no debería ocurrir con tamaño > 0
                    break
                digest.update(data)
                processed += len(data)
                # El último bloque se reconoce por tamaño, no por lectura corta:
                # así un fichero múltiplo exacto de 1 MiB también cierra bien.
                last = processed >= size
                if last:
                    data += digest.digest()
                encryptor = Cipher(
                    algorithms.AES(key), modes.GCM(_nonce(prefix, index))
                ).encryptor()
                encryptor.authenticate_additional_data(_aad(header, index, last))
                target.write(encryptor.update(data) + encryptor.finalize())
                target.write(encryptor.tag)
                if last:
                    break
                index += 1
        part.replace(destination)
    except BaseException:
        part.unlink(missing_ok=True)
        raise


def decrypt_file(source_path, destination_path, password: str) -> None:
    """Descifra un .enc validando cada bloque y la huella antes de publicarlo.

    Args:
        source_path: Fichero .enc de entrada, que no se modifica.
        destination_path: Ruta del contenido recuperado.
        password: Contraseña UTF-8 usada para derivar la clave.

    Raises:
        CryptoError: Destino ocupado, contraseña vacía, formato inválido,
            fichero truncado o integridad comprometida.
    """
    source, destination = Path(source_path), Path(destination_path)
    _check_destination(source, destination)
    part = _part_path(destination)
    try:
        with source.open("rb") as stream, part.open("wb") as target:
            header = stream.read(HEADER_SIZE)
            if len(header) != HEADER_SIZE:
                raise FormatError("TRUNCATED")
            if (
                header[:4] != MAGIC
                or header[4:5] != VERSION
                or header[5:6] != KDF_PBKDF2_SHA256
            ):
                raise FormatError("INVALID_FORMAT")
            iterations = struct.unpack(">I", header[6:10])[0]
            block_size = struct.unpack(">I", header[BLOCK_SIZE_OFFSET:])[0]
            if block_size != BLOCK_SIZE:
                raise FormatError("INVALID_BLOCK_SIZE")
            key = _derive_key(password, header[SALT_OFFSET:PREFIX_OFFSET], iterations)
            prefix = header[PREFIX_OFFSET:BLOCK_SIZE_OFFSET]

            payload = source.stat().st_size - HEADER_SIZE
            if payload <= 0:
                raise FormatError("TRUNCATED")
            # Con el tamaño total se deduce cuántos bloques hay y cuánto medía
            # el original: C = 34 + 16*N + (tamaño + 32). Sin esa cuenta, un
            # último bloque mayor que 1 MiB + 16 B se partiría por la mitad.
            blocks = math.ceil((payload - DIGEST_SIZE) / FULL_CIPHER_BLOCK)
            size = payload - DIGEST_SIZE - TAG_SIZE * blocks
            if blocks < 1 or not 1 <= size <= blocks * BLOCK_SIZE:
                raise FormatError("TRUNCATED")
            digest = hashlib.sha256()
            remaining = payload
            for index in range(blocks):
                last = index == blocks - 1
                length = remaining if last else FULL_CIPHER_BLOCK
                blob = stream.read(length)
                if len(blob) != length:
                    raise FormatError("TRUNCATED")
                decryptor = Cipher(
                    algorithms.AES(key),
                    modes.GCM(_nonce(prefix, index), blob[-TAG_SIZE:]),
                ).decryptor()
                decryptor.authenticate_additional_data(_aad(header, index, last))
                try:
                    plain = decryptor.update(blob[:-TAG_SIZE]) + decryptor.finalize()
                except Exception as error:  # etiqueta GCM inválida
                    raise IntegrityError("DECRYPT_FAILED") from error
                if last:
                    content = plain[:-DIGEST_SIZE]
                    expected = plain[-DIGEST_SIZE:]
                    digest.update(content)
                    if not secrets.compare_digest(digest.digest(), expected):
                        raise IntegrityError("DECRYPT_FAILED")
                    target.write(content)
                else:
                    digest.update(plain)
                    target.write(plain)
                remaining -= length
        part.replace(destination)
    except BaseException:
        part.unlink(missing_ok=True)
        raise
