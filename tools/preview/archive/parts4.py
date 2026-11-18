# v7: continuous edges up to the wheel arches (front lip, side skirts, rear splitters)
import numpy as np
from slicer import slice_mesh
from parts2 import strip_mesh
from parts3 import front_contour
ZC=-0.335
def side_pts(V,F,xs):
    out=[]
    for x in xs:
        p=slice_mesh(V,F,0,x); p=p[(p[:,2]>ZC)&(p[:,1]<0.75)]
        y0=p[:,1].min(); q=p[p[:,1]<y0+0.05]; out.append([x,y0,q[:,2].max()])
    return np.array(out)
def smooth(P,w=5):
    if len(P)<w: return P
    k=np.ones(w)/w; Q=P.copy()
    for j in range(3):
        pad=np.r_[np.repeat(P[0,j],w//2),P[:,j],np.repeat(P[-1,j],w//2)]
        Q[:,j]=np.convolve(pad,k,'valid')
    Q[0]=P[0]; Q[-1]=P[-1]; return Q
def blade(P, outer, inner, th, lift, cx):
    """P: Pfad (n,3). Querschnitt nach aussen (weg von Wagenmitte (cx,ZC)) / innen"""
    T=np.gradient(P[:,[0,2]],axis=0); T/=np.linalg.norm(T,axis=1,keepdims=True)
    N=np.stack([-T[:,1],T[:,0]],1)
    mid=np.stack([np.full(len(P),cx),np.full(len(P),ZC)],1)
    s=np.sign(((P[:,[0,2]]-mid)*N).sum(1)); N*=np.where(s==0,1,s)[:,None]
    rows=[]
    for p,nrm,o,i in zip(P,N,np.broadcast_to(outer,len(P)),np.broadcast_to(inner,len(P))):
        po=p[[0,2]]+nrm*o; pi=p[[0,2]]-nrm*i; y=p[1]
        rows.append([[po[0],y-th,po[1]],[po[0],y+lift,po[1]],[pi[0],y+lift,pi[1]],[pi[0],y-th,pi[1]]])
    return strip_mesh(rows)
def mirror(P): Q=P.copy(); Q[:,2]=2*ZC-Q[:,2]; return Q
def front_wrap(V,Fp,Fall):
    side=side_pts(V,Fall,np.arange(-2.79,-3.33,-0.02))            # left side, from the wheel arch forward
    fx,fy=front_contour(V,Fp)
    us=np.linspace(0.98,-0.98,80)
    front=np.stack([fx(us),fy(us),ZC+us],1)
    P=np.vstack([side,front,mirror(side)[::-1]]); P=smooth(P,7)
    # overhang: 2.5 cm at the sides, 5.5 cm at the front (smooth transition)
    t=np.linspace(0,1,len(P)); ns=len(side)/len(P)
    w=np.clip(np.minimum(t,1-t)/ns,0,1); outer=0.025+0.03*w**2
    return blade(P,outer,0.16,0.030,0.006,0.0)
def skirts(V,F):
    P=side_pts(V,F,np.linspace(-1.57,1.64,50)); P=smooth(P,9)
    a=blade(P,0.045,0.09,0.032,0.006,0.0); b=blade(mirror(P),0.045,0.09,0.032,0.006,0.0)
    return np.vstack([a[0],b[0]]),np.vstack([a[1],b[1]+len(a[0])])
def rear_split(V,F):
    P=side_pts(V,F,np.linspace(2.88,3.50,30)); P=smooth(P,7)
    a=blade(P,0.04,0.12,0.035,0.006,0.0); b=blade(mirror(P),0.04,0.12,0.035,0.006,0.0)
    return np.vstack([a[0],b[0]]),np.vstack([a[1],b[1]+len(a[0])])

