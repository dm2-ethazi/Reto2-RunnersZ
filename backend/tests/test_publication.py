from pathlib import Path

import pytest

from reto2_backend import aes_file
from reto2_backend.errors import OutputExistsError


@pytest.mark.parametrize("decrypt", [False, True])
def test_concurrent_destination_is_not_overwritten(tmp_path, monkeypatch, decrypt):
    original = tmp_path / "original"
    original.write_bytes(b"Keep original")
    encrypted = tmp_path / "encrypted.enc"
    aes_file.cifrar_fichero(str(original), str(encrypted), "password")
    source = encrypted if decrypt else original
    destination = tmp_path / "result"
    operation = "rename" if aes_file.os.name == "nt" else "link"
    real_operation = getattr(aes_file.os, operation)

    def collide(part, target):
        Path(target).write_bytes(b"Other writer")
        return real_operation(part, target)

    monkeypatch.setattr(aes_file.os, operation, collide)
    function = aes_file.descifrar_fichero if decrypt else aes_file.cifrar_fichero
    with pytest.raises(OutputExistsError):
        function(str(source), str(destination), "password")
    assert destination.read_bytes() == b"Other writer"
    assert original.read_bytes() == b"Keep original"
    assert not Path(str(destination) + ".part").exists()
