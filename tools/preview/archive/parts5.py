# diffuser from a photo: edge under the paint over the full width + 4 large fins
import numpy as np
from slicer import slice_mesh
from parts2 import strip_mesh
ZC=-0.335
def _rear_edge(V,F,z,ymax):
    p=slice_mesh(V,F,2,z); p=p[(p[:,0]>3.3)&(p[:,1]<ymax)]
    return p
def diffuser_v8(V,Fall,Fpaint):
    # 1) edge right below the paint (lower paint edge y~0.66-lower)
    rows=[]
    for u in np.linspace(-0.92,0.92,60):
        z=ZC+u; p=_rear_edge(V,Fpaint,z,0.8)
        y0=p[:,1].min(); x=p[p[:,1]<y0+0.02][:,0].max()
        rows.append([[x-0.06,y0+0.004,z],[x+0.035,y0+0.004,z],[x+0.035,y0-0.028,z],[x-0.06,y0-0.04,z]])
    V1,F1=strip_mesh(rows)
    out=list(V1); FF=list(F1)
    # 2) 4 fins at the positions from the photo (z = ZC +-0.17, +-0.47)
    for u in (-0.47,-0.17,0.17,0.47):
        z=ZC+u; p=_rear_edge(V,Fall,z,0.7); p=p[p[:,0]>3.45]
        yt=p[:,1].min()+0.01; xr=min(p[:,0].max(),3.84)
        t=0.016; h=0.14; L=0.30
        prof=[[xr+0.01,yt],[xr-0.015,yt-h],[xr-0.06,yt-h+0.01],[xr-L,yt]]   # tall at the back, pointed at the bottom, fading out to the front
        b=len(out)
        for side in (-t,t): out+=[[px,py,z+side] for px,py in prof]
        for fc in ([0,1,2],[0,2,3],[4,6,5],[4,7,6],[0,4,5],[0,5,1],[1,5,6],[1,6,2],[2,6,7],[2,7,3],[3,7,4],[3,4,0]): FF.append([b+i for i in fc])
    return np.array(out,np.float32),np.array(FF)

