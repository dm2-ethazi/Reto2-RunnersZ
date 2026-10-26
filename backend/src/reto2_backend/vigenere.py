from .errors import InvalidKeyError, InvalidModeError

ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"


def validate_key(key: str) -> str:
    if not key:
        raise InvalidKeyError()
    normalized = key.upper()
    if any(char not in ALPHABET for char in normalized):
        raise InvalidKeyError()
    return normalized


def _shift_char(char: str, shift: int) -> str:
    if "A" <= char <= "Z":
        return chr((ord(char) - ord("A") + shift) % 26 + ord("A"))
    if "a" <= char <= "z":
        return chr((ord(char) - ord("a") + shift) % 26 + ord("a"))
    return char


def cifrar(texto: str, clave: str) -> str:
    normalized = validate_key(clave)
    result: list[str] = []
    key_index = 0
    for char in texto:
        if "A" <= char <= "Z" or "a" <= char <= "z":
            shift = ord(normalized[key_index % len(normalized)]) - ord("A")
            result.append(_shift_char(char, shift))
            key_index += 1
        else:
            result.append(char)
    return "".join(result)


def descifrar(texto: str, clave: str) -> str:
    normalized = validate_key(clave)
    result: list[str] = []
    key_index = 0
    for char in texto:
        if "A" <= char <= "Z" or "a" <= char <= "z":
            shift = ord(normalized[key_index % len(normalized)]) - ord("A")
            result.append(_shift_char(char, -shift))
            key_index += 1
        else:
            result.append(char)
    return "".join(result)


def transformar(texto: str, clave: str, modo: str) -> str:
    if modo == "cifrar":
        return cifrar(texto, clave)
    if modo == "descifrar":
        return descifrar(texto, clave)
    raise InvalidModeError()
