from dataclasses import dataclass
from struct import Struct

from .errors import FormatError

MAGIC = b"HGAE"
VERSION = 1
SALT_SIZE = 16
NONCE_PREFIX_SIZE = 8
DEFAULT_ITERATIONS = 600_000
MIN_ITERATIONS = 100_000
MAX_ITERATIONS = 2_000_000
DEFAULT_BLOCK_SIZE = 1024 * 1024
MIN_BLOCK_SIZE = 64 * 1024
MAX_BLOCK_SIZE = 16 * 1024 * 1024
TAG_SIZE = 16
DIGEST_SIZE = 32
HEADER_STRUCT = Struct(">4sB16sI8sI")
HEADER_SIZE = HEADER_STRUCT.size


@dataclass(frozen=True)
class Header:
    salt: bytes
    iterations: int
    nonce_prefix: bytes
    block_size: int

    def to_bytes(self) -> bytes:
        return HEADER_STRUCT.pack(
            MAGIC,
            VERSION,
            self.salt,
            self.iterations,
            self.nonce_prefix,
            self.block_size,
        )

    @classmethod
    def parse(cls, raw: bytes) -> "Header":
        if len(raw) != HEADER_SIZE:
            raise FormatError("La cabecera .enc está incompleta.")
        magic, version, salt, iterations, nonce_prefix, block_size = HEADER_STRUCT.unpack(raw)
        if magic != MAGIC:
            raise FormatError("La marca del fichero no es HGAE.")
        if version != VERSION:
            raise FormatError(f"Versión de formato no soportada: {version}.")
        if len(salt) != SALT_SIZE or len(nonce_prefix) != NONCE_PREFIX_SIZE:
            raise FormatError("La cabecera contiene longitudes inválidas.")
        validate_iterations(iterations)
        validate_block_size(block_size)
        return cls(
            salt=salt,
            iterations=iterations,
            nonce_prefix=nonce_prefix,
            block_size=block_size,
        )


def validate_iterations(iterations: int) -> None:
    if not MIN_ITERATIONS <= iterations <= MAX_ITERATIONS:
        raise FormatError(
            f"Iteraciones PBKDF2 fuera de rango: {iterations} "
            f"(se aceptan {MIN_ITERATIONS}-{MAX_ITERATIONS})."
        )


def validate_block_size(block_size: int) -> None:
    if not MIN_BLOCK_SIZE <= block_size <= MAX_BLOCK_SIZE:
        raise FormatError(f"Tamaño de bloque fuera de rango: {block_size}.")


def build_aad(header_bytes: bytes, is_last: bool) -> bytes:
    return header_bytes + (b"\x01" if is_last else b"\x00")
