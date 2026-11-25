# Cockpit overlay for the mib-cluster-map stream player (speed, consumption, fuel)

Draws two small boxes over the CarPlay / Android Auto map in the cockpit: the speed on the left, the long term
consumption and the estimated fuel left (with a jerrycan) on the right. What it looks like, settings,
install and lessons learned: **[docs/13](../../docs/13-cockpit-overlay.md)**. Written for mib-cluster-map
**v1.2.6** on `MHI2_ER_AU57x_K3663_1`. **Works in our car (our current final cockpit version).**

This is a change to [MacsInSpace/mib-cluster-map](https://github.com/MacsInSpace/mib-cluster-map)'s player
(GPL-3.0). All map work is theirs; this folder only holds our addition.

| File | What |
|---|---|
| `mcm-v1.2.6-opengl_gpu.patch` | the 3 lines added to `player/opengl_gpu.cc` of mib-cluster-map v1.2.6 |
| `player/speed_overlay.h` | the overlay (0.8): reads `/tmp/vehlog_speed` and `/tmp/vehlog_fuel` every 300 ms, draws with its own small GLES2 shader on texture unit 3, never touches the map |
| `player/speed_font.h` | big digits and "km/h" as 8-bit alpha bitmaps, from `tools/make_font.py` |
| `player/speed_font_small.h` | small digits, ",", "l/100km", "L", the small "km/h" and the jerrycan, from `tools/make_font_small.py` |
| `player/DejaVu-LICENSE.txt` | the fonts are rendered from DejaVu Sans |
| `test/overlay_test.cc` | PC test with GL stubbed out (any Linux g++): positions, box sizes, no flicker over 10 simulated minutes, hiding when VehLog stops, fuel line rules, config file |

Behaviour:

- The values come from [VehLog](../vehlog/): `"<km/h> <counter>"` and `"<l/100km x10> <litres> <counter>"`,
  `-1` = unknown. A new counter means VehLog is alive; after 6 s without one, or with `/tmp/speedoverlay-off`,
  nothing is drawn. Reads that are empty or have no line end yet (VehLog rewrites in place) keep the last value.
- Both boxes have one fixed size (126×47 on our car): the larger of `888 km/h` and the widest fuel content
  (`88,8 l/100km` over `150 L`), plus 6 px from the ink to the edge left and right and 5 px top and bottom.
  Speed digits are right-aligned so the (small) "km/h" never moves. Background 70 % opaque by default.
- `/mnt/persist/speedoverlay.conf`, read when the player starts: `pos=bl|br|tl|tr`, `left=`, `right=`,
  `top=`, `bottom=` (speed box), `fuel=on|off`, `fuelright=` (fuel box, default: same as `left`),
  `alpha=` (box opacity, 40 to 255, default 179).
- Every time a box appears or disappears, the player logs it with the reason (at most 40 lines).

Install and rollback on the unit: `tools/vehlog/sd/vehlog/speedoverlay.sh` (replaces the player only when it
is the original v1.2.6 one or an older build of ours, sha1 checked, and keeps the original).

## Build

1. QNX toolchain image from [luka-dev/qnx65-armv7-toolchain](https://github.com/luka-dev/qnx65-armv7-toolchain)
   (tag `8.5`), as mib-cluster-map's `build_local.sh` expects. On Windows: clone with
   `git -c core.autocrlf=false`, then `git -c core.autocrlf=false archive HEAD | docker build ... -` so the
   symlinks and line endings survive.
2. mib-cluster-map at tag `v1.2.6`, apply the patch, copy the three headers to `player/`.
3. `./build_local.sh player`. On Windows run it in a Docker volume, not in a Windows folder (the 32-bit QNX
   `ar` fails on Windows mounts with "Value too large for defined data type").
4. Check: build the unchanged v1.2.6 first; it should be byte-identical to the released player
   (sha1 `0e38137fe58434d6722a09c5e5c67c10e91c0f92`). Run `tools/neon_audit.py` with
   `tools/neon_audit.allow` on the patched build. The only new imports should be `glBlendFunc`,
   `glDeleteShader`, `glDisable`, `glEnable`, `stat`, `time`.

PC test: `g++ -std=c++11 -Wall -Wextra -o t test/overlay_test.cc && mkdir -p /mnt/persist /out && ./t && ./t conf && ./t fueloff && ./t oldformat`
(as root in a throwaway container, it writes to `/tmp` and `/mnt/persist`).
