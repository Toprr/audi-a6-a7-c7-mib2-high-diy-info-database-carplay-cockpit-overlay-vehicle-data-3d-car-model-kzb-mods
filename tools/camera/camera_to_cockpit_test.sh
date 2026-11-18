#!/bin/sh
# Rear camera to cockpit, step 2: TEST (RAM only, nothing is installed).
# When the centre screen shows the rear camera (display 0 -> context 3), displayable 17
# is also put into cockpit context 73; when the camera goes off, the previous displayable comes back.
# Runs ~90 s and ALWAYS restores at the end. A reboot also resets everything.
# RESULT on an A6 C7.5: switching works, but the cockpit only shows the empty grid -
# displayable 17 is a hardware video 'stream' without buffers, the cluster encoder gets no picture.
trap '' HUP
PATH=/proc/boot:/bin:/usr/bin:/usr/sbin:/sbin:/mnt/app/armle/bin:/mnt/app/armle/sbin:/mnt/app/armle/usr/bin:/mnt/app/armle/usr/sbin:$PATH
export PATH
KAM=`cd "\`dirname "$0"\`" && pwd`
VOL=`dirname "$KAM"`
SHIM=$VOL/ClusterMap/lib/libdmdt_flush.so
VC_CTX=73
VC_DISP=4
CAM=17
CAM_CTX=3
mount -uw "$VOL" 2>/dev/null
N=1
while [ -e "$KAM/test_$N" ]; do N=`expr $N + 1`; done
OUT=$KAM/test_$N
mkdir -p "$OUT" || exit 0
LOG=$OUT/log.txt
log() { echo "$*" >> "$LOG"; }
dm() {
    ( cd /eso && LD_PRELOAD="$SHIM" LD_LIBRARY_PATH=/eso/lib:/lib:/usr/lib \
      IPL_CONFIG_DIR=/etc/eso/production ./bin/apps/dmdt "$@" ) 2>&1
}
# prints "ctx0 ctx1 first_displayable_of_display1" (shell built-ins only)
lese() {
    dm gs > "$OUT/.gs" 2>&1
    _d=""; _c0=""; _c1=""; _w1=""
    while read _l; do
        set -f; set -- $_l; set +f
        case "$1" in
            display) _d=$2 ;;
            context) [ "$_d" = "0:" ] && _c0=$3; [ "$_d" = "1:" ] && _c1=$3 ;;
            [0-9]*) [ "$_d" = "1:" ] && [ -z "$_w1" ] && _w1=$1 ;;
        esac
    done < "$OUT/.gs"
    echo "$_c0 $_c1 $_w1"
}
zahl() { case "$1" in ''|*[!0-9]*) return 1 ;; esac; return 0; }
echo "started, waiting 20 s" > "$OUT/STATUS.txt"
sleep 20
set -- `lese`
C0=$1; C1=$2; ORIG=$3
cp "$OUT/.gs" "$OUT/0_gs_start.txt"
log "start: centre=$C0 cockpit-context=$C1 cockpit-displayable=$ORIG"
if [ "$C1" != "$VC_CTX" ] || ! zahl "$ORIG" || [ "$ORIG" = "$CAM" ]; then
    log "ABORT: cockpit not as expected - nothing changed"
    echo "abort" > "$OUT/DONE.txt"; exit 0
fi
AN=0
restore() {
    if [ $AN -eq 1 ]; then
        dm dc $VC_CTX $ORIG >> "$LOG"; dm sc $VC_DISP $VC_CTX >> "$LOG"
        AN=0; log "$1: cockpit back to displayable $ORIG"
    fi
}
trap 'restore Signal; exit 0' INT TERM
i=1
while [ $i -le 45 ]; do
    set -- `lese`
    C0=$1
    if [ "$C0" = "$CAM_CTX" ] && [ $AN -eq 0 ]; then
        log "$i: camera on (centre=$C0) -> displayable $CAM into cockpit"
        dm dc $VC_CTX $CAM >> "$LOG"; dm sc $VC_DISP $VC_CTX >> "$LOG"
        AN=1
        cp "$OUT/.gs" "$OUT/gs_camera_$i.txt"
    elif [ "$C0" != "$CAM_CTX" ] && [ $AN -eq 1 ]; then
        restore "$i: camera off (centre=$C0)"
    else
        log "$i: centre=$C0 cockpit=$AN"
    fi
    echo "sample $i/45, cockpit-camera=$AN" > "$OUT/STATUS.txt"
    i=`expr $i + 1`
    sleep 2
done
restore "end"
dm gs > "$OUT/9_gs_ende.txt"
rm -f "$OUT/.gs"
echo "done $N" > "$OUT/DONE.txt"
exit 0
