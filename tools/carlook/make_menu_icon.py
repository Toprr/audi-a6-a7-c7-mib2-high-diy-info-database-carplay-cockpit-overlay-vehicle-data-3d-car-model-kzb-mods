import os as _os, sys as _sys
WORK = _os.environ.get("MIB_WORK", "work")  # folder with content.kzb, kzb_dir.json and intermediate files
_HERE = _os.path.dirname(_os.path.abspath(__file__))
for _p in (_HERE, _os.path.join(_HERE, "..", "kzb"), _os.path.join(_HERE, "..", "preview")): _sys.path.insert(0, _p)
# Render the modified car (preview builder, tools/preview/build_v9.py) into the 65x48 main-menu icons.
# Output: WORK/icon_enabled.png, WORK/icon_highlighted.png, WORK/icon_compare.png (originals vs new, 5x)
import sys, numpy as np, cv2

from build_v9 import build
from glrender import render
sc=build()
im=render(sc,90,4,W=1600,H=1200,ground=False,alpha=True,fov=12)
ys,xs=np.where(im[:,:,3]>8); im=im[ys.min():ys.max()+1,xs.min():xs.max()+1]
import json as _json
_kzb=open(WORK+"/content.kzb","rb").read(); _F=_json.load(open(WORK+"/kzb_dir.json"))["files"]
def kzb_png(path):  # read an original PNG straight from content.kzb (entry: 4 zero bytes + PNG)
    f=[x for x in _F if x["path"]==path][0]; return cv2.imdecode(np.frombuffer(_kzb[f["off"]+4:f["off"]+4+f["size"]],np.uint8),cv2.IMREAD_UNCHANGED)
ICON="/Resource Files/Images/WizardIcons/Main Wizard/2D/d4_wizard_maingroups_car_"
orig=kzb_png(ICON+"enabled.png")
oy,ox=np.where(orig[:,:,3]>8); ow,oh=ox.max()-ox.min()+1,oy.max()-oy.min()+1; print("original image area",ow,oh,"margin",ox.min(),oy.min())
s=ow/im.shape[1]; s=min(s,oh/im.shape[0]*1.0); car=cv2.resize(im,(max(1,int(im.shape[1]*s)),max(1,int(im.shape[0]*s))),interpolation=cv2.INTER_AREA)
can=np.zeros_like(orig); x0=ox.min()+(ow-car.shape[1])//2; y0=oy.min()+(oh-car.shape[0])
can[y0:y0+car.shape[0],x0:x0+car.shape[1]]=car
# brighten like the original (white icon car), more contrast
rgb=can[:,:,:3].astype(np.float32); rgb=np.clip((rgb-15)*1.35+10,0,255); can[:,:,:3]=rgb.astype(np.uint8)
cv2.imwrite(WORK+"/icon_enabled.png",can)
# highlighted state: tint dark areas (glass, grille, lights) red like the original
hl=can.copy().astype(np.float32); lum=hl[:,:,:3].mean(2,keepdims=True)
dark=np.clip((85-lum)/55,0,1)
red=np.concatenate([lum*0.15,lum*0.15,np.clip(lum*1.6+60,0,255)],2)
hl[:,:,:3]=hl[:,:,:3]*(1-dark)+red*dark
cv2.imwrite(WORK+"/icon_highlighted.png",hl.astype(np.uint8))
T=[]
for img in (orig,kzb_png(ICON+"highlighted.png"),can,hl.astype(np.uint8)):
    bg=np.full(img.shape[:2]+(3,),20,np.uint8); al=img[:,:,3:4]/255.0; v=(img[:,:,:3]*al+bg*(1-al)).astype(np.uint8)
    T.append(cv2.resize(v,None,fx=5,fy=5,interpolation=cv2.INTER_NEAREST))
cv2.imwrite(WORK+"/icon_compare.png",np.vstack([np.hstack(T[:2]),np.hstack(T[2:])]))

