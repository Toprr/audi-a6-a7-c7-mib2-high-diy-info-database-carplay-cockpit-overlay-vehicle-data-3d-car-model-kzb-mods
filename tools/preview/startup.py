import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
import sys, numpy as np, cv2
from PIL import Image, ImageDraw, ImageFont

from build_v9 import build
from glrender import render
W,H=800,480; SS=3
sc=build()
def studio(w,h):
    y,x=np.mgrid[0:h,0:w].astype(np.float32)
    r=np.hypot((x-w*0.40)/(w*0.65),(y-h*0.42)/(h*0.75))
    bg=np.clip(78-58*r,10,80)
    floor=y>h*0.70; bg[floor]=np.clip(bg[floor]*0.75,8,70)
    return np.dstack([bg,bg,bg+2]).astype(np.float32)
def car_layer(yaw,pitch,zoom,h_px,fov=28):
    im=render(sc,yaw,pitch,W=1800,H=1200,zoom=zoom,ground=False,alpha=True,fov=fov)
    a=im[:,:,3]; ys,xs=np.where(a>8); im=im[ys.min():ys.max()+1,xs.min():xs.max()+1]
    s=h_px/im.shape[0]; return cv2.resize(im,(int(im.shape[1]*s),h_px),interpolation=cv2.INTER_AREA)
def paste(base,lay,x,y,op=1.0):
    h=min(lay.shape[0],base.shape[0]-y); w=min(lay.shape[1],base.shape[1]-x); lay=lay[:h,:w]; a=lay[:,:,3:4].astype(np.float32)/255*op
    base[y:y+h,x:x+w]=base[y:y+h,x:x+w]*(1-a)+lay[:,:,:3]*a
def startup():
    bg=studio(W*SS,H*SS)
    car=car_layer(97,1.5,1.0,int(225*SS),fov=16)
    cx=int(285*SS-car.shape[1]/2); cy=int(345*SS-car.shape[0])
    refl=cv2.flip(car,0).astype(np.float32); g=np.linspace(0.30,0.0,refl.shape[0])[:,None]; refl[:,:,3]*=g
    paste(bg,refl.astype(np.uint8),cx,cy+car.shape[0]-int(6*SS))
    paste(bg,car,cx,cy)
    img=Image.fromarray(np.clip(bg,0,255).astype(np.uint8)[:,:,::-1]); d=ImageDraw.Draw(img)
    # rings (chrome)
    R=int(27*SS); t=int(5.5*SS); x0=int(560*SS); y0=int(205*SS)
    for k in range(4):
        cxk=x0+k*int(38*SS)
        for j,col in enumerate(((95,95,100),(205,205,210),(150,150,155))):
            rr=R-j*int(1.2*SS); d.ellipse([cxk-rr,y0-rr,cxk+rr,y0+rr],outline=col,width=max(1,t-j*int(1.5*SS)))
    f1=ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf",int(54*SS)); f2=ImageFont.truetype("C:/Windows/Fonts/arial.ttf",int(15*SS))
    mid=x0+int(57*SS)
    d.text((mid,int(262*SS)),"A6",font=f1,fill=(210,210,214),anchor="ma")
    txt1,txt2="Audi"," MMI Navigation plus"; w1=d.textlength(txt1,font=f2); w2=d.textlength(txt2,font=f2)
    xs=mid-(w1+w2)/2; d.text((xs,int(338*SS)),txt1,font=f2,fill=(220,20,30)); d.text((xs+w1,int(338*SS)),txt2,font=f2,fill=(225,225,225))
    out=np.array(img)[:,:,::-1]; return cv2.resize(out,(W,H),interpolation=cv2.INTER_AREA)
def car_icon(size=(120,72)):
    lay=car_layer(108,7,1.0,int(size[1]*4),fov=20); h,w=lay.shape[:2]
    canvas=np.zeros((size[1]*4,size[0]*4,4),np.uint8); s=min(canvas.shape[1]/w,canvas.shape[0]/h)
    lay=cv2.resize(lay,(int(w*s),int(h*s)),interpolation=cv2.INTER_AREA); y=(canvas.shape[0]-lay.shape[0])//2; x=(canvas.shape[1]-lay.shape[1])//2
    canvas[y:y+lay.shape[0],x:x+lay.shape[1]]=lay
    return cv2.resize(canvas,size,interpolation=cv2.INTER_AREA)
if __name__=="__main__":
    st=startup(); cv2.imwrite(sys.argv[1]+r"\startup_800x480.png",st)
    ic=car_icon(); cv2.imwrite(sys.argv[1]+r"\car_icon_120x72.png",ic)
    # menu preview: icon on a black menu crop, enlarged
    m=np.zeros((480,800,3),np.uint8); m[:]=12
    a=ic[:,:,3:4]/255.0; big=cv2.resize(ic,(240,144),interpolation=cv2.INTER_CUBIC); ab=big[:,:,3:4]/255.0
    m[160:304,280:520]=(m[160:304,280:520]*(1-ab)+big[:,:,:3]*ab).astype(np.uint8)
    m[60:132,60:180]=(m[60:132,60:180]*(1-a)+ic[:,:,:3]*a).astype(np.uint8)
    cv2.putText(m,"Car",(100,52),cv2.FONT_HERSHEY_SIMPLEX,0.7,(230,230,230),1,cv2.LINE_AA)
    cv2.putText(m,"Originalgroesse ~120x72",(40,160),cv2.FONT_HERSHEY_SIMPLEX,0.45,(150,150,150),1,cv2.LINE_AA)
    cv2.putText(m,"vergroessert",(350,330),cv2.FONT_HERSHEY_SIMPLEX,0.5,(150,150,150),1,cv2.LINE_AA)
    cv2.imwrite(sys.argv[1]+r"\car_icon_vorschau.png",m)


