import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
import sys, glob, numpy as np, cv2

from render2 import load
from parts import surf_sampler, honeycomb, text_mesh, disc
from rotor import rotor_wheel
from composite2 import ring_block, bbox, f
from matplotlib.path import Path
# colours (BGR) + gloss
TORNADO=(128,124,118); BLACK=(22,22,24); GLOSS=(14,14,16); DARK=(48,48,50); GLASS=(36,34,32)
TYRE=(30,30,30); RED=(36,36,205); DISC=(105,105,108); HEAD=(150,150,156); TAIL=(30,30,150)
BACK=(8,8,9); HONEY=(40,40,44); CHROME=(205,205,210)
SIL=(196,196,198); SIL_D=(140,140,144); HUB=(120,120,124)
class Scene:
    def __init__(s): s.V=[];s.F=[];s.C=[];s.S=[];s.n=0
    def add(s,V,F,cols,shine=0.5):
        if len(F)==0: return
        s.V.append(np.asarray(V,np.float32)); s.F.append(np.asarray(F)+s.n); s.C+=list(cols)
        s.S+= list(shine) if np.ndim(shine) else [shine]*len(F); s.n+=len(V)
def body_parts(V,F,G):
    T=V[F]; c=T.mean(1); base={0:BLACK,1:TORNADO,2:HEAD,3:DARK,4:DARK,5:GLOSS,6:DARK,7:BLACK,8:DARK}
    C=np.array([base.get(g,DARK) for g in G],np.float32); S=np.full(len(F),0.6); keep=np.ones(len(F),bool)
    S[G==1]=1.0
    front=c[:,0]<-3.3
    big=Path(np.array([(-1.07,1.10),(0.42,1.10),(0.42,0.95),(0.25,0.40),(-0.90,0.40),(-1.07,0.95)]))
    ing=front & big.contains_points(c[:,[2,1]]) & (G!=1)
    C[ing]=GLOSS; S[ing]=0.5
    keep[ing & ((G==3)|(G==4))]=False             # remove the S6 grille bars
    C[ing & (G==5)]=BACK; S[ing&(G==5)]=0.0
    inner=Path(np.array([(-0.905,1.03),(0.235,1.03),(0.315,0.958),(0.175,0.475),(-0.845,0.475),(-0.985,0.958)]))
    rings=(c[:,1]>0.84)&(np.abs(c[:,2]+0.335)<0.30)
    inn=(c[:,0]<-3.2) & inner.contains_points(c[:,[2,1]]) & ~rings
    C[inn]=BACK; S[inn]=0.0
    C[(c[:,0]<-3.2)&rings&(G!=1)]=GLOSS
    intake=(c[:,0]<-3.1)&(c[:,1]>0.40)&(c[:,1]<0.68)&((c[:,2]<-0.97)|(c[:,2]>0.33))&(G!=1)
    C[intake]=BLACK; S[intake]=0.2
    fender=(c[:,0]>-2.95)&(c[:,0]<-2.15)&(c[:,1]>0.95)&(c[:,1]<1.45)&(G!=1)&(np.abs(c[:,2]+0.335)>0.85)
    C[fender]=TORNADO; S[fender]=1.0
    return C,S,keep
