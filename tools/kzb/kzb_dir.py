# Parse the directory of a Kanzi .kzb (big-endian, starts at 0x3c) -> JSON with every file entry.
# usage: python kzb_dir.py content.kzb kzb_dir.json
import struct,sys,json,collections
d=open(sys.argv[1],'rb').read(); N=len(d)
be=lambda o: struct.unpack_from('>I',d,o)[0]; be16=lambda o: struct.unpack_from('>H',d,o)[0]
A=lambda p:(p+3)&~3
files=[]; folders=[]
def folder(o,path):
    size=be(o); L=be16(o+4); name=d[o+6:o+6+L].decode('latin1'); p=A(o+6+L)
    full=path+"/"+name; folders.append(dict(path=full,entry=o,size=size))
    nsub=be(p); p+=4
    for _ in range(nsub): p=folder(p,full)
    nf=be(p); p+=4
    for _ in range(nf):
        off,sz,typ,fl=struct.unpack_from('>IIII',d,p); L=be16(p+16); nm=d[p+18:p+18+L].decode('latin1')
        assert off+sz<=N, (full,nm,hex(p))
        files.append(dict(path=full+"/"+nm,off=off,size=sz,type=typ,flags=fl,entry=p)); p=A(p+18+L)
    return p
p=0x3c; n=be(p); p+=4
for _ in range(n): p=folder(p,"")
print("directory end",hex(p),"folders",len(folders),"files",len(files))
fs=sorted(files,key=lambda f:f['off']); gaps=sum(1 for a,b in zip(fs,fs[1:]) if b['off']<a['off']+a['size'])
print("overlaps",gaps,"data",hex(fs[0]['off']),"to",hex(fs[-1]['off']+fs[-1]['size']),"file",hex(N))
json.dump(dict(files=files,folders=folders,dir_end=p),open(sys.argv[2],"w"))
print(collections.Counter(f['path'].split('/')[1] for f in files).most_common(20))
print(collections.Counter((f['type']) for f in files).most_common(25))

