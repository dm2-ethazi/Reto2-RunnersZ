from runnersz.crypto import *
import pytest
def test_roundtrip(tmp_path):
 p=tmp_path/"a"; e=tmp_path/"a.enc"; o=tmp_path/"o"; p.write_bytes(b"x"*100); encrypt_file(p,e,"pw"); decrypt_file(e,o,"pw"); assert o.read_bytes()==p.read_bytes()
def test_empty(tmp_path):
 p=tmp_path/"e"; e=tmp_path/"e.enc"; p.write_bytes(b"")
 with pytest.raises(CryptoError,match="EMPTY_FILE"): encrypt_file(p,e,"pw")
def test_blocks(tmp_path):
 p=tmp_path/"a"; e=tmp_path/"e"; o=tmp_path/"o"; p.write_bytes(b"x"*(BLOCK_SIZE+3)); encrypt_file(p,e,"pw"); decrypt_file(e,o,"pw"); assert o.read_bytes()==p.read_bytes()
