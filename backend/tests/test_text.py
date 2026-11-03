import pytest
from runnersz.text import *
def test_vigenere(): assert vigenere("A ñ-B2! z","BC")=="B ñ-D2! a"
def test_roundtrip(): assert vigenere(vigenere("Ataque Ñ","CLAVE"),"CLAVE",True)=="Ataque Ñ"
@pytest.mark.parametrize("k",["","A1","Á"])
def test_bad_key(k):
 with pytest.raises(ValueError): vigenere("x",k)
def test_caesar(): assert caesar("Dwdtxh do dpdqhfhu",3,True)=="Ataque al amanecer"
