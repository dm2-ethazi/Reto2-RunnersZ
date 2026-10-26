import pytest

from reto2_backend.errors import InvalidKeyError, InvalidModeError
from reto2_backend.vigenere import cifrar, descifrar, transformar


def test_vector_del_contrato() -> None:
    assert cifrar("Hola Mundo, ¿qué tal?", "LUNA") == "Siya Xoadz, ¿khé tlf?"


def test_descifra_el_vector_del_contrato() -> None:
    assert descifrar("Siya Xoadz, ¿khé tlf?", "LUNA") == "Hola Mundo, ¿qué tal?"


def test_conserva_mayusculas_y_minusculas() -> None:
    assert cifrar("aBcDeF", "B") == "bCdEfG"


def test_caracteres_ajenos_no_avanzan_la_clave() -> None:
    assert cifrar("A-A", "BC") == "B-C"
    assert cifrar("ÁÉÍÓÚÑñü 123 !?.,", "LUNA") == "ÁÉÍÓÚÑñü 123 !?.,"


def test_ida_y_vuelta_con_texto_mixto() -> None:
    texto = "Orden: atacar al amanecer. ¡A las 05:30! Ñandú y pingüino."
    cifrado = cifrar(texto, "runnersz")
    assert cifrado != texto
    assert descifrar(cifrado, "RUNNERSZ") == texto


def test_clave_minuscula_se_normaliza() -> None:
    assert cifrar("Hola", "luna") == cifrar("Hola", "LUNA")


def test_texto_vacio() -> None:
    assert cifrar("", "LUNA") == ""
    assert descifrar("", "LUNA") == ""


@pytest.mark.parametrize("clave", ["", "LU NA", "LUNA1", "ÑUNA", "LUNÁ", "LU-NA"])
def test_clave_invalida(clave: str) -> None:
    with pytest.raises(InvalidKeyError):
        cifrar("Hola", clave)
    with pytest.raises(InvalidKeyError):
        descifrar("Hola", clave)


def test_modo_invalido() -> None:
    with pytest.raises(InvalidModeError):
        transformar("Hola", "LUNA", "rotar")


def test_transformar_cifrar_y_descifrar() -> None:
    assert transformar("Hola", "LUNA", "cifrar") == "Siya"
    assert transformar("Siya", "LUNA", "descifrar") == "Hola"
