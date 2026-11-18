# kzbmesh: read/write a mesh entry (type 500) of a Kanzi .kzb. Padding may be relative to the ABSOLUTE
# position in the .kzb (base) - parse_auto() tries both and only accepts a byte-exact round trip.
import struct
TS={6:2,7:4,0:1,1:1,2:2,3:2,4:4,5:4}
def parse(b,base):
    n0=struct.unpack_from('<I',b,0)[0]; h=dict(n0=n0,var=b[4:4+n0],z=struct.unpack_from('<I',b,4+n0)[0]); p=8+n0
    n=struct.unpack_from('<I',b,p)[0]; p+=4; attrs=[]
    for _ in range(n):
        L=struct.unpack_from('<H',b,p)[0]; nm=b[p+2:p+2+L]; q=p+2+L; pad=(-(2+L))%4
        s,t,m=struct.unpack_from('<III',b,q+pad)
        if not (s<=12 and t in TS and 1<=m<=4): raise ValueError("attr %s"%nm)
        attrs.append(dict(name=nm,padb=b[q:q+pad],sem=s,typ=t,dim=m)); p=q+pad+12
    nv=struct.unpack_from('<I',b,p)[0]; p+=4; stride=sum(TS[a['typ']]*a['dim'] for a in attrs)
    verts=b[p:p+nv*stride]; p+=nv*stride
    ns=struct.unpack_from('<I',b,p)[0]; p+=4; subs=[]
    for _ in range(ns):
        mat,cnt,isz=struct.unpack_from('<III',b,p); p+=12; idx=b[p:p+cnt*isz]; p+=cnt*isz
        padn=(-(cnt*isz))%4; pad=b[p:p+padn]; p+=padn
        subs.append(dict(mat=mat,cnt=cnt,isz=isz,idx=idx,padb=pad))
    return dict(h=h,attrs=attrs,nv=nv,stride=stride,verts=verts,subs=subs,tail=b[p:])
def build(m,base):
    o=bytearray(struct.pack('<I',m['h']['n0'])+m['h']['var']+struct.pack('<I',m['h']['z'])); o+=struct.pack('<I',len(m['attrs']))
    for a in m['attrs']:
        o+=struct.pack('<H',len(a['name']))+a['name']; pad=(-(2+len(a['name'])))%4
        o+=(a['padb'] if len(a['padb'])==pad else b'\0'*pad)+struct.pack('<III',a['sem'],a['typ'],a['dim'])
    o+=struct.pack('<I',m['nv'])+m['verts']+struct.pack('<I',len(m['subs']))
    for s in m['subs']:
        o+=struct.pack('<III',s['mat'],s['cnt'],s['isz'])+s['idx']; pad=(-(s['cnt']*s['isz']))%4
        o+=(s['padb'] if len(s['padb'])==pad else b'\0'*pad)
    return bytes(o+m['tail'])

def parse_auto(b,base):
    """tries both padding references; returns the mesh only if rebuilding it is byte-identical"""
    for ref in (base,0):
        try:
            m=parse(b,ref)
            if build(m,ref)==b: m['ref']=ref; return m
        except Exception: pass
    raise ValueError("no exact round trip")


