# v9 preview design: RS6 Avant body (mesh 67), RS7 5-arm rims on the RS6 axles. Works on the OBJ files from kzb_extract.py (WORK/obj).
import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
import sys, glob, numpy as np, cv2

from render2 import load
from composite4 import Scene
from composite2 import ring_block
from parts import surf_sampler2, rs_honeycomb
from parts4 import front_wrap, skirts, rear_split
f=lambda i: glob.glob(WORK+"/obj/%03d_*.obj"%i)[0]
ZC=-0.335
PAINT=(132,129,124)          # BGR: silver grey, slightly cooler than Tornado grey
GLOSS=(14,14,16); TRIM=(22,22,24); GLASS=(34,32,30); HEAD=(170,170,176); TAIL=(28,28,150)
TYRE=(30,30,30); SIL=(170,170,174); DISC=(100,100,104); RED=(36,36,205); HONEY=(24,24,26)
RS6C={"FL":np.array([-2.165,0.52,0.905]),"FR":np.array([-2.165,0.52,-1.584]),"R":np.array([2.208,0.52,-0.34])}
RS6_OUT_L,RS6_OUT_R=1.10,-1.78
def wheel(sc,i,ctr):
    V,F,G,_=load(f(i)); c=(V.min(0)+V.max(0))/2
    V=V.copy(); V[:,0]+=ctr[0]-c[0]; V[:,1]+=ctr[1]-c[1]
    L=V[:,2]>ZC
    if L.any(): V[L,2]+=RS6_OUT_L-V[L,2].max()
    if (~L).any(): V[~L,2]+=RS6_OUT_R-V[~L,2].min()
    sc.add(V,F,[{0:TYRE,1:SIL,2:DISC}[g] for g in G],[{0:0.1,1:0.6,2:0.4}[g] for g in G])
def build(lower=0.035):
    sc=Scene(); sh=np.array((0,-lower,0),np.float32)
    V,F,G,n=load(f(67)); V=V+sh; T=V[F]; c=T.mean(1)
    C=np.zeros((len(F),3)); S=np.zeros(len(F))
    C[G==1]=PAINT; S[G==1]=1.0
    C[G==0]=GLOSS; S[G==0]=0.7
    C[(G==2)&(c[:,0]<0)]=HEAD; S[(G==2)&(c[:,0]<0)]=1.0
    C[(G==2)&(c[:,0]>0)]=GLOSS; S[(G==2)&(c[:,0]>0)]=0.8      # rear: rings + trim black
    rings_f=(G==2)&(c[:,0]<0)&(np.abs(c[:,2]-ZC)<0.3); C[rings_f]=GLOSS; S[rings_f]=0.9
    C[G==3]=(16,16,18); S[G==3]=0.8          # exhaust tips black
    C[G==4]=TRIM; S[G==4]=0.5                # roof rails black
    keep=np.ones(len(F),bool)
    # remove the RS6 badge at the rear (small triangles, not paint, lower tailgate)
    A=0.5*np.linalg.norm(np.cross(T[:,1]-T[:,0],T[:,2]-T[:,0]),axis=1)
    keep&=~((c[:,0]>3.4)&(c[:,1]>0.85-lower)&(c[:,1]<1.15-lower)&(G!=1)&(A<6e-5)&~((c[:,1]>1.18-lower)&(np.abs(c[:,2]-ZC)<0.3)))
    keep&=~((G==2)&(c[:,0]>3.3)&(c[:,1]<1.12-lower)&(A<2e-4)&(np.abs(c[:,2]-ZC)>0.25)&(np.abs(c[:,2]-ZC)<0.75))   # RS6 logo at the rear removed
    keep&=~((G==2)&(c[:,0]>3.3)&(c[:,1]>0.975-lower)&(c[:,1]<1.12-lower)&(c[:,2]-ZC>0.45))   # RS6 logo removed, trim stays (black)
    sc.add(V,F[keep],[tuple(x) for x in C[keep]],S[keep])
    Vg,Fg,_,_=load(f(68)); sc.add(Vg+sh,Fg,[GLASS]*len(Fg),1.0)
    Vt,Ft,_,_=load(f(41)); sc.add(Vt+sh,Ft,[TAIL]*len(Ft),0.9)       # tail lights
    # wheels + calipers
    wheel(sc,91,RS6C["FL"]); wheel(sc,89,RS6C["FR"]); wheel(sc,90,RS6C["R"])
    for key,r1,r0 in (("FL",0.31,0.19),("FR",0.31,0.19),("R",0.28,0.18)):
        ctr=RS6C[key]
        sides=[1,-1] if key=="R" else ([1] if key=="FL" else [-1])
        for side in sides:
            zo=RS6_OUT_L if side>0 else RS6_OUT_R
            z1=zo-side*0.085; z0=zo-side*0.15
            Vv,Ff=ring_block(ctr,r0,r1,5,65,min(z0,z1),max(z0,z1)); sc.add(Vv,Ff,[RED]*len(Ff),0.6)
    # honeycomb grille (upper grille only)
    nr=np.cross(T[:,1]-T[:,0],T[:,2]-T[:,0]); nr/=np.linalg.norm(nr,axis=1,keepdims=True)+1e-9
    gm=(G==0)&(c[:,0]<-3.35)&(np.abs(c[:,2]-ZC)<0.75)&(np.abs(nr[:,0])>0.7)
    poly=np.array([(-0.94,1.075-lower),(0.255,1.075-lower),(0.29,0.98-lower),(0.27,0.67-lower),(-0.93,0.67-lower),(-0.96,0.98-lower)])
    hv,hf=rs_honeycomb(surf_sampler2(V,F,gm),poly=poly,maxd=1.0); sc.add(hv,hf,[HONEY]*len(hf),0.25)
    # lip, side skirts, rear splitters (gloss black) - preview only, not installed
    Fp=F[G==1]
    for mesh in (front_wrap(V,Fp,F),skirts(V,F),rear_split(V,Fp)):
        sc.add(mesh[0],mesh[1],[GLOSS]*len(mesh[1]),0.8)
    return sc
if __name__=="__main__":
    from glrender import render
    sc=build()
    views=[("Front",90,4),("vorne rechts",135,10),("rechte Seite",180,2),("hinten rechts",-135,10),
           ("Heck",-90,6),("hinten links",-45,10),("linke Seite",0,2),("vorne links",45,10),("von oben",130,45)]
    ims=[]
    for name,y,p in views:
        im=render(sc,y,p,W=960,H=540,zoom=(0.75 if p>40 else 1.0)); cv2.putText(im,name,(16,36),cv2.FONT_HERSHEY_SIMPLEX,1.0,(220,220,220),2,cv2.LINE_AA); ims.append(im)
    cv2.imwrite(sys.argv[1],np.vstack([np.hstack(ims[i:i+3]) for i in (0,3,6)]),[cv2.IMWRITE_JPEG_QUALITY,90])







