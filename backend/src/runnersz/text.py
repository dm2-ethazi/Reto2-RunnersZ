"""Algoritmos Vigenère y César independientes de HTTP (§6.1 y §6.3).

Solo se transforman letras ASCII A-Z y a-z; espacios, dígitos, signos, la Ñ y
las letras acentuadas se conservan en su posición y no avanzan la clave.
"""

from __future__ import annotations

import re

_KEY_PATTERN = re.compile(r"[A-Za-z]+")


def vigenere(text: str, key: str, decrypt: bool = False) -> str:
    """Aplica Vigenère conservando mayúsculas, espacios y símbolos.

    Args:
        text: Texto original.
        key: Clave formada solo por letras ASCII.
        decrypt: Invierte el desplazamiento.

    Returns:
        Texto transformado.

    Raises:
        ValueError: Clave vacía o con caracteres no válidos (INVALID_KEY).
    """
    if not key or not _KEY_PATTERN.fullmatch(key):
        raise ValueError("INVALID_KEY")
    keyword = key.upper()
    result = []
    position = 0
    for character in text:
        if character.isascii() and character.isalpha():
            base = 65 if character.isupper() else 97
            shift = ord(keyword[position % len(keyword)]) - 65
            if decrypt:
                shift = -shift
            result.append(chr((ord(character) - base + shift) % 26 + base))
            position += 1
        else:
            result.append(character)
    return "".join(result)


def caesar(text: str, shift: int, decrypt: bool = False) -> str:
    """Aplica un desplazamiento entero entre 1 y 25.

    Args:
        text: Texto original.
        shift: Desplazamiento entre 1 y 25.
        decrypt: Invierte el desplazamiento.

    Returns:
        Texto transformado.

    Raises:
        ValueError: Desplazamiento fuera de rango (INVALID_SHIFT).
    """
    if isinstance(shift, bool) or not isinstance(shift, int) or not 1 <= shift <= 25:
        raise ValueError("INVALID_SHIFT")
    offset = -shift if decrypt else shift
    result = []
    for character in text:
        if character.isascii() and character.isalpha():
            base = 65 if character.isupper() else 97
            result.append(chr((ord(character) - base + offset) % 26 + base))
        else:
            result.append(character)
    return "".join(result)
