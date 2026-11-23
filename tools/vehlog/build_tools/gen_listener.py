# Writes a Java class with empty bodies for every method of a listener interface,
# from javap output, except the methods named in KEEP (written by hand elsewhere).
import sys, re
iface, cls, keepfile, out = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
javap = sys.stdin.read().splitlines()
keep = open(keepfile, encoding="utf-8").read()
body = []
for line in javap:
    m = re.match(r"\s*public abstract (\S+) (\w+)\((.*)\);", line)
    if not m:
        continue
    ret, name, params = m.groups()
    types = [p.strip() for p in params.split(",")] if params.strip() else []
    args = ", ".join("%s a%d" % (t, i) for i, t in enumerate(types))
    sig = "public %s %s(%s)" % (ret, name, args)
    if (" %s(" % name) in keep:
        continue
    ret_stmt = "" if ret == "void" else " return %s;" % ("false" if ret == "boolean" else "0" if ret in ("int","byte","short","long","float","double","char") else "null")
    body.append("    %s {%s }" % (sig, ret_stmt))
pkg, simple = cls.rsplit(".", 1)
src = "package %s;\n\n/** Generated: empty methods of %s, see %s.java for the ones used. */\npublic class %s implements %s {\n%s\n%s}\n" % (
    pkg, iface, simple, simple, iface, keep.rstrip() + "\n", "\n".join(body) + "\n")
open(out, "w", encoding="utf-8", newline="\n").write(src)
print("%s: %d empty methods" % (cls, len(body)))
