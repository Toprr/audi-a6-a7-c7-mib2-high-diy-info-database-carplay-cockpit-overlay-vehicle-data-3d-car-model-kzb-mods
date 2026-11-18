# Scene container for the preview renderers (glrender.py) and the caliper mesh helper.
import numpy as np

class Scene:
    def __init__(s): s.V=[];s.F=[];s.C=[];s.S=[];s.n=0
    def add(s,V,F,cols,shine=0.5):
        if len(F)==0: return
        s.V.append(np.asarray(V,np.float32)); s.F.append(np.asarray(F)+s.n); s.C+=list(cols)
        s.S+= list(shine) if np.ndim(shine) else [shine]*len(F); s.n+=len(V)
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
