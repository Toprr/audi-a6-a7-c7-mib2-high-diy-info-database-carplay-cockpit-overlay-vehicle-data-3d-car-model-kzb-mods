# Dump every embedded PNG of a .kzb.  usage: python kzbpng.py content.kzb out_folder
import sys, os, re, struct
src, out = sys.argv[1], sys.argv[2]; os.makedirs(out, exist_ok=True)
d = open(src,'rb').read()
sig = b'\x89PNG\r\n\x1a\n'; n=0; pos=0; sizes=[]
while True:
    i = d.find(sig, pos)
    if i < 0: break
    e = d.find(b'IEND', i)
    if e < 0: break
    png = d[i:e+8]
    w,h = struct.unpack('>II', png[16:24])
    open(f"{out}/png_{n:04d}_{w}x{h}.png",'wb').write(png); sizes.append((w,h)); n+=1; pos=e+8
print("PNG:", n)
for k in ['DDS ', 'KTX', 'PVR']:
    print(k, d.count(k.encode()))
big = sorted(sizes, key=lambda x:-x[0]*x[1])[:15]; print("largest:", big)
