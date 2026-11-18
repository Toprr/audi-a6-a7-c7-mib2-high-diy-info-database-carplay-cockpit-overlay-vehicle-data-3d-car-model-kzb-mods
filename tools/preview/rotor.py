# custom 5-arm 'rotor' style rim (modelled from a photo): narrow spoke, flared on one side at the rim, chamfer on one edge
import numpy as np
def smooth(a,b,x): t=np.clip((x-a)/(b-a),0,1); return t*t*(3-2*t)
def rotor_wheel(c, zo, side, R=0.405, spokes=5, rot=0.0):
    V=[];F=[];K=[]
    def add(vs,fs,k):
        b=len(V); V.extend(vs); F.extend([[b+i for i in f] for f in fs]); K.extend([k]*len(fs))
    def P(r,a,depth): return [c[0]+r*np.cos(a), c[1]+r*np.sin(a), zo-side*depth]
    n=72; A=np.linspace(0,2*np.pi,n,endpoint=False)
    Rin=R-0.028
    for k in range(n):  # rim flange + well
        a0,a1=A[k],A[(k+1)%n]
        add([P(R,a0,0.0),P(R,a1,0.0),P(Rin,a1,0.006),P(Rin,a0,0.006)],[[0,1,2],[0,2,3]],2)
        add([P(Rin,a0,0.006),P(Rin,a1,0.006),P(Rin,a1,0.17),P(Rin,a0,0.17)],[[0,1,2],[0,2,3]],1)
    r0=0.07; rs=np.linspace(r0,Rin+0.004,16)
    for s in range(spokes):
        base=np.radians(90+s*360/spokes)
        prev=None
        for r in rs:
            t=min((r-r0)/(Rin-r0),1.0)
            wl=0.050+0.022*(1-t)**2+0.120*smooth(0.50,1.0,t)   # one side strongly flared
            wr=0.050+0.022*(1-t)**2+0.050*smooth(0.70,1.0,t)
            al=base+side*wl/r; ar=base-side*wr/r
            d=0.030*(1-t)**1.6+0.006
            bev=0.35                                            # share of the chamfer
            am=ar+(al-ar)*bev
            row=(r,al,am,ar,d)
            if prev:
                (q,pl,pm,pr,pd)=prev
                add([P(q,pl,pd),P(r,al,d),P(r,am,d),P(q,pm,pd)],[[0,1,2],[0,2,3]],0)          # main face
                add([P(q,pm,pd),P(r,am,d),P(r,ar,d+0.03),P(q,pr,pd+0.03)],[[0,1,2],[0,2,3]],1) # chamfer
                for (x0,x1,dd) in ((pl,al,0),(pr,ar,0.03)):
                    add([P(q,x0,pd+dd),P(r,x1,d+dd),P(r,x1,d+0.045),P(q,x0,pd+0.045)],[[0,1,2],[0,2,3]],1)
            prev=row
    for k in range(n):  # hub / cap
        a0,a1=A[k],A[(k+1)%n]
        add([P(0.075,a0,0.012),P(0.075,a1,0.012),P(0.04,a1,0.010),P(0.04,a0,0.010)],[[0,1,2],[0,2,3]],3)
        add([P(0,0,0.008),P(0.04,a0,0.010),P(0.04,a1,0.010)],[[0,1,2]],2)
    for s in range(5):  # wheel bolts between the spokes
        a=np.radians(90+36+s*72); cx=c[0]+0.10*np.cos(a); cy=c[1]+0.10*np.sin(a)
        for k in range(12):
            b0,b1=2*np.pi*k/12,2*np.pi*(k+1)/12
            add([[cx,cy,zo-side*0.012],[cx+0.013*np.cos(b0),cy+0.013*np.sin(b0),zo-side*0.02],[cx+0.013*np.cos(b1),cy+0.013*np.sin(b1),zo-side*0.02]],[[0,1,2]],2)
    return np.array(V,np.float32),np.array(F),np.array(K)


