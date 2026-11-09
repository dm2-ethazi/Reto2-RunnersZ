def transform(text: str, shift: int, *, decrypt: bool = False) -> str:
    if isinstance(shift, bool) or not isinstance(shift, int) or not 1 <= shift <= 25:
        raise ValueError("El desplazamiento debe ser un entero entre 1 y 25.")
    movement = -shift if decrypt else shift
    result = []
    for character in text:
        if "A" <= character <= "Z":
            base = ord("A")
        elif "a" <= character <= "z":
            base = ord("a")
        else:
            result.append(character)
            continue
        result.append(chr(base + (ord(character) - base + movement) % 26))
    return "".join(result)
