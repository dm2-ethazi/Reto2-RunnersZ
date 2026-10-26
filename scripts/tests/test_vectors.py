import hashlib
import json
import struct
import zlib
from pathlib import Path

VECTORS = Path(__file__).resolve().parents[2] / "android/app/src/test/resources/vectors"


def test_png_vector_has_valid_chunk_checksums():
    image = (VECTORS / "image.bin").read_bytes()
    assert image[:8] == b"\x89PNG\r\n\x1a\n"
    offset = 8
    while offset < len(image):
        length = struct.unpack_from(">I", image, offset)[0]
        expected = struct.unpack_from(">I", image, offset + 8 + length)[0]
        assert (
            zlib.crc32(image[offset + 4 : offset + 8 + length]) & 0xFFFFFFFF == expected
        )
        offset += length + 12
    assert offset == len(image)


def test_manifest_matches_plaintext_vectors():
    manifest = json.loads((VECTORS / "manifest.json").read_text(encoding="utf-8"))
    for name, expected in manifest["vectors"].items():
        content = (VECTORS / f"{name}.bin").read_bytes()
        assert len(content) == expected["size"]
        assert hashlib.sha256(content).hexdigest() == expected["sha256"]
