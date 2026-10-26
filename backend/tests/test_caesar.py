import pytest

from reto2_backend.caesar import transform


@pytest.mark.parametrize("shift", range(1, 26))
def test_round_trip(shift):
    text = "Ab zZ Ñáéíóú 012!"
    assert transform(transform(text, shift), shift, decrypt=True) == text


@pytest.mark.parametrize("shift", [-1, 0, 26, 1.5, "3", True, None])
def test_invalid_shift(shift):
    with pytest.raises(ValueError):
        transform("a", shift)


def test_known_vector():
    assert transform("Ataque Ñá!", 3) == "Dwdtxh Ñá!"
    assert transform("", 1) == ""
