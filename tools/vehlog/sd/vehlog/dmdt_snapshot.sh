#!/bin/sh
# Read-only snapshot of the display manager (which windows are in which context),
# for the street name bar question. Runs on the MMX (command.sh starts it with
# "on -f mmx"). dmdt gs / gc / gd only list, they change nothing.
#   dmdt_snapshot.sh <number>   -> vehlog/logs/dmdt_<number>/ on the SD card
VOL=/fs/sda0
N=${1:-0}
OUT=$VOL/vehlog/logs/dmdt_$N
mkdir -p "$OUT"
# dmdt buffers its output when it is not a terminal; mib-cluster-map's flush shim fixes that
SHIM=$VOL/vehlog/libdmdt_flush.so
for c in gs gc gd; do
    ( cd /eso && LD_PRELOAD="$SHIM" LD_LIBRARY_PATH=/eso/lib:/lib:/usr/lib \
      IPL_CONFIG_DIR=/etc/eso/production ./bin/apps/dmdt $c ) > "$OUT/dmdt_$c.txt" 2>&1
done
# what mib-cluster-map says about CarPlay at this moment (streaming = CarPlay map is up)
cat /tmp/mibr-carplay111.state > "$OUT/carplay_state.txt" 2>&1
ls -la /tmp/vehlog_speed* >> "$OUT/carplay_state.txt" 2>&1
echo "done" > "$OUT/done.txt"
