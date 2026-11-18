import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
import sys, glob, numpy as np, cv2
from render2 import load
O=WORK+"/obj"; f=lambda i: glob.glob(f"{O}/{i:03d}_*.obj")[0]
TORNADO=(128,124,118); BLACK=(22,22,24); GLOSS=(14,14,16); DARK=(48,48,50); GLASS=(36,34,32); SILVER=(192,192,194)
TYRE=(32,32,32); RED=(36,36,205); DISC=(110,110,112); CLEAR=(205,205,210); TAIL=(30,30,150)
def bbox(i): V=load(f(i))[0]; return V.min(0),V.max(0)
class Scene:
    def __init__(s): s.V=[];s.F=[];s.C=[];s.n=0
    def add(s,V,F,cols):
        s.V.append(V); s.F.append(F+s.n); s.C+=list(cols); s.n+=len(V)
    def addobj(s,i,cmap,shift=(0,0,0),default=DARK):
        V,F,G,nm=load(f(i)); s.add(V+np.array(shift,np.float32),F,[cmap.get(g,default) for g in G]); return V+np.array(shift,np.float32),F,G
def ring_block(c,r0,r1,a0,a1,z0,z1,seg=10):
    # curved block (brake caliper) around the z axis
    A=np.radians(np.linspace(a0,a1,seg+1)); V=[]
    for z in (z0,z1):
        for r in (r0,r1):
            for a in A: V.append([c[0]+r*np.cos(a), c[1]+r*np.sin(a), z])
    V=np.array(V,np.float32); n=seg+1; F=[]
    idx=lambda zi,ri,k: zi*2*n+ri*n+k
    for k in range(seg):
        for zi in (0,1):  # end faces
            F+=[[idx(zi,0,k),idx(zi,1,k),idx(zi,1,k+1)],[idx(zi,0,k),idx(zi,1,k+1),idx(zi,0,k+1)]]
        for ri in (0,1):  # outer/inner surface
            F+=[[idx(0,ri,k),idx(1,ri,k),idx(1,ri,k+1)],[idx(0,ri,k),idx(1,ri,k+1),idx(0,ri,k+1)]]
    for k in (0,seg):
        F+=[[idx(0,0,k),idx(1,0,k),idx(1,1,k)],[idx(0,0,k),idx(1,1,k),idx(0,1,k)]]
    return V,np.array(F)
def build(lower=0.05, opts=()):
    sc=Scene(); BODY_SHIFT=(0,-lower,0)
    BV,BF,BG=sc.addobj(59,{0:BLACK,1:TORNADO,2:CLEAR,3:DARK,4:DARK,5:GLOSS,6:DARK,7:BLACK,8:DARK},BODY_SHIFT)
    for i,c in ((68,GLASS),(40,CLEAR),(41,TAIL)): sc.addobj(i,{0:c},BODY_SHIFT)
    shifts={91:(-0.06,0.016,-0.031),89:(-0.054,0.016,-0.024),90:(-0.047,0.016,-0.027)}
    for i,sh in shifts.items():
        V,F,G=sc.addobj(i,{0:TYRE,1:SILVER,2:DISC},sh)
    # brake calipers: wheel centres taken from the S6 wheels
    for i,side in ((110,1),(109,-1)):
        a,b=bbox(i); c=(a+b)/2; zo=b[2] if side>0 else a[2]
        z1=zo-side*0.045; z0=zo-side*0.115
        V,F=ring_block(c,0.17,0.30,25,85,min(z0,z1),max(z0,z1)); sc.add(V,F,[RED]*len(F))
    a,b=bbox(111); c=(a+b)/2
    for zo,side in ((b[2],1),(a[2],-1)):
        z1=zo-side*0.045; z0=zo-side*0.115
        V,F=ring_block(c,0.16,0.27,30,85,min(z0,z1),max(z0,z1)); sc.add(V,F,[RED]*len(F))
    return sc
def render(sc,yaw,pitch,W=960,H=540,zoom=1.0,center=None):
    V=np.vstack(sc.V);F=np.vstack(sc.F);C=np.array(sc.C,np.float32)
    c=(V.max(0)+V.min(0))/2 if center is None else np.array(center,np.float32); P=V-c
    a,b=np.radians(yaw),np.radians(pitch)
    Ry=np.array([[np.cos(a),0,np.sin(a)],[0,1,0],[-np.sin(a),0,np.cos(a)]]); Rx=np.array([[1,0,0],[0,np.cos(b),-np.sin(b)],[0,np.sin(b),np.cos(b)]])
    P=P@Ry.T@Rx.T
    allp=(V-(V.max(0)+V.min(0))/2)@Ry.T@Rx.T
    s=zoom*0.85*min(W/np.ptp(allp[:,0]),H/np.ptp(allp[:,1]))
    xy=np.stack([P[:,0]*s+W/2,-P[:,1]*s+H/2],1); T=P[F]
    nrm=np.cross(T[:,1]-T[:,0],T[:,2]-T[:,0]); nrm/=(np.linalg.norm(nrm,axis=1,keepdims=True)+1e-9)
    L=np.array([0.4,0.7,0.6]); L/=np.linalg.norm(L); dif=np.abs(nrm@L); spec=np.abs(nrm[:,2])**12
    sh=0.30+0.60*dif
    img=np.zeros((H,W,3),np.uint8); img[:]=np.linspace(70,20,H)[:,None,None].astype(np.uint8)
    pts=xy[F]; vis=(np.abs(pts[:,:,0]).max(1)<W*3)
    for i in np.argsort(T[:,:,2].mean(1)):
        col=np.clip(C[i]*sh[i]+70*spec[i],0,255)
        cv2.fillConvexPoly(img,np.round(pts[i]*4).astype(np.int32),tuple(int(x) for x in col),lineType=cv2.LINE_AA,shift=2)
    return img
if __name__=="__main__":
    sc=build(lower=float(sys.argv[2]))
    side=render(sc,0,0); fr=render(sc,0,0,zoom=3.2,center=(-2.17,0.6,1.0))
    a=render(sc,-30,12); b=render(sc,150,12)
    cv2.imwrite(sys.argv[1],np.vstack([np.hstack([a,b]),np.hstack([side,fr])]),[cv2.IMWRITE_JPEG_QUALITY,88])
