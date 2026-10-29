import os,tempfile
from pathlib import Path
from fastapi import FastAPI,File,Form,Header,UploadFile
from fastapi.responses import Response,JSONResponse
from pydantic import BaseModel
from .crypto import CryptoError,encrypt_file,decrypt_file
from .text import vigenere,caesar
app=FastAPI(title="RunnersZ API",version="2.0.0"); TOKEN=os.getenv("RZ_API_TOKEN",""); LIMIT=int(os.getenv("RZ_MAX_UPLOAD_MIB","100"))*1048576
class Text(BaseModel): text:str; key:str|None=None; shift:int|None=None; mode:str="encrypt"
def auth(a):
 if not TOKEN or a!=f"Bearer {TOKEN}": raise ValueError("UNAUTHORIZED")
def fail(code,status): return JSONResponse(status,{"code":code,"message":code})
@app.get("/api/v1/health")
def health(): return {"status":"ok","version":"2.0.0"}
@app.get("/api/v1/auth/check",status_code=204)
def check(authorization: str|None=Header(None)):
 try: auth(authorization)
 except ValueError: return fail("UNAUTHORIZED",401)
async def upload(file):
 data=await file.read(LIMIT+1)
 if not data:return None,"EMPTY_FILE"
 if len(data)>LIMIT:return None,"FILE_TOO_LARGE"
 p=Path(tempfile.mktemp()); p.write_bytes(data); return p,None
@app.post("/api/v1/text/vigenere")
def vig(x:Text,authorization: str|None=Header(None)):
 try: auth(authorization); return {"result":vigenere(x.text,x.key or "",x.mode=="decrypt")}
 except ValueError as e:return fail(str(e),401 if str(e)=="UNAUTHORIZED" else 400)
@app.post("/api/v1/text/caesar")
def ces(x:Text,authorization: str|None=Header(None)):
 try: auth(authorization); return {"result":caesar(x.text,x.shift or 0,x.mode=="decrypt")}
 except ValueError as e:return fail(str(e),401 if str(e)=="UNAUTHORIZED" else 400)
async def fileop(file,password,authorization,dec):
 try: auth(authorization)
 except ValueError:return fail("UNAUTHORIZED",401)
 src,err=await upload(file)
 if err:return fail(err,413 if err=="FILE_TOO_LARGE" else 400)
 dst=src.with_suffix(".out")
 try:
  (decrypt_file if dec else encrypt_file)(src,dst,password)
  return Response(dst.read_bytes(),media_type="application/octet-stream",headers={"X-Integrity":"verified"})
 except CryptoError as e:return fail(str(e),400 if str(e) in ("EMPTY_PASSWORD","EMPTY_FILE") else 422)
 finally: src.unlink(missing_ok=True); dst.unlink(missing_ok=True); dst.with_name(dst.name+".part").unlink(missing_ok=True)
@app.post("/api/v1/files/encrypt")
async def enc(file:UploadFile=File(...),password:str=Form(...),authorization:str|None=Header(None)): return await fileop(file,password,authorization,False)
@app.post("/api/v1/files/decrypt")
async def dec(file:UploadFile=File(...),password:str=Form(...),authorization:str|None=Header(None)): return await fileop(file,password,authorization,True)
