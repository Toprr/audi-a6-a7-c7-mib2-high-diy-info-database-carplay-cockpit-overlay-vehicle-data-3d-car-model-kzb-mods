# Stage 2 - in-place edits only: same sizes, directory unchanged.
# lower 3.5 cm, RS7 5-arm rims on the RS6 axles, red calipers, black exhaust tips / rings / trim, RS6 badge removed.
# Input WORK/content_stufe1b.kzb + WORK/kzb_dir.json, output WORK/content_stufe2.kzb
import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
import json,struct,sys,hashlib,numpy as np
from kzbmesh import parse_auto, build
d=bytearray(open(WORK+"/content_stufe1b.kzb","rb").read())
F=json.load(open(WORK+"/kzb_dir.json"))["files"]; P={f["path"]:(i+1,f) for i,f in enumerate(F)}
ZC=-0.335; LOWER=0.035
LOG=[]; REG=[]
def blobrange(i): f=F[i-1]; n0=struct.unpack_from("<I",d,f["off"])[0]; return f["off"], f["off"]+f["size"]+4+n0
def mesh(i): f=F[i-1]; return parse_auto(bytes(d[f["off"]:f["off"]+f["size"]]),f["off"])
def put_mesh(i,m):
    f=F[i-1]; new=build(m,m["ref"]); assert len(new)==f["size"],(i,len(new),f["size"])
    # tail (bounding box) sits at the end: 8x0 + 6 floats + 4x0, from off+size-len(tail)
    d[f["off"]:f["off"]+f["size"]]=new; REG.append((f["off"],f["off"]+f["size"]+40)); LOG.append("mesh %d %s rewritten (same size)"%(i,f["path"]))
def set_bbox(i,P3):
    f=F[i-1]; m=mesh(i); start=f["off"]+f["size"]-len(m["tail"]); assert bytes(d[start:start+8])==b"\0"*8
    struct.pack_into("<6f",d,start+8,*P3.min(0),*P3.max(0)); REG.append((start,start+36))
def verts(m):
    dt=np.dtype([(f"a{k}",{6:"<f2",7:"<f4"}[a["typ"]],(a["dim"],)) for k,a in enumerate(m["attrs"])])
    return np.frombuffer(m["verts"],dtype=dt).copy()
def matpos(i):
    a,b=blobrange(i); k=bytes(d[a:b]).find(struct.pack("<III",0x50,0x0b,2)); assert k>0; return a+k+16
# ---------- A) group matrices: body/interior lower, reflection group the opposite way
for name,dy in (("Exterior",-LOWER),("Interior_left",-LOWER),("Interior_right",-LOWER),("Mirror",+LOWER)):
    i,_=P["/Scenes/AU572_RS6_Avant_PA_Geometry/Root Node/"+name]; p=matpos(i)+13*4
    old=struct.unpack_from("<f",d,p)[0]; struct.pack_into("<f",d,p,old+dy); REG.append((p,p+4)); LOG.append("group %s: y %.4f -> %.4f"%(name,old,old+dy))
# ---------- B) RS7 wheels on RS6 axles (+LOWER), brake disc triangles -> red caliper
RS6C={"FL":np.array([-2.165,0.52,0.905]),"FR":np.array([-2.165,0.52,-1.584]),"R":np.array([2.208,0.52,-0.34])}
OUT_L,OUT_R=1.10,-1.78
def caliper_tris(ctr,zo,side,r0,r1,a0=5,a1=65,seg=6):
    z1=zo-side*0.085; z0=zo-side*0.15; za,zb=min(z0,z1),max(z0,z1)
    A=np.radians(np.linspace(a0,a1,seg+1)); n=seg+1; V=[]
    for z in (za,zb):
        for r in (r0,r1):
            for a in A: V.append([ctr[0]+r*np.cos(a),ctr[1]+r*np.sin(a),z])
    V=np.array(V); I=lambda zi,ri,k: zi*2*n+ri*n+k; T=[]
    for k in range(seg):
        for zi in (0,1): T+=[[I(zi,0,k),I(zi,1,k),I(zi,1,k+1)],[I(zi,0,k),I(zi,1,k+1),I(zi,0,k+1)]]
        for ri in (0,1): T+=[[I(0,ri,k),I(1,ri,k),I(1,ri,k+1)],[I(0,ri,k),I(1,ri,k+1),I(0,ri,k+1)]]
    for k in (0,seg): T+=[[I(0,0,k),I(1,0,k),I(1,1,k)],[I(0,0,k),I(1,1,k),I(0,1,k)]]
    return V[np.array(T)]   # (nt,3,3)
