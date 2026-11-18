# Rebuild the car scene FROM THE FINISHED .kzb the way the MMI does: A6 placeholder -> geometry node -> children -> meshes
# usage: python render_from_kzb.py modified.kzb out.jpg  (needs WORK/kzb_dir.json)
import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
import json,struct,sys,numpy as np,cv2
from kzbmesh import parse_auto
from composite4 import Scene
from glrender import render
KZB=sys.argv[1]; OUT=sys.argv[2]
d=open(KZB,"rb").read(); F=json.load(open(WORK+"/kzb_dir.json"))["files"]; P={f["path"]:(i+1,f) for i,f in enumerate(F)}
def blob(i): f=F[i-1]; n0=struct.unpack_from("<I",d,f["off"])[0]; return d[f["off"]:f["off"]+f["size"]+4+n0]
def lastref(i): b=blob(i); return struct.unpack_from("<I",b,len(b)-4)[0]
def mat(i):
    b=blob(i); k=b.find(struct.pack("<III",0x50,0x0b,2)); return np.array(struct.unpack_from("<16f",b,k+16)).reshape(4,4) if k>=0 else np.eye(4)
def diffuse(mid):
    b=blob(mid); k=b.find(struct.pack("<III",0x20,0x63,2)); a=b.find(struct.pack("<III",0x20,0x60,2))
    dif=np.array(struct.unpack_from("<3f",b,k+16)) if k>=0 else np.zeros(3); amb=np.array(struct.unpack_from("<3f",b,a+16)) if a>=0 else np.zeros(3)
    return dif,amb
NAMECOL={"Paint":None,"Matt":(20,20,22),"Tire":(28,28,28),"Rims":(175,175,180),"Alloy":(175,175,180),"Glass":(34,32,30),"Windows":(34,32,30),"Rear_Lights":(30,30,150),"Grillmat":(20,20,22),"Interior":(60,60,62)}
issues=[]
def addmesh(sc,mi,M):
    f=F[mi-1]; m=parse_auto(d[f["off"]:f["off"]+f["size"]],f["off"])
    dt=np.dtype([(f"a{k}",{6:"<f2",7:"<f4"}[a["typ"]],(a["dim"],)) for k,a in enumerate(m["attrs"])])
    v=np.frombuffer(m["verts"],dt); pos=v["a0"].astype(np.float64)
    if not np.isfinite(pos).all(): issues.append("NaN in "+f["path"])
    W=(np.c_[pos,np.ones(len(pos))]@M)[:,:3]
    for s in m["subs"]:
        idx=np.frombuffer(s["idx"],"<u2" if s["isz"]==2 else "<u4").reshape(-1,3)
        if len(idx) and idx.max()>=m["nv"]: issues.append("Index ausserhalb in "+f["path"])
        name=F[s["mat"]-1]["path"]; col=None
        for k,c in NAMECOL.items():
            if k in name: col=c
        if col is None:
            dif,amb=diffuse(s["mat"]); c=np.clip((dif*0.85+amb)*255,0,255); col=(c[2],c[1],c[0])
        ok=idx[:,0]!=idx[:,1]   # skip degenerate triangles
        sc.add(W,idx[ok].astype(np.int64),[tuple(col)]*int(ok.sum()),0.9 if "Paint" in name or "Rims" in name else 0.5)
def walk(node,M,sc,depth=0):
    path=F[node-1]["path"]; Mn=mat(node)@M
    for p,(i,f) in P.items():
        if p.startswith(path+"/") and p.count("/")==path.count("/")+1:
            if f["type"]==120: addmesh(sc,lastref(i),mat(i)@Mn)
            elif f["type"]==110: walk(i,Mn,sc,depth+1)
sc=Scene()
for name in ("Exterior_Left","Interior_Left"):
    ph,_=P["/Scenes/AU572_Avant_PA_CarMenu_Leftsided/Root Node/CarRoot/"+name]; tgt=lastref(ph)
    print(name,"->",F[tgt-1]["path"]); walk(tgt,mat(ph),sc)
print("problems:",issues if issues else "none")
views=[("Front",90,4),("front right",135,10),("right side",180,2),("rear right",-135,10),("rear",-90,6),("rear left",-45,10),("left side",0,2),("front left",45,10),("wheel close",160,4)]
ims=[]
for nm,y,pt in views:
    im=render(sc,y,pt,W=960,H=540,zoom=(2.6 if nm=="wheel close" else 1.0),center=((-2.165,0.5,1.0) if nm=="wheel close" else None))
    cv2.putText(im,nm,(16,36),cv2.FONT_HERSHEY_SIMPLEX,1.0,(220,220,220),2,cv2.LINE_AA); ims.append(im)
cv2.imwrite(OUT,np.vstack([np.hstack(ims[i:i+3]) for i in (0,3,6)]),[cv2.IMWRITE_JPEG_QUALITY,90])

