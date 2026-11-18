#!/bin/sh
# Rear camera research, step 1: READ ONLY, runs in the BACKGROUND.
# Why background: while the green engineering menu is open, the MMI does not show the rear camera.
# Flow: press the GEM button -> close the green menu -> recording starts after 20 s,
# then logs the display table (dmdt gs) every 2 s for ~100 s. Changes nothing on the MMI.
trap '' HUP
PATH=/proc/boot:/bin:/usr/bin:/usr/sbin:/sbin:/mnt/app/armle/bin:/mnt/app/armle/sbin:/mnt/app/armle/usr/bin:/mnt/app/armle/usr/sbin:$PATH
export PATH
KAM=`cd "\`dirname "$0"\`" && pwd`
VOL=`dirname "$KAM"`
SHIM=$VOL/ClusterMap/lib/libdmdt_flush.so
mount -uw "$VOL" 2>/dev/null
N=1
while [ -e "$KAM/scan_$N" ]; do N=`expr $N + 1`; done
OUT=$KAM/scan_$N
mkdir -p "$OUT" || exit 0
dm() {
    ( cd /eso && LD_PRELOAD="$SHIM" LD_LIBRARY_PATH=/eso/lib:/lib:/usr/lib \
      IPL_CONFIG_DIR=/etc/eso/production ./bin/apps/dmdt "$@" ) 2>&1
}
echo "started, waiting 20 s" > "$OUT/STATUS.txt"
sleep 20
dm gd > "$OUT/0_gd_start.txt"
BASE=`dm gs`
echo "$BASE" > "$OUT/0_gs_start.txt"
CHG=0
i=1
while [ $i -le 50 ]; do
    NOW=`dm gs`
    if [ "$NOW" = "$BASE" ]; then
        echo "$i same" >> "$OUT/timeline.txt"
    else
        echo "$i CHANGED" >> "$OUT/timeline.txt"
        CHG=`expr $CHG + 1`
        echo "$NOW" > "$OUT/changed_gs_$i.txt"
        if [ $CHG -eq 1 ] || [ $CHG -eq 5 ]; then
            dm gd > "$OUT/changed_gd_$i.txt"
            dm gc > "$OUT/changed_gc_$i.txt"
        fi
    fi
    echo "sample $i/50, changes $CHG" > "$OUT/STATUS.txt"
    i=`expr $i + 1`
    sleep 2
done
dm gd > "$OUT/9_gd_ende.txt"
echo "done $N, changes $CHG" > "$OUT/DONE.txt"
exit 0
