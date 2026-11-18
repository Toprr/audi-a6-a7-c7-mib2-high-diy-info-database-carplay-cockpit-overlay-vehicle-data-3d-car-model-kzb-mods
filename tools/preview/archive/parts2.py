# add-on parts, analytic and smooth (symmetric around ZC)
import numpy as np, cv2
ZC=-0.335
def strip_mesh(rows):
    V=[];F=[]; m=len(rows[0])
    for r in rows: V+=list(r)
    for i in range(len(rows)-1):
        for j in range(m):
            a=i*m+j; b=i*m+(j+1)%m; c=(i+1)*m+(j+1)%m; d=(i+1)*m+j
            F+=[[a,b,c],[a,c,d]]
    for i in (0,len(rows)-1):
        for j in range(1,m-1): F.append([i*m,i*m+j,i*m+j+1])
    return np.array(V,np.float32),np.array(F)
def front_lip(lower, half=1.05, n=60):
    # outline: straight at the front at x=-3.585, corners pulled back (ellipse)
    rows=[]
    for u in np.linspace(-1,1,n):
        z=ZC+u*half; e=abs(u)
        xo=-3.585+0.30*e**6; y=0.300-lower+0.06*e**6
        rows.append([[xo,y-0.022,z],[xo,y+0.004,z],[xo+0.25,y+0.004,z],[xo+0.25,y-0.010,z]])
    V,F=strip_mesh(rows)
    # fins under the lip (aftermarket-style flaps)
    fins=[];FF=[]
    for u in (-0.6,-0.2,0.2,0.6):
        z=ZC+u*half; x=-3.56; y=0.300-lower-0.022; t=0.008
        q=[[x,y,z-t],[x+0.12,y,z-t],[x+0.10,y-0.025,z-t],[x,y-0.03,z-t],[x,y,z+t],[x+0.12,y,z+t],[x+0.10,y-0.025,z+t],[x,y-0.03,z+t]]
        b=len(V)+len(fins); fins+=q
        for f in ([0,1,2],[0,2,3],[4,6,5],[4,7,6],[0,3,7],[0,7,4]): FF.append([b+i for i in f])
    return np.vstack([V,np.array(fins,np.float32)]),np.vstack([F,np.array(FF)])
def side_skirt(lower, side, x0=-1.55, x1=1.62, n=24):
    zo=ZC+side*1.375; rows=[]
    for x in np.linspace(x0,x1,n):
        y=0.285-lower
        rows.append([[x,y-0.035,zo+side*0.04],[x,y+0.01,zo+side*0.005],[x,y+0.01,zo-side*0.10],[x,y-0.035,zo-side*0.10]])
    return strip_mesh(rows)
def roof_spoiler(n=40, half=0.74):
    rows=[]
    for u in np.linspace(-1,1,n):
        z=ZC+u*half; e=abs(u)
        x=2.975-0.11*e**4; y=2.045-0.04*e**4
        rows.append([[x-0.06,y+0.012,z],[x+0.13,y-0.015,z],[x+0.13,y-0.040,z],[x-0.06,y-0.005,z]])
    return strip_mesh(rows)
def diffuser(lower):
    # black diffuser band + 5 fins
    y=0.42-lower; xr=3.80; rows=[]
    for u in np.linspace(-1,1,30):
        z=ZC+u*0.82; rows.append([[xr-0.30,y,z],[xr+0.03,y,z],[xr+0.03,y-0.05,z],[xr-0.30,y-0.02,z]])
    V,F=strip_mesh(rows); out=list(V); FF=list(F)
    for dz in (-0.44,-0.22,0.0,0.22,0.44):
        z=ZC+dz; t=0.010
        q=[[xr-0.28,y-0.02,z-t],[xr+0.02,y-0.05,z-t],[xr-0.02,y-0.13,z-t],[xr-0.24,y-0.05,z-t],
           [xr-0.28,y-0.02,z+t],[xr+0.02,y-0.05,z+t],[xr-0.02,y-0.13,z+t],[xr-0.24,y-0.05,z+t]]
        b=len(out); out+=q
        for f in ([0,1,2],[0,2,3],[4,6,5],[4,7,6],[1,5,6],[1,6,2],[0,3,7],[0,7,4],[3,2,6],[3,6,7]): FF.append([b+i for i in f])
    return np.array(out,np.float32),np.array(FF)
def rear_text(text, zc, yc, width, sx, dx=0.01, thick=2, font=cv2.FONT_HERSHEY_SIMPLEX):
    img=np.zeros((70,420),np.uint8); cv2.putText(img,text,(5,50),font,1.7,255,thick,cv2.LINE_AA)
    ys,xs=np.where(img>128); x0,x1=xs.min(),xs.max(); y0,y1=ys.min(),ys.max(); s=width/(x1-x0+1)
    V=[];F=[]
    for x,y in zip(xs,ys):
        z=zc+width/2-(x-x0)*s; yy=yc+((y1+y0)/2-y)*s
        q=[(z,yy),(z-s,yy),(z-s,yy-s),(z,yy-s)]; b=len(V)
        V+=[[sx(p[0],p[1])+dx,p[1],p[0]] for p in q]; F+=[[b,b+1,b+2],[b,b+2,b+3]]
    return np.array(V,np.float32),np.array(F)
