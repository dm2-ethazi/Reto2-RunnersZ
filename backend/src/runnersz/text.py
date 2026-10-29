import re
def vigenere(text,key,decrypt=False):
 if not key or not re.fullmatch("[A-Za-z]+",key): raise ValueError("INVALID_KEY")
 key=key.upper(); out=[]; i=0
 for c in text:
  if c.isascii() and c.isalpha():
   base=65 if c.isupper() else 97; s=ord(key[i%len(key)])-65
   out.append(chr((ord(c)-base+(-s if decrypt else s))%26+base)); i+=1
  else: out.append(c)
 return "".join(out)
def caesar(text,shift,decrypt=False):
 if isinstance(shift,bool) or not isinstance(shift,int) or not 1<=shift<=25: raise ValueError("INVALID_SHIFT")
 s=-shift if decrypt else shift
 return "".join(chr((ord(c)-(65 if c.isupper() else 97)+s)%26+(65 if c.isupper() else 97)) if c.isascii() and c.isalpha() else c for c in text)
