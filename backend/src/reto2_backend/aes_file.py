import hashlib
import hmac
import os
from contextlib import suppress
from dataclasses import dataclass
from pathlib import Path

from cryptography.exceptions import InvalidTag
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC

from .enc_format import (
    DEFAULT_BLOCK_SIZE,
    DEFAULT_ITERATIONS,
    DIGEST_SIZE,
    HEADER_SIZE,
    NONCE_PREFIX_SIZE,
    SALT_SIZE,
    TAG_SIZE,
    Header,
    build_aad,
)
from .errors import (
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
    Reto2Error,
    StorageError,
)

CHUNK_OVERHEAD = TAG_SIZE
MAX_COUNTER = 0xFFFFFFFF


@dataclass(frozen=True)
class FileResult:
    ruta_salida: str
    bytes_entrada: int
    bytes_salida: int
    sha256: str
    integridad_verificada: bool | None


def _same_path(first: Path, second: Path) -> bool:
    try:
        if first.exists() and second.exists():
            return os.path.samefile(first, second)
    except OSError:
        pass
    return os.path.normcase(os.path.realpath(first)) == os.path.normcase(os.path.realpath(second))


def _validate_password(password: str) -> bytes:
    if password is None or password == "":
        raise EmptyPasswordError()
    try:
        return password.encode("utf-8")
    except UnicodeEncodeError as exc:
        raise InvalidPasswordError() from exc


def _prepare_paths(input_path: str, output_path: str) -> tuple[Path, Path, Path]:
    source = Path(input_path)
    destination = Path(output_path)

    if not source.exists():
        raise InputNotFoundError(str(source))
    if not source.is_file():
        raise InputNotFileError(str(source))
    if _same_path(source, destination):
        raise OutputSameAsInputError()
    if destination.exists():
        raise OutputExistsError(str(destination))
    if not destination.parent.exists() or not destination.parent.is_dir():
        raise OutputParentError(str(destination.parent))

    part = Path(str(destination) + ".part")
    if part.exists():
        raise OutputExistsError(str(part))
    return source, destination, part


def _publish_no_replace(part: Path, destination: Path) -> None:
    try:
        if os.name == "nt":
            os.rename(part, destination)
        else:
            os.link(part, destination)
            part.unlink()
    except FileExistsError as exc:
        raise OutputExistsError(str(destination)) from exc


def _derive_key(header: Header, password_bytes: bytes) -> AESGCM:
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=header.salt,
        iterations=header.iterations,
    )
    key = kdf.derive(password_bytes)
    return AESGCM(key)


def _nonce(prefix: bytes, counter: int) -> bytes:
    return prefix + counter.to_bytes(4, "big")


def _block_count_and_last_length(payload_size: int, block_size: int) -> tuple[int, int]:
    if payload_size < DIGEST_SIZE + TAG_SIZE:
        raise FormatError("El fichero .enc no contiene ningÃºn bloque completo.")
    quotient, remainder = divmod(payload_size - DIGEST_SIZE - TAG_SIZE, block_size + TAG_SIZE)
    if remainder > block_size:
        raise FormatError("El tamaÃ±o de los bloques cifrados no es coherente.")
    block_count = quotient + 1
    if block_count > MAX_COUNTER:
        raise FormatError("El fichero contiene demasiados bloques.")
    return block_count, remainder


def _cleanup(part: Path, part_created: bool) -> None:
    if part_created:
        with suppress(OSError):
            part.unlink(missing_ok=True)