def build(lower=0.05, wheels="rotor"):
    sc=Scene(); sh=np.array((0,-lower,0),np.float32)
    V,F,G,n=load(f(59)); V=V+sh; C,S,keep=body_parts(V,F,G)
    sc.add(V,F[keep],[tuple(x) for x in C[keep]],S[keep])
    for i,col,shn in ((68,GLASS,1.0),(40,HEAD,1.0),(41,TAIL,0.8)):
        Vv,Ff,Gg,_=load(f(i)); sc.add(Vv+sh,Ff,[col]*len(Ff),shn)
    # wheels at the S6 positions
    W={'FL':(91,(-0.06,0.016,-0.031),110,1),'FR':(89,(-0.054,0.016,-0.024),109,-1)}
    for k,(i,s_,ref,side) in W.items():
        Vv,Ff,Gg,_=load(f(i)); Vv=Vv+np.array(s_,np.float32)
        keepw=(Gg!=1) if wheels=="rotor" else np.ones(len(Ff),bool)
        sc.add(Vv,Ff[keepw],[{0:TYRE,1:SIL,2:DISC}[g] for g in Gg[keepw]],[{0:0.1,1:0.9,2:0.4}[g] for g in Gg[keepw]])
    Vv,Ff,Gg,_=load(f(90)); Vv=Vv+np.array((-0.047,0.016,-0.027),np.float32)
    keepw=(Gg!=1) if wheels=="rotor" else np.ones(len(Ff),bool)
    sc.add(Vv,Ff[keepw],[{0:TYRE,1:SIL,2:DISC}[g] for g in Gg[keepw]],[{0:0.1,1:0.9,2:0.4}[g] for g in Gg[keepw]])
    centers=[]
    for ref,side in ((110,1),(109,-1)):
        a,b=bbox(ref); c=(a+b)/2; zo=(b[2] if side>0 else a[2]); centers.append((c,zo,side,0.35,0.18,-30,45))
    a,b=bbox(111); c=(a+b)/2
    centers+= [(c,b[2],1,0.32,0.17,-25,40),(c,a[2],-1,0.32,0.17,-25,40)]
    for c,zo,side,r1,r0,a0,a1 in centers:
        z1=zo-side*0.065; z0=zo-side*0.13
        Vv,Ff=ring_block(c,r0,r1,a0,a1,min(z0,z1),max(z0,z1)); sc.add(Vv,Ff,[RED]*len(Ff),0.6)
        if wheels=="rotor":
            rv,rf,rk=rotor_wheel(c, zo-side*0.008, side)
            sc.add(rv,rf,[(SIL,SIL_D,SIL,HUB)[k] for k in rk],[(0.9,0.5,0.9,0.6)[k] for k in rk])
    # grille honeycomb, quattro, ACC
    sx=surf_sampler(V,F,(G==5))
    from parts import surf_sampler2, rs_honeycomb
    gpoly=np.array([(-0.89,1.022),(0.22,1.022),(0.30,0.955),(0.165,0.48),(-0.835,0.48),(-0.97,0.955)])
    hv,hf=rs_honeycomb(surf_sampler2(V,F,(G==5)),poly=gpoly,maxd=1.0); sc.add(hv,hf,[(26,26,28)]*len(hf),0.18)
    pass  # quattro badge in the grille removed (owner's choice)
    T=V[F]; cc=T.mean(1)
    for zc in (-1.17,0.50):
        m=(np.abs(cc[:,2]-zc)<0.05)&(np.abs(cc[:,1]-(0.53-lower))<0.05)&(cc[:,0]<-3.0)
        x=cc[m,0].min()-0.01 if m.any() else -3.45
        dv,df=disc(zc,0.53-lower,0.055,x); sc.add(dv,df,[GLOSS]*len(df),0.15)
    return sc
def render(sc,yaw,pitch,W=960,H=540,zoom=1.0,center=None):
    V=np.vstack(sc.V);F=np.vstack(sc.F);C=np.array(sc.C,np.float32);S=np.array(sc.S,np.float32)
    mid=(V.max(0)+V.min(0))/2; c=mid if center is None else np.array(center,np.float32)
    a,b=np.radians(yaw),np.radians(pitch)
    Ry=np.array([[np.cos(a),0,np.sin(a)],[0,1,0],[-np.sin(a),0,np.cos(a)]]); Rx=np.array([[1,0,0],[0,np.cos(b),-np.sin(b)],[0,np.sin(b),np.cos(b)]])
    P=(V-c)@Ry.T@Rx.T; allp=(V-mid)@Ry.T@Rx.T
    s=zoom*0.85*min(W/np.ptp(allp[:,0]),H/np.ptp(allp[:,1]))
    xy=np.stack([P[:,0]*s+W/2,-P[:,1]*s+H/2],1); T=P[F]
    nrm=np.cross(T[:,1]-T[:,0],T[:,2]-T[:,0]); nrm/=(np.linalg.norm(nrm,axis=1,keepdims=True)+1e-9)
    L=np.array([0.4,0.7,0.6]); L/=np.linalg.norm(L); dif=np.abs(nrm@L)
    Hh=L+np.array([0,0,1.0]); Hh/=np.linalg.norm(Hh); spec=np.abs(nrm@Hh)**40
    img=np.zeros((H,W,3),np.uint8); img[:]=np.linspace(70,20,H)[:,None,None].astype(np.uint8)
    pts=xy[F]; ok=(pts[:,:,0].max(1)>-50)&(pts[:,:,0].min(1)<W+50)&(pts[:,:,1].max(1)>-50)&(pts[:,:,1].min(1)<H+50)
    for i in np.argsort(T[:,:,2].mean(1)):
        if not ok[i]: continue
        col=np.clip(C[i]*(0.32+0.6*dif[i])+90*S[i]*spec[i],0,255)
        cv2.fillConvexPoly(img,np.round(pts[i]*4).astype(np.int32),tuple(int(x) for x in col),lineType=cv2.LINE_AA,shift=2)
    return img
if __name__=="__main__":
    sc=build()
    a=render(sc,-30,12); b=render(sc,150,12)
    side=render(sc,180,0); whl=render(sc,180,0,zoom=3.4,center=(-2.17,0.52,1.0))
    frz=render(sc,90,4,zoom=2.4,center=(-3.4,0.70,-0.33)); fr=render(sc,90,4)
    whl2=render(sc,0,0,zoom=3.4,center=(-2.17,0.52,-1.55))
    cv2.imwrite(sys.argv[1],np.hstack([whl,whl2]),[cv2.IMWRITE_JPEG_QUALITY,88]) if len(sys.argv)>2 else cv2.imwrite(sys.argv[1],np.vstack([np.hstack([a,b]),np.hstack([side,whl]),np.hstack([fr,frz])]),[cv2.IMWRITE_JPEG_QUALITY,88])











