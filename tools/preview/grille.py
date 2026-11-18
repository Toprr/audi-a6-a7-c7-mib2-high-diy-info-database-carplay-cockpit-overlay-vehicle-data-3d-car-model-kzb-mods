# RS-style honeycomb grille mesh, projected onto the body surface.
import numpy as np

def quads_to_mesh(quads):
    V=[];Fs=[]
    for q in quads:
        b=len(V); V+=list(q); Fs+=[[b,b+1,b+2],[b,b+2,b+3]]
    return np.array(V,np.float32),np.array(Fs)
def seg_quad(p0,p1,w):
    d=np.array(p1)-np.array(p0); n=np.array([-d[1],d[0]]); n=n/np.linalg.norm(n)*w/2
    return [p0+n,p1+n,p1-n,p0-n]
def surf_sampler2(V,F,mask):
    c=V[F[mask]].mean(1)
    def sx(z,y):
        d=(c[:,2]-z)**2+(c[:,1]-y)**2; k=d.argmin(); return c[k,0], np.sqrt(d[k])
    return sx
def rs_honeycomb(sx2, W=0.095, H=0.036, w=0.007, dx=-0.008, maxd=0.012, poly=None):
    """RS honeycomb: wide hexagons, points left/right, staggered columns"""
    from matplotlib.path import Path
    P=Path(poly) if poly is not None else None
    shape=[(-W/2,0),(-W/4,H/2),(W/4,H/2),(W/2,0),(W/4,-H/2),(-W/4,-H/2)]
    quads=[]; seen=set()
    for col in range(-2,30):
        for row in range(-2,40):
            zc=-1.10+col*0.75*W; yc=0.40+row*H+(col%2)*H/2
            pts=[(zc+a,yc+b) for a,b in shape]
            for k in range(6):
                a=np.array(pts[k]); b=np.array(pts[(k+1)%6])
                key=tuple(np.round(np.r_[a,b],4)); key2=tuple(np.round(np.r_[b,a],4))
                if key in seen or key2 in seen: continue
                seen.add(key)
                ok=True; samples=[]
                for t in np.linspace(0,1,5):
                    p=a+(b-a)*t; x,d=sx2(p[0],p[1])
                    if d>maxd or (P is not None and not P.contains_point(p)): ok=False;break
                    samples.append(x)
                if not ok: continue
                q=seg_quad(a,b,w); xs=[sx2(p[0],p[1])[0] for p in q]
                quads.append([[xs[i]+dx,q[i][1],q[i][0]] for i in range(4)])
    return quads_to_mesh(quads)