def cifrar_fichero(ruta_entrada: str, ruta_salida: str, contrasena: str) -> FileResult:
    password_bytes = _validate_password(contrasena)
    source, destination, part = _prepare_paths(ruta_entrada, ruta_salida)
    part_created = False
    completed = False
    written = HEADER_SIZE

    try:
        header = Header(
            salt=os.urandom(SALT_SIZE),
            iterations=DEFAULT_ITERATIONS,
            nonce_prefix=os.urandom(NONCE_PREFIX_SIZE),
            block_size=DEFAULT_BLOCK_SIZE,
        )
        header_bytes = header.to_bytes()
        aesgcm = _derive_key(header, password_bytes)
        hasher = hashlib.sha256()
        digest = b""

        with source.open("rb") as origin, part.open("xb") as target:
            part_created = True
            target.write(header_bytes)
            previous = origin.read(header.block_size)

            if not previous:
                digest = hashlib.sha256(b"").digest()
                ciphertext = aesgcm.encrypt(
                    _nonce(header.nonce_prefix, 0),
                    digest,
                    build_aad(header_bytes, True),
                )
                target.write(ciphertext)
                written += len(ciphertext)
            else:
                counter = 0
                while True:
                    hasher.update(previous)
                    following = origin.read(header.block_size)
                    if not following:
                        digest = hasher.digest()
                        plaintext = previous + digest
                        ciphertext = aesgcm.encrypt(
                            _nonce(header.nonce_prefix, counter),
                            plaintext,
                            build_aad(header_bytes, True),
                        )
                        target.write(ciphertext)
                        written += len(ciphertext)
                        break
                    ciphertext = aesgcm.encrypt(
                        _nonce(header.nonce_prefix, counter),
                        previous,
                        build_aad(header_bytes, False),
                    )
                    target.write(ciphertext)
                    written += len(ciphertext)
                    counter += 1
                    if counter > MAX_COUNTER:
                        raise FormatError("El fichero es demasiado grande para el formato v1.")
                    previous = following
            target.flush()
            os.fsync(target.fileno())

        if destination.exists():
            raise OutputExistsError(str(destination))
        _publish_no_replace(part, destination)
        completed = True
        return FileResult(
            ruta_salida=str(destination),
            bytes_entrada=source.stat().st_size,
            bytes_salida=written,
            sha256=digest.hex(),
            integridad_verificada=None,
        )
    except Reto2Error:
        raise
    except PermissionError as exc:
        raise StorageError(
            "No hay permisos suficientes para leer o escribir el fichero.", 403
        ) from exc
    except OSError as exc:
        raise StorageError(f"Error de almacenamiento: {exc}") from exc
    finally:
        if not completed:
            _cleanup(part, part_created)


def descifrar_fichero(ruta_entrada: str, ruta_salida: str, contrasena: str) -> FileResult:
    password_bytes = _validate_password(contrasena)
    source, destination, part = _prepare_paths(ruta_entrada, ruta_salida)
    part_created = False
    completed = False
    written = 0

    try:
        input_size = source.stat().st_size
        if input_size < HEADER_SIZE + DIGEST_SIZE + TAG_SIZE:
            raise FormatError("El fichero .enc es demasiado pequeÃ±o.")

        with source.open("rb") as origin:
            header_bytes = origin.read(HEADER_SIZE)
            if len(header_bytes) != HEADER_SIZE:
                raise FormatError("La cabecera .enc estÃ¡ incompleta.")
            header = Header.parse(header_bytes)
            payload_size = input_size - HEADER_SIZE
            block_count, last_length = _block_count_and_last_length(payload_size, header.block_size)
            aesgcm = _derive_key(header, password_bytes)
            hasher = hashlib.sha256()
            expected_digest = b""

            with part.open("xb") as target:
                part_created = True
                for counter in range(block_count):
                    is_last = counter == block_count - 1
                    ciphertext_length = (
                        last_length + DIGEST_SIZE + TAG_SIZE
                        if is_last
                        else header.block_size + TAG_SIZE
                    )
                    ciphertext = origin.read(ciphertext_length)
                    if len(ciphertext) != ciphertext_length:
                        raise FormatError("El fichero .enc estÃ¡ truncado.")
                    try:
                        plaintext = aesgcm.decrypt(
                            _nonce(header.nonce_prefix, counter),
                            ciphertext,
                            build_aad(header_bytes, is_last),
                        )
                    except InvalidTag as exc:
                        raise AuthenticationError() from exc

                    if is_last:
                        if len(plaintext) != last_length + DIGEST_SIZE:
                            raise FormatError("El Ãºltimo bloque no contiene la huella esperada.")
                        content = plaintext[:-DIGEST_SIZE]
                        expected_digest = plaintext[-DIGEST_SIZE:]
                    else:
                        if len(plaintext) != header.block_size:
                            raise FormatError("Un bloque intermedio tiene un tamaÃ±o incorrecto.")
                        content = plaintext

                    target.write(content)
                    written += len(content)
                    hasher.update(content)

                if origin.read(1):
                    raise FormatError("El fichero .enc contiene datos sobrantes.")
                target.flush()
                os.fsync(target.fileno())

        if not hmac.compare_digest(expected_digest, hasher.digest()):
            raise IntegrityError()

        if destination.exists():
            raise OutputExistsError(str(destination))
        _publish_no_replace(part, destination)
        completed = True
        return FileResult(
            ruta_salida=str(destination),
            bytes_entrada=input_size,
            bytes_salida=written,
            sha256=hasher.hexdigest(),
            integridad_verificada=True,
        )
    except Reto2Error:
        raise
    except PermissionError as exc:
        raise StorageError(
            "No hay permisos suficientes para leer o escribir el fichero.", 403
        ) from exc
    except OSError as exc:
        raise StorageError(f"Error de almacenamiento: {exc}") from exc
    finally:
        if not completed:
            _cleanup(part, part_created)
