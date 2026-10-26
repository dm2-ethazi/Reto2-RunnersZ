import base64
import hashlib
import json
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "backend" / "src"))

PASSWORD = "RunnersZ-ñ-🔐"
OUTPUT = ROOT / "android/app/src/test/resources/vectors"


def main():
    from reto2_backend.aes_file import _derive_key, _nonce, cifrar_fichero
    from reto2_backend.enc_format import HEADER_SIZE, Header, build_aad

    OUTPUT.mkdir(parents=True, exist_ok=True)
    vectors = {
        "empty": b"",
        "one": b"Z",
        "boundary": bytes(range(256)) * 4096,
        "multi": bytes(range(256)) * 4096 + b"!",
        "image": base64.b64decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg=="
        ),
    }
    manifest = {"format": "HGAE v1", "password": PASSWORD, "vectors": {}}
    with tempfile.TemporaryDirectory() as directory:
        for name, content in vectors.items():
            source = Path(directory) / f"{name}.bin"
            encrypted = Path(directory) / f"{name}.enc"
            source.write_bytes(content)
            cifrar_fichero(str(source), str(encrypted), PASSWORD)
            (OUTPUT / source.name).write_bytes(content)
            (OUTPUT / encrypted.name).write_bytes(encrypted.read_bytes())
            manifest["vectors"][name] = {
                "size": len(content),
                "sha256": hashlib.sha256(content).hexdigest(),
            }
    encrypted = (OUTPUT / "one.enc").read_bytes()
    raw_header = encrypted[:HEADER_SIZE]
    header = Header.parse(raw_header)
    aes = _derive_key(header, PASSWORD.encode("utf-8"))
    authenticated_bad_digest = aes.encrypt(
        _nonce(header.nonce_prefix, 0), b"Z" + bytes(32), build_aad(raw_header, True)
    )
    (OUTPUT / "bad-digest.enc").write_bytes(raw_header + authenticated_bad_digest)
    (OUTPUT / "manifest.json").write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    print(
        f"Generated {len(vectors)} backend interoperability vectors and bad-digest case"
    )


if __name__ == "__main__":
    main()
