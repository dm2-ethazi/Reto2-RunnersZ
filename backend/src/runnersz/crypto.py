from pathlib import Path
import hashlib,secrets,struct
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.ciphers import Cipher,algorithms,modes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
MAGIC=b"RZEN"; BLOCK_SIZE=1048576; TAG_SIZE=16; HEADER_SIZE=34; ITERATIONS=600000
class CryptoError(ValueError): pass
class FormatError(CryptoError): pass
class IntegrityError(CryptoError): pass
def key(password,salt,it):
 if not password: raise CryptoError("EMPTY_PASSWORD")
 if not 1<=it<=2000000: raise FormatError("INVALID_ITERATIONS")
 return PBKDF2HMAC(hashes.SHA256(),32,salt,it).derive(password.encode())
def header(salt,prefix): return MAGIC+b"\1\1"+struct.pack(">I",ITERATIONS)+salt+prefix+struct.pack(">I",BLOCK_SIZE)
def encrypt_file(src,dst,password):
 src,dst=Path(src),Path(dst)
 if src.stat().st_size==0: raise CryptoError("EMPTY_FILE")
 if dst.exists() or src.resolve()==dst.resolve(): raise CryptoError("OUTPUT_EXISTS")
 salt=secrets.token_bytes(16); prefix=secrets.token_bytes(4); h=header(salt,prefix); k=key(password,salt,ITERATIONS); part=dst.with_name(dst.name+".part"); digest=hashlib.sha256(); n=0
 try:
  with src.open("rb") as i,part.open("wb") as o:
   o.write(h)
   while True:
    data=i.read(BLOCK_SIZE)
    if not data: break
    digest.update(data); last=len(data)<BLOCK_SIZE
    if last: data+=digest.copy().digest()
    flag=bytes([last]); e=Cipher(algorithms.AES(k),modes.GCM(prefix+struct.pack(">I",n))).encryptor(); e.authenticate_additional_data(h+flag); o.write(flag+e.update(data)+e.finalize()+e.tag); n+=1
  part.replace(dst)
 except Exception: part.unlink(missing_ok=True); raise
def decrypt_file(src,dst,password):
 src,dst=Path(src),Path(dst)
 if dst.exists() or src.resolve()==dst.resolve(): raise CryptoError("OUTPUT_EXISTS")
 part=dst.with_name(dst.name+".part")
 try:
  with src.open("rb") as i:
   h=i.read(HEADER_SIZE)
   if len(h)!=HEADER_SIZE or h[:4]!=MAGIC or h[4:6]!=b"\1\1": raise FormatError("INVALID_FORMAT")
   it=struct.unpack(">I",h[6:10])[0]; bs=struct.unpack(">I",h[30:34])[0]
   if bs!=BLOCK_SIZE: raise FormatError("INVALID_BLOCK_SIZE")
   k=key(password,h[10:26],it); prefix=h[26:30]; digest=hashlib.sha256(); n=0
   with part.open("wb") as o:
    while True:
     flag=i.read(1)
     if not flag: raise FormatError("TRUNCATED")
     if flag[0] not in (0,1): raise FormatError("INVALID_FLAG")
     last=flag==b"\1"; blob=i.read() if last else i.read(BLOCK_SIZE+TAG_SIZE)
     if len(blob)<TAG_SIZE or (not last and len(blob)!=BLOCK_SIZE+TAG_SIZE): raise FormatError("TRUNCATED")
     d=Cipher(algorithms.AES(k),modes.GCM(prefix+struct.pack(">I",n),blob[-TAG_SIZE:])).decryptor(); d.authenticate_additional_data(h+flag)
     try: data=d.update(blob[:-TAG_SIZE])+d.finalize()
     except Exception as e: raise IntegrityError("DECRYPT_FAILED") from e
     if last:
      if len(data)<32: raise IntegrityError("DECRYPT_FAILED")
      plain,want=data[:-32],data[-32:]; digest.update(plain)
      if not secrets.compare_digest(digest.digest(),want): raise IntegrityError("DECRYPT_FAILED")
      o.write(plain); break
     digest.update(data); o.write(data); n+=1
  part.replace(dst)
 except Exception: part.unlink(missing_ok=True); raise