for key,mname,rs6node in (("FL","AU573_RS7_PA_Wheel_FL","AU572_RS6_PA_Wheel_FL"),("FR","AU573_RS7_PA_Wheel_FR","AU572_RS6_PA_Wheel_FR"),("R","AU573_RS7_PA_Wheel_rear","AU572_RS6_PA_Wheels_Back")):
    mi,_=P["/Mesh Data/"+mname]; m=mesh(mi); v=verts(m); pos=v["a0"].astype(np.float64)
    c=(pos.min(0)+pos.max(0))/2; pos[:,0]+=RS6C[key][0]-c[0]; pos[:,1]+=RS6C[key][1]-c[1]
    L=pos[:,2]>ZC
    if L.any(): pos[L,2]+=OUT_L-pos[L,2].max()
    if (~L).any(): pos[~L,2]+=OUT_R-pos[~L,2].min()
    pos[:,1]+=LOWER
    # build the caliper from the brake-disc vertices (sub-mesh 2) - vertex count stays the same
    sub=m["subs"][2]; idx=np.frombuffer(sub["idx"],"<u2").copy(); dv=np.unique(idx)
    sides=[1] if key=="FL" else ([-1] if key=="FR" else [1,-1])
    tris=np.concatenate([caliper_tris(RS6C[key]+np.array([0,LOWER,0]),OUT_L if sd>0 else OUT_R,sd,0.19 if key!="R" else 0.18,0.31 if key!="R" else 0.28) for sd in sides])
    need=tris.shape[0]*3; assert need<=len(dv) and tris.shape[0]<=len(idx)//3,(need,len(dv))
    uv0=v["a2"][dv[0]].copy()
    newidx=[]
    for t,tri in enumerate(tris):
        nrm=np.cross(tri[1]-tri[0],tri[2]-tri[0]); nrm/=np.linalg.norm(nrm)+1e-12
        for k in range(3):
            vi=dv[t*3+k]; pos[vi]=tri[k]; v["a1"][vi]=nrm; v["a2"][vi]=uv0; newidx.append(vi)
    rest=dv[need:]; pos[rest]=tris[0][0]                    # park unused vertices inside the caliper
    while len(newidx)<len(idx): newidx+= [dv[0],dv[0],dv[0]]  # remaining triangles degenerate (invisible)
    sub["idx"]=np.array(newidx,"<u2").tobytes()
    v["a0"]=pos.astype(np.float16); m["verts"]=v.tobytes(); put_mesh(mi,m); set_bbox(mi,pos)
    # point the RS6 wheel nodes to the RS7 meshes
    ni,nf=P["/Scenes/AU572_RS6_Avant_PA_Geometry/Root Node/Exterior/"+rs6node]; a,b=blobrange(ni)
    old=struct.unpack_from("<I",d,b-4)[0]; struct.pack_into("<I",d,b-4,mi); REG.append((b-4,b)); LOG.append("wheel node %s: %d -> %d (%s)"%(rs6node,old,mi,mname))
# ---------- C) move the reflection wheels against the Mirror group
mi,_=P["/Mesh Data/AU572_RS6_PA_mirror_Wheels"]; m=mesh(mi); v=verts(m); pos=v["a0"].astype(np.float64); pos[:,1]-=LOWER
v["a0"]=pos.astype(np.float16); m["verts"]=v.tobytes(); put_mesh(mi,m); set_bbox(mi,pos)
# ---------- D) RS6 body: exhaust tips black, rings + trim black, RS6 badge removed
mi,_=P["/Mesh Data/AU572_RS6_PA_Avant_Exterieur"]; m=mesh(mi); v=verts(m); pos=v["a0"].astype(np.float64)
s0,s2,s3=m["subs"][0],m["subs"][2],m["subs"][3]; assert (s0["mat"],s2["mat"],s3["mat"])==(486,420,420)
s3["mat"]=486; LOG.append("exhaust sub-mesh: material 420 -> 486 (matt black)")
i2=np.frombuffer(s2["idx"],"<u2").reshape(-1,3).copy(); cc=pos[i2].mean(1)
ring=((cc[:,0]<-3.3)&(cc[:,1]>0.84)&(np.abs(cc[:,2]-ZC)<0.3)) | ((cc[:,0]>3.3)&(cc[:,1]>1.12)&(np.abs(cc[:,2]-ZC)<0.3))
strip=(cc[:,0]>3.3)&(cc[:,1]<0.975)
badge=(cc[:,0]>3.3)&(cc[:,1]>0.975)&(cc[:,1]<1.12)&(cc[:,2]-ZC>0.45)
move=np.where(ring|strip)[0]
if len(move)%2: badge[move[-1]]=True; move=move[:-1]
i2[badge]=i2[badge][:,:1]          # degenerate
i0=np.frombuffer(s0["idx"],"<u2").reshape(-1,3)
keep=np.ones(len(i2),bool); keep[move]=False
s0["idx"]=np.concatenate([i0,i2[move]]).astype("<u2").tobytes(); s0["cnt"]=len(s0["idx"])//2
s2["idx"]=i2[keep].astype("<u2").tobytes(); s2["cnt"]=len(s2["idx"])//2
LOG.append("rings/trim: %d triangles -> matt, RS6 badge: %d triangles degenerated"%(len(move),int(badge.sum())))
put_mesh(mi,m)
# ---------- E) RS7 brake disc material -> red (caliper)
mi,f=P["/Materials/PhongTextured/AU573_RS7_PA_BrakeDisc"]; a,b=blobrange(mi); blob=bytes(d[a:b])
for pid,val in ((0x60,(0.22,0.01,0.01)),(0x63,(1.0,0.06,0.05)),(0x67,(0.6,0.35,0.35))):
    k=blob.find(struct.pack("<III",0x20,pid,2)); assert k>0; p=a+k+16
    LOG.append("Material RS7_BrakeDisc prop %#x: %s -> %s"%(pid,[round(x,3) for x in struct.unpack_from("<3f",d,p)],val)); struct.pack_into("<3f",d,p,*val); REG.append((p,p+12))
open(WORK+"/content_stufe2.kzb","wb").write(d); json.dump(REG,open(WORK+"/stufe2_regionen.json","w"))
for l in LOG: print(l)
print("SHA1",hashlib.sha1(d).hexdigest(),"size",len(d))
