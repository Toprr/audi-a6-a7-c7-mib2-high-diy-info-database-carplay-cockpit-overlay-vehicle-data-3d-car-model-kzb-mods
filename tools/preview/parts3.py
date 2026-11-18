# add-on parts v6: follow the sliced body contours (slicer.py)
import numpy as np
from slicer import slice_mesh
from parts2 import strip_mesh
ZC=-0.335
def even_fit(u,v,deg=3):
    A=np.stack([u**(2*k) for k in range(deg+1)],1); c,*_=np.linalg.lstsq(A,v,rcond=None)
    return lambda uu: np.stack([np.asarray(uu)**(2*k) for k in range(deg+1)],-1)@c
def robust(u,v,fn):
    r=v-fn(u); k=np.abs(r)<2.5*np.median(np.abs(r))+1e-4; return u[k],v[k]
def front_contour(V,F):
    us=[];xs=[];ys=[]
    for z in np.arange(-1.35,0.66,0.05):
        p=slice_mesh(V,F,2,z); p=p[p[:,0]<-2.75]
        if len(p)==0: continue
        y0=p[:,1].min(); q=p[p[:,1]<y0+0.03]; us.append(z-ZC); xs.append(q[:,0].min()); ys.append(y0)
    us,xs,ys=map(np.array,(us,xs,ys))
    fx=even_fit(us,xs); u2,x2=robust(us,xs,fx); fx=even_fit(u2,x2)
    fy=even_fit(us,ys); return fx,fy
def front_lip(V,F, half=1.0, n=70):
    fx,fy=front_contour(V,F); rows=[]
    for u in np.linspace(-half,half,n):
        x=float(fx(u)); y=float(fy(u)); z=ZC+u
        xo=x-0.055                      # 5.5 cm overhang to the front
        rows.append([[xo,y-0.032,z],[xo-0.004,y-0.004,z],[x+0.02,y+0.006,z],[x+0.20,y+0.006,z],[x+0.20,y-0.020,z]])
    V2,F2=strip_mesh(rows)
    # 3 flaps per side under the lip
    out=list(V2); FF=list(F2)
    for u in (-0.75,-0.45,-0.15,0.15,0.45,0.75):
        x=float(fx(u))-0.05; y=float(fy(u))-0.030; z=ZC+u; t=0.006
        q=[[x,y,z-t],[x+0.10,y,z-t],[x+0.09,y-0.02,z-t],[x+0.01,y-0.025,z-t],[x,y,z+t],[x+0.10,y,z+t],[x+0.09,y-0.02,z+t],[x+0.01,y-0.025,z+t]]
        b=len(out); out+=q
        for fc in ([0,1,2],[0,2,3],[4,6,5],[4,7,6],[0,3,7],[0,7,4],[1,5,6],[1,6,2]): FF.append([b+i for i in fc])
    return np.array(out,np.float32),np.array(FF)
def _side_profile(V,F,xs):
    zs=[];ys=[]
    for x in xs:
        p=slice_mesh(V,F,0,x); p=p[(p[:,2]>ZC)&(p[:,1]<0.7)]
        y0=p[:,1].min(); q=p[p[:,1]<y0+0.06]; zs.append(q[:,2].max()); ys.append(y0)
    return np.array(zs),np.array(ys)
def _blade_along_x(xs,zo,ys,side,inner=0.09,outer=0.045,th=0.032,lift=0.006):
    rows=[]
    for x,z,y in zip(xs,zo,ys):
        zz=ZC+side*(z-ZC)
        rows.append([[x,y-th,zz+side*outer],[x,y+lift,zz+side*outer],[x,y+lift,zz-side*inner],[x,y-th,zz-side*inner]])
    return strip_mesh(rows)
def side_skirts(V,F):
    xs=np.linspace(-1.45,1.55,40); zo,ys=_side_profile(V,F,xs)
    pz=np.polyfit(xs,zo,4); py=np.polyfit(xs,ys,3); zo=np.polyval(pz,xs); ys=np.polyval(py,xs)
    a=_blade_along_x(xs,zo+0.004,ys,1); b=_blade_along_x(xs,zo+0.004,ys,-1)
    return np.vstack([a[0],b[0]]),np.vstack([a[1],b[1]+len(a[0])])
def rear_splitters(V,F):
    xs=np.linspace(2.95,3.62,24); zo,ys=_side_profile(V,F,xs)
    pz=np.polyfit(xs,zo,3); py=np.polyfit(xs,ys,2); zo=np.polyval(pz,xs); ys=np.polyval(py,xs)
    a=_blade_along_x(xs,zo+0.004,ys,1,inner=0.12,outer=0.04,th=0.035); b=_blade_along_x(xs,zo+0.004,ys,-1,inner=0.12,outer=0.04,th=0.035)
    return np.vstack([a[0],b[0]]),np.vstack([a[1],b[1]+len(a[0])])
def roof_extension(V,F, half=0.70, n=40):
    us=[];xs=[];ys=[]
    for z in np.arange(ZC-0.72,ZC+0.73,0.05):
        p=slice_mesh(V,F,2,z); p=p[(p[:,1]>1.9)&(p[:,0]>2.4)]
        if len(p): k=p[:,0].argmax(); us.append(z-ZC); xs.append(p[k,0]); ys.append(p[k,1])
    us,xs,ys=map(np.array,(us,xs,ys)); fx=even_fit(us,xs,2); fy=even_fit(us,ys,2)
    rows=[]
    for u in np.linspace(-half,half,n):
        x=float(fx(u)); y=float(fy(u)); z=ZC+u
        rows.append([[x-0.004,y-0.004,z],[x+0.09,y-0.016,z],[x+0.09,y-0.030,z],[x-0.004,y-0.020,z]])
    V2,F2=strip_mesh(rows)
    # side end plates
    out=list(V2); FF=list(F2)
    for u in (-half,half):
        x=float(fx(u)); y=float(fy(u)); z=ZC+u; t=0.006
        q=[[x-0.004,y-0.004,z-t],[x+0.095,y-0.012,z-t],[x+0.095,y-0.06,z-t],[x-0.004,y-0.035,z-t],[x-0.004,y-0.004,z+t],[x+0.095,y-0.012,z+t],[x+0.095,y-0.06,z+t],[x-0.004,y-0.035,z+t]]
        b=len(out); out+=q
        for fc in ([0,1,2],[0,2,3],[4,6,5],[4,7,6],[0,3,7],[0,7,4],[1,5,6],[1,6,2],[0,4,5],[0,5,1]): FF.append([b+i for i in fc])
    return np.array(out,np.float32),np.array(FF)


