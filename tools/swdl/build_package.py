#!/usr/bin/env python3
"""
Build an SD-card package that installs a modified content.kzb through the MMI's own
software update (red engineering menu -> Update -> SD).

Two layouts:
  --cluster-map DIR   (CAR-TESTED) DIR is an unpacked mib-cluster-map release
                      (https://github.com/MacsInSpace/mib-cluster-map). Its files are copied
                      unchanged, the CarLook component is appended to its metainfo2.txt and one
                      FinalScript runs first the cluster-map installer, then CarLook.
  (no --cluster-map)  standalone CarLook package (same metainfo pattern, but this exact layout
                      was NOT tested in a car - test with care).

Usage:
  python build_package.py --orig content.kzb.original --new content_modified.kzb \
      --sha1sum path/to/M.I.B./apps/sbin/sha1sum --train MHI2_ER_AU57x_K3663_1 --out SD_PACKAGE \
      [--cluster-map unpacked/mib-cluster-map_v1.2.6] [--name "My CarLook"]

Uninstall: move CarLook/to_uninstall_move_this_file_up/UNINSTALL.txt one level up (into CarLook/)
and run the update again -> the original from CarLook/backup/ is restored (SHA1-checked).
"""
import argparse, hashlib, os, re, shutil, sys

HERE = os.path.dirname(os.path.abspath(__file__))
def sha1(p, n=None):
    with open(p, "rb") as f:
        return hashlib.sha1(f.read() if n is None else f.read(n)).hexdigest()

ap = argparse.ArgumentParser()
ap.add_argument("--orig", required=True)
ap.add_argument("--new", required=True)
ap.add_argument("--sha1sum", required=True, help="QNX/ARM sha1sum binary, e.g. from M.I.B. apps/sbin/sha1sum")
ap.add_argument("--train", required=True)
ap.add_argument("--out", required=True)
ap.add_argument("--cluster-map")
ap.add_argument("--name", default="CarLook")
a = ap.parse_args()

so, sn = os.path.getsize(a.orig), os.path.getsize(a.new)
if so != sn:
    sys.exit("original and modified content.kzb must have the same size (all edits in place): %d != %d" % (so, sn))
ORIG, NEW = sha1(a.orig), sha1(a.new)
if ORIG == NEW:
    sys.exit("modified file is identical to the original")

OUT = a.out
if os.path.exists(OUT):
    shutil.rmtree(OUT)
CL = os.path.join(OUT, "CarLook")
for d in ("bin", "data", "backup", "final", "version", "to_uninstall_move_this_file_up"):
    os.makedirs(os.path.join(CL, d))
with open(os.path.join(CL, "to_uninstall_move_this_file_up", "UNINSTALL.txt"), "w") as f:
    f.write("move me one folder up (into CarLook/) and run the update again to uninstall\n")
shutil.copy(a.sha1sum, os.path.join(CL, "bin", "sha1sum"))
shutil.copy(a.new, os.path.join(CL, "data", "content.kzb"))
shutil.copy(a.orig, os.path.join(CL, "backup", "content.kzb.original"))

ins = open(os.path.join(HERE, "install.sh"), encoding="utf-8").read()
ins = re.sub(r"ORIG=[0-9a-f]{40}", "ORIG=" + ORIG, ins)
ins = re.sub(r"NEW=[0-9a-f]{40}", "NEW=" + NEW, ins)
ins = re.sub(r"SIZE=\d+", "SIZE=%d" % sn, ins)
ins = re.sub(r"TRAIN=\S+", "TRAIN=" + a.train, ins)
with open(os.path.join(CL, "install.sh"), "w", newline="\n") as f:
    f.write(ins)
with open(os.path.join(CL, "version", "CarLook"), "w", newline="\n") as f:
    f.write(a.name + " v1\n")

