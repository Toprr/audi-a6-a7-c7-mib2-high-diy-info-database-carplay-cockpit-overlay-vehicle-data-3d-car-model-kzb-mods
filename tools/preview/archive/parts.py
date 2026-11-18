# Extra parts for the preview: grille, quattro badge, ACC sensor
import numpy as np, cv2
GRILL_POLY=np.array([(-0.92,1.035),(0.26,1.035),(0.33,0.96),(0.17,0.50),(-0.81,0.50),(-0.98,0.96)],np.float32)
def inside(poly,pts):
    from matplotlib.path import Path
    return Path(poly).contains_points(pts)
def surf_sampler(V,F,mask):
    c=V[F[mask]].mean(1)
    def sx(z,y):
        d=(c[:,2]-z)**2+(c[:,1]-y)**2; return c[d.argmin(),0]
    return sx
def quads_to_mesh(quads):
    V=[];Fs=[]
    for q in quads:
        b=len(V); V+=list(q); Fs+=[[b,b+1,b+2],[b,b+2,b+3]]
    return np.array(V,np.float32),np.array(Fs)
def seg_quad(p0,p1,w):
    d=np.array(p1)-np.array(p0); n=np.array([-d[1],d[0]]); n=n/np.linalg.norm(n)*w/2
    return [p0+n,p1+n,p1-n,p0-n]
def honeycomb(sx, cell=0.065, w=0.010, dx=-0.008):
    from matplotlib.path import Path
    P=Path(GRILL_POLY); quads=[]
    R=cell/np.sqrt(3)  # hex radius (flat-top? use pointy-top)
    for row in range(-2,30):
        for col in range(-2,40):
            zc=-1.05+col*cell+(row%2)*cell/2; yc=0.47+row*R*1.5
            pts=[(zc+R*np.cos(np.radians(30+60*k)), yc+R*np.sin(np.radians(30+60*k))) for k in range(6)]
            for k in range(6):
                a,b=np.array(pts[k]),np.array(pts[(k+1)%6])
                if P.contains_point(a) and P.contains_point(b):
                    q=seg_quad(a,b,w); quads.append([[sx(p[0],p[1])+dx,p[1],p[0]] for p in q])
    return quads_to_mesh(quads)
def text_mesh(text, zc, yc, width, sx, dx=-0.012, scale_px=4):
    img=np.zeros((60,400),np.uint8)
    cv2.putText(img,text,(5,45),cv2.FONT_HERSHEY_SIMPLEX,1.6,255,2,cv2.LINE_AA)
    ys,xs=np.where(img>128); x0,x1=xs.min(),xs.max(); y0,y1=ys.min(),ys.max()
    s=width/(x1-x0+1); quads=[]
    for x,y in zip(xs,ys):
        z=zc-width/2+(x-x0)*s; yy=yc+((y1+y0)/2-y)*s
        q=[(z,yy),(z+s,yy),(z+s,yy-s),(z,yy-s)]
        quads.append([[sx(p[0],p[1])+dx,p[1],p[0]] for p in q])
    return quads_to_mesh(quads)
def disc(zc,yc,r,x,seg=24):
    A=np.linspace(0,2*np.pi,seg,endpoint=False)
    V=[[x,yc,zc]]+[[x,yc+r*np.sin(a),zc+r*np.cos(a)] for a in A]
    F=[[0,1+k,1+(k+1)%seg] for k in range(seg)]
    return np.array(V,np.float32),np.array(F)


def surf_sampler2(V,F,mask):
    c=V[F[mask]].mean(1)
    def sx(z,y):
        d=(c[:,2]-z)**2+(c[:,1]-y)**2; k=d.argmin(); return c[k,0], np.sqrt(d[k])
    return sx
def rs_honeycomb(sx2, W=0.095, H=0.036, w=0.007, dx=-0.008, maxd=0.012, poly=None):
    """RS-Waben: breit gezogene Sechsecke, Spitzen links/rechts, versetzte Spalten"""
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
