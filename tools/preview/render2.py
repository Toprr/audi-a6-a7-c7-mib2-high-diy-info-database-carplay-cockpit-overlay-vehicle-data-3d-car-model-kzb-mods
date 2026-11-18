import sys, glob, numpy as np, cv2, os
PAL=[(180,180,180),(60,60,220),(60,200,60),(220,140,40),(40,200,220),(200,60,200),(230,230,60),(120,80,200),(80,160,255),(255,120,120)]
def load(fn):
    V=[];F=[];G=[];g=-1;names=[]
    for l in open(fn):
        if l.startswith('v '): V.append(list(map(float,l.split()[1:4])))
        elif l.startswith('g '): g+=1; names.append(l.split()[1])
        elif l.startswith('f '): F.append([int(x.split('/')[0])-1 for x in l.split()[1:4]]); G.append(max(g,0))
    return np.array(V,np.float32),np.array(F,np.int64),np.array(G),names
def render(V,F,G,yaw,pitch,W=640,H=400):
    c=(V.max(0)+V.min(0))/2; V=V-c
    a,b=np.radians(yaw),np.radians(pitch)
    Ry=np.array([[np.cos(a),0,np.sin(a)],[0,1,0],[-np.sin(a),0,np.cos(a)]]); Rx=np.array([[1,0,0],[0,np.cos(b),-np.sin(b)],[0,np.sin(b),np.cos(b)]])
    P=V@Ry.T@Rx.T; s=0.9*min(W,H)/np.ptp(P[:,:2],axis=0).max()
    xy=np.stack([P[:,0]*s+W/2,-P[:,1]*s+H/2],1); T=P[F]
    n=np.cross(T[:,1]-T[:,0],T[:,2]-T[:,0]); n/=(np.linalg.norm(n,axis=1,keepdims=True)+1e-9)
    L=np.array([0.3,0.5,0.8]); L/=np.linalg.norm(L); sh=np.abs(n@L)*0.75+0.25
    img=np.full((H,W,3),30,np.uint8); pts=xy[F]
    for i in np.argsort(T[:,:,2].mean(1)):
        col=PAL[G[i]%len(PAL)]
        cv2.fillConvexPoly(img,np.round(pts[i]*4).astype(np.int32),tuple(int(x*sh[i]) for x in col),lineType=cv2.LINE_AA,shift=2)
    return img
if __name__=='__main__':
  out=sys.argv[2]; os.makedirs(out,exist_ok=True)
  for pat in sys.argv[3].split(','):
      for fn in sorted(glob.glob(f"{sys.argv[1]}/{pat}")):
          V,F,G,names=load(fn)
          im=np.hstack([render(V,F,G,y,p) for y,p in [(35,15),(90,0),(-145,15)]])
          cv2.putText(im,os.path.basename(fn),(8,22),0,0.6,(0,255,255),1)
          for k,nm in enumerate(names): cv2.putText(im,nm,(8,44+k*18),0,0.5,PAL[k%len(PAL)],1)
          cv2.imwrite(f"{out}/{os.path.basename(fn)[:-4]}.jpg",im,[cv2.IMWRITE_JPEG_QUALITY,82]); print(os.path.basename(fn))

