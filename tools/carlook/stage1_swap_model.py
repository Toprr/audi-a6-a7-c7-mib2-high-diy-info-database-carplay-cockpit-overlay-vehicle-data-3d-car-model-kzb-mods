# Stage 1: the A6 Avant menu scenes show the RS6 Avant geometry + silver-grey paint. In-place edits only.
# usage: python stage1_swap_model.py content.kzb content_stage1.kzb kzb_dir.json
import json,struct,hashlib,sys
src,dst=sys.argv[1],sys.argv[2]
d=bytearray(open(src,'rb').read()); orig=bytes(d)
D=json.load(open(sys.argv[3])); F=D['files']; P={f['path']:(i+1,f) for i,f in enumerate(F)}
def lastref_pos(f):
    n0=struct.unpack_from('<I',orig,f['off'])[0]; return f['off']+f['size']+4+n0-4
MAP={'AU572_A6_PA_Polymodel':'Exterior','AU572_A6_PA_Mirrormodel':'Mirror','AU572_A6_PA_Interior_left_grp':'Interior_left','AU572_A6_PA_Interior_right':'Interior_right'}
A6={P['/Scenes/AU572_Avant_PA_Geometry/Root Node/'+k][0]:k for k in MAP}
RS={k:P['/Scenes/AU572_RS6_Avant_PA_Geometry/Root Node/'+v][0] for k,v in MAP.items()}
changes=[]
for p,(i,f) in P.items():
    if f['type']!=170 or not p.startswith('/Scenes/AU572_Avant_PA_'): continue
    pos=lastref_pos(f); r=struct.unpack_from('<I',orig,pos)[0]
    if r in A6:
        new=RS[A6[r]]; struct.pack_into('<I',d,pos,new); changes.append((p,pos,r,new))
assert len(changes)==12, len(changes)
# paint: RS6 paint material, Diffuse (property 0x63) -> silver grey
i,f=P['/Materials/PhongTexturedCube/AU572_RS6_PA_Paint']; b=orig[f['off']:f['off']+f['size']]
k=b.find(struct.pack('<III',0x20,0x63,2)); assert k==0x4a, hex(k)
pos=f['off']+k+16; old=struct.unpack_from('<3f',orig,pos)
NEW=(0.74,0.755,0.775)
struct.pack_into('<3f',d,pos,*NEW); changes.append(('RS6_PA_Paint Diffuse',pos,old,NEW))
open(dst,'wb').write(d)
for c in changes: print(c)
print("Original SHA256",hashlib.sha256(orig).hexdigest()); print("New      SHA256",hashlib.sha256(d).hexdigest())

