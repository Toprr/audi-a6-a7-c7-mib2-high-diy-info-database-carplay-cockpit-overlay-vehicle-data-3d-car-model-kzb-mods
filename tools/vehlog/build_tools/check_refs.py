# Checks that every method/field our classes use on a stock class exists in the
# unit's lsd.jar with the same descriptor and is public (or the owner is ours).
import re, subprocess, sys
javap, ours_dir, lsd = sys.argv[1], sys.argv[2], sys.argv[3]
ours = sys.argv[4:]
refs = set()
for c in ours:
    out = subprocess.run([javap, "-c", "-p", "-cp", ours_dir, c], capture_output=True, text=True, check=True).stdout
    for m in re.finditer(r"// (Method|InterfaceMethod|Field) ([\w/$]+)\.(\"?<?\w+>?\"?):(\S+)", out):
        kind, owner, name, desc = m.groups()
        refs.add((kind, owner, name.strip('"'), desc))
    for m in re.finditer(r"// class ([\w/$]+)", out):
        refs.add(("Class", m.group(1), "", ""))
own = set(c.replace(".", "/") for c in ours)
cache, bad = {}, 0
for kind, owner, name, desc in sorted(refs):
    if owner in own:
        continue
    if owner not in cache:
        r = subprocess.run([javap, "-p", "-s", "-cp", lsd, owner.replace("/", ".")], capture_output=True, text=True)
        cache[owner] = r.stdout if r.returncode == 0 else None
    txt = cache[owner]
    if txt is None:
        print("MISSING CLASS", owner); bad += 1; continue
    if kind == "Class":
        continue
    lines = txt.splitlines()
    found = False
    for i, l in enumerate(lines):
        if i + 1 < len(lines) and lines[i + 1].strip() == "descriptor: " + desc:
            nm = "<init>" if name == "<init>" else name
            if (nm == "<init>" and "(" in l and owner.split("/")[-1].split("$")[-1] + "(" in l.replace(owner.replace("/", "."), owner.split("/")[-1])) or re.search(r"[ .]" + re.escape(nm) + r"[(;]", l):
                found = True
                if "public" not in l:
                    print("NOT PUBLIC", owner, name, desc, "|", l.strip()); bad += 1
                break
    if not found:
        print("MISSING", kind, owner, name, desc); bad += 1
print("checked %d references to stock classes, problems: %d" % (len([r for r in refs if r[1] not in own]), bad))
sys.exit(1 if bad else 0)