cm_part = ""
if a.cluster_map:
    for d in ("ClusterMap", "Custom"):
        shutil.copytree(os.path.join(a.cluster_map, d), os.path.join(OUT, d))
    cm_part = r'''
# 1) mib-cluster-map installer, unchanged. Skipped if this card already shows a successful install
#    (unless ClusterMap/UNINSTALL.txt is present).
if [ ! -f $VOLUME/ClusterMap/UNINSTALL.txt ] && grep -q CLUSTERMAP_INSTALL_OK $VOLUME/ClusterMap/update_result.txt 2>/dev/null; then
  echo "ClusterMap: already installed (CLUSTERMAP_INSTALL_OK on this card) - skipped" >> $LOG
else
  on -f mmx /bin/sh $LOCAL/ClusterMap/final/update_run.sh $LOCAL "${1}" "${2}" >> $VOLUME/ClusterMap/final_trace.txt 2>&1
  echo "ClusterMap update_run.sh exit $?" >> $LOG
  grep -h -e CLUSTERMAP_INSTALL_OK -e CLUSTERMAP_UNINSTALL -e "not supported" $VOLUME/ClusterMap/update_result.txt >> $LOG 2>/dev/null
fi
'''
final = r'''#!/bin/ksh
# FinalScript (runs on the RCC after the SWDL copy step)
echo "[CarLook] finalScript HW=${1} medium=${2}"
VOLUME="${2}"
if [ -z "$VOLUME" ] || [ ! -d "$VOLUME/CarLook" ]; then
  for V in /net/mmx/fs/sda0 /net/mmx/fs/sdb0; do [ -d "$V/CarLook" ] && VOLUME=$V; done
fi
if [ ! -d "$VOLUME/CarLook" ]; then echo "[CarLook] folder not found"; touch /tmp/SWDLScript.Result; exit 0; fi
case "$VOLUME" in
  /net/*/*) LOCAL=/${VOLUME#/net/*/} ;;
  *) LOCAL=$VOLUME ;;
esac
LOG=$VOLUME/CarLook/result.txt
on -f mmx /bin/mount -uw $VOLUME
echo "==== run: HW=${1} medium=${2} card=$VOLUME mmx=$LOCAL" >> $LOG
''' + cm_part + r'''
# 2) CarLook
/bin/ksh $VOLUME/CarLook/install.sh "$VOLUME" >> $LOG 2>&1
echo "CarLook install.sh exit $?" >> $LOG
on -f mmx /bin/mount -ur $VOLUME
echo "[CarLook] done"
touch /tmp/SWDLScript.Result
'''
fs = os.path.join(CL, "final", "finalScript.sh")
with open(fs, "w", newline="\n") as f:
    f.write(final)
hs = os.path.join(CL, "final", "hashes.txt")
with open(hs, "w", newline="\n") as f:
    f.write('[CarLook\\final\\finalScript.sh]\nCheckSum = "%s"\n' % sha1(fs))
ver = os.path.join(CL, "version", "CarLook")

component = '''
[CarLook\\final\\Dir]
FileSize = "%d"
CheckSum = "%s"

[CarLook]
VendorInfo = "CarLook"
DeviceDescription = "%s"
DownloadGroup = "CarLook"

[CarLook\\version\\0\\default\\File]
checkUpdate = "true"
CheckSumSize = "524288"
CheckSum = "%s"
FileSize = "%d"
Version = "1"
Source = "../../CarLook"
Destination = "/HBpersistence/SWDL/Log/service/CarLook"
DisplayName = "%s"
DeleteDestinationDirBeforeCopy = "false"
UpdateOnlyExisting = "false"
''' % (os.path.getsize(hs), sha1(hs), a.name, sha1(ver, 524288), os.path.getsize(ver), a.name)

if a.cluster_map:
    meta = open(os.path.join(a.cluster_map, "metainfo2.txt"), encoding="utf-8").read().replace("\r\n", "\n")
    meta = re.sub(r'release = ".*?"', 'release = "%s + mib-cluster-map"' % a.name, meta, count=1)
else:
    meta = open(os.path.join(HERE, "metainfo2_template.txt"), encoding="utf-8").read().replace("{NAME}", a.name)
meta = re.sub(r'FinalScript = ".*?"', 'FinalScript = "./CarLook/final/finalScript.sh"', meta)
meta = re.sub(r'FinalScriptChecksum = ".*?"', 'FinalScriptChecksum = "%s"' % sha1(fs), meta)
meta = re.sub(r'FinalScriptMaxTime = ".*?"', 'FinalScriptMaxTime = "900"', meta)
meta = meta.rstrip("\n") + "\n" + component
with open(os.path.join(OUT, "metainfo2.txt"), "w", newline="\r\n") as f:
    f.write(meta)
print("package written to", OUT)
print("  original SHA1", ORIG)
print("  modified SHA1", NEW, "size", sn)
print("Install: card in SD1 (SD2 empty), red menu -> Update -> SD -> '%s'. Then check CarLook/result.txt" % a.name)
