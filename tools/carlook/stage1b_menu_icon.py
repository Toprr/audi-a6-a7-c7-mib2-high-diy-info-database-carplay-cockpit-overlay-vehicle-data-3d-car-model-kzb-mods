# Stage 1b: replace the main-menu 'Car' icon PNGs in place with exactly the same byte length
# (new PNG + padding tEXt chunk). Input: WORK/content_stufe1.kzb, WORK/icon_enabled.png, WORK/icon_highlighted.png
import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
import json,struct,zlib,hashlib,sys,numpy as np,cv2
d=bytearray(open(WORK+"/content_stufe1.kzb","rb").read()); base=open(WORK+"/content.kzb","rb").read()
F=json.load(open(WORK+"/kzb_dir.json"))["files"]
def ihdr(png): w,h,bd,ct,cm,fm,il=struct.unpack(">IIBBBBB",png[16:29]); return dict(w=w,h=h,bitdepth=bd,colortype=ct,interlace=il)
def chunk(t,data): return struct.pack(">I",len(data))+t+data+struct.pack(">I",zlib.crc32(t+data)&0xffffffff)
def pad_to(png,target):
    iend=png.rfind(b"IEND")-4; kw=b"Comment\x00"; n=target-len(png)-12-len(kw)
    if n<0: raise ValueError("PNG too big: %d > %d - compress more or simplify the image"%(len(png),target))
    out=png[:iend]+chunk(b"tEXt",kw+b"A6 CarLook"[:n].ljust(n,b" "))+png[iend:]
    assert len(out)==target; return out
for idx,src in ((4989,"icon_enabled.png"),(4990,"icon_highlighted.png")):
    f=F[idx-1]; old=base[f["off"]+4:f["off"]+4+f["size"]]
    img=cv2.imread(WORK+"/"+src,cv2.IMREAD_UNCHANGED)
    ok,enc=cv2.imencode(".png",img,[cv2.IMWRITE_PNG_COMPRESSION,9]); png=enc.tobytes()
    print(f["path"].split("/")[-1],"old",ihdr(old),"new",ihdr(png),"length old",len(old),"new",len(png))
    assert ihdr(png)["w"]==ihdr(old)["w"] and ihdr(png)["h"]==ihdr(old)["h"] and ihdr(png)["colortype"]==ihdr(old)["colortype"] and ihdr(png)["bitdepth"]==ihdr(old)["bitdepth"]
    new=pad_to(png,len(old))
    chk=cv2.imdecode(np.frombuffer(new,np.uint8),cv2.IMREAD_UNCHANGED); assert chk is not None and (chk==img).all()
    assert d[f["off"]:f["off"]+4]==b"\0\0\0\0"
    d[f["off"]+4:f["off"]+4+len(new)]=new
open(WORK+"/content_stufe1b.kzb","wb").write(d)
print("SHA1",hashlib.sha1(d).hexdigest(),"size",len(d))
