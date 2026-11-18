#!/bin/sh
. "`dirname "$0"`/common.sh"
# Replacement for mib-cluster-map's ClusterMap/GEM/savelogs.sh (GPL-3.0, MacsInSpace/mib-cluster-map):
# camera research tools (RAM only / read only) keep running in the background
# after the green menu is closed. TEST_ACTIVE.txt wins over SCAN_ACTIVE.txt. Without flag files: original behaviour.
K="$VOLUME/Camera"
if [ -f "$K/TEST_ACTIVE.txt" ] && [ -f "$K/camera_to_cockpit_test.sh" ]; then
    ( trap '' HUP; sh "$K/camera_to_cockpit_test.sh" </dev/null >/dev/null 2>&1 ) &
    echo "Camera-to-cockpit test running in the background (90 s, restores afterwards)."
    exit 0
fi
if [ -f "$K/SCAN_ACTIVE.txt" ] && [ -f "$K/camera_scan.sh" ]; then
    ( trap '' HUP; sh "$K/camera_scan.sh" </dev/null >/dev/null 2>&1 ) &
    echo "Camera scan running in the background. Close the green menu now."
    exit 0
fi
echo "Phone map on the digital cluster: save logs"
sh "$PKG/collect_logs.sh"
exit 0
