# Extract every mesh of a Kanzi .kzb to OBJ files (+ meshes.json with offsets, materials, bounding boxes).
# usage: python kzb_extract.py content.kzb out_folder
import sys, struct, re, os, json
import numpy as np
d=open(sys.argv[1],'rb').read(); out=sys.argv[2]; os.makedirs(out,exist_ok=True)
u32=lambda o: struct.unpack_from('<I',d,o)[0]; u16=lambda o: struct.unpack_from('<H',d,o)[0]
TS={6:2,7:4,0:1,1:1,2:2,3:2,4:4,5:4}
def attrs(cnt_off):
    n=u32(cnt_off); o=cnt_off+4; at=[]
    for i in range(n):
        L=u16(o)
        if not 1<=L<=200: return None
        nm=d[o+2:o+2+L]
        if not all(32<=c<127 for c in nm): return None
        p=o+2+L
        for pad in range(4):
            s,t,m=struct.unpack_from('<III',d,p+pad)
            if s<=12 and t in TS and 1<=m<=4: p+=pad; break
        else: return None
        at.append((nm.decode(),s,t,m)); o=p+12
    return at,o
meshes=[]; seen=set()
for m in re.finditer(rb'[\x20-\x7e]{3,200}', d):
    st=m.start()
    # the name may start inside a printable run; try the run start plus small offsets
    for st2 in (st, st+1, st+2):
        if st2-6<0: continue
        L=u16(st2-2); n=u32(st2-6)
        if not (1<=n<=8 and L>=3 and st2+L<=m.end()): continue
        r=attrs(st2-6)
        if not r: continue
        at,o=r
        if at[0][1]!=0 or at[0][3]!=3: continue
        if (st2-6) in seen: continue
        nv=u32(o); stride=sum(TS[a[2]]*a[3] for a in at)
        if not 3<=nv<=200000: continue
        vs=o+4; ve=vs+nv*stride
        nsub=u32(ve)
        if not 1<=nsub<=64: continue
        p=ve+4; subs=[]; okk=True
        for k in range(nsub):
            mat,cnt,isz=u32(p),u32(p+4),u32(p+8)
            if isz not in (2,4) or cnt%3 or cnt>3_000_000: okk=False;break
            idx=np.frombuffer(d[p+12:p+12+cnt*isz],dtype='<u2' if isz==2 else '<u4')
            if len(idx) and idx.max()>=nv: okk=False;break
            subs.append((mat,idx)); p=p+12+cnt*isz; p+= (cnt*isz)%4
        if not okk: continue
        seen.add(st2-6)
        # decode positions (first attribute), normals, uv
        dt=[]; 
        for a in at: dt.append((a[0], {6:'<f2',7:'<f4',0:'i1',1:'u1',2:'<i2',3:'<u2',4:'<i4',5:'<u4'}[a[2]], (a[3],)))
        arr=np.frombuffer(d[vs:ve], dtype=np.dtype([(f"a{i}",x[1],x[2]) for i,x in enumerate(dt)]))
        pos=arr['a0'].astype(np.float32)
        uvi=[i for i,a in enumerate(at) if a[1]==3]; nri=[i for i,a in enumerate(at) if a[1]==1]
        name=re.sub(r'(Shape)?-?(positions|Position)$','',at[0][0]); safe=re.sub(r'[^\w\-]','_',name)
        fn=f"{out}/{len(meshes):03d}_{safe}.obj"
        with open(fn,'w') as f:
            f.write(f"o {safe}\n")
            np.savetxt(f,pos,fmt='v %.5f %.5f %.5f')
            if uvi: np.savetxt(f,arr[f"a{uvi[0]}"].astype(np.float32),fmt='vt %.5f %.5f')
            if nri: np.savetxt(f,arr[f"a{nri[0]}"].astype(np.float32),fmt='vn %.4f %.4f %.4f')
            for mat,idx in subs:
                f.write(f"g mat_{mat}\n")
                t=idx.reshape(-1,3)+1
                if uvi and nri: np.savetxt(f,np.repeat(t,3,axis=1),fmt='f %d/%d/%d %d/%d/%d %d/%d/%d')
                else: np.savetxt(f,t,fmt='f %d %d %d')
        meshes.append(dict(i=len(meshes),name=name,off=st2-6,nv=int(nv),tris=int(sum(len(x[1]) for x in subs)//3),mats=[int(x[0]) for x in subs],
                           bmin=pos.min(0).round(2).tolist(),bmax=pos.max(0).round(2).tolist(),attrs=[a[0].rsplit('-',1)[-1] for a in at]))
        break
json.dump(meshes,open(f"{out}/meshes.json","w"),indent=1)
print("meshes:",len(meshes), "triangles total:", sum(m['tris'] for m in meshes))
for m in []: print(f"{m['i']:3d} {m['name'][:48]:48s} v={m['nv']:6d} t={m['tris']:6d} {m['bmin']} {m['bmax']}")


