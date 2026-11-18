# 07 – Display manager (`dmdt`) and the rear camera in the cockpit

Goal: show the **factory rear-view camera** in the virtual cockpit while reversing (some paid "cluster mods"
advertise this). Result on our A6 C7.5: **the switching works, but the cockpit shows no picture** – the camera
is a hardware video layer. Below is everything we measured, so the next person can start where we stopped.

> Measurements are from our car; the explanations are our best interpretation and may be wrong.

Tools: [`tools/camera/`](../tools/camera/) – started via mib-cluster-map's green-menu entry
"Save logs to SD" (our modified `savelogs.sh` runs them when a flag file is on the card).

## `dmdt` – the display manager tool

```
cd /eso && LD_LIBRARY_PATH=/eso/lib:/lib:/usr/lib IPL_CONFIG_DIR=/etc/eso/production ./bin/apps/dmdt <cmd>
```
(mib-cluster-map adds `LD_PRELOAD=.../libdmdt_flush.so` so the output can be redirected to a file.)

| command | what |
|---|---|
| `gs` | system: displays, the context each display shows, the displayables in it |
| `gd` | all displayables that currently exist (ID, type, size, buffers) |
| `gc` | all contexts and their displayables |
| `dc <context> <displayable...>` | set what a context contains |
| `sc <display> <context>` | show a context on a display |

All changes are **RAM only** and gone after a reboot. On our car the cockpit is display **1** in `gs`, but
`sc`/`sb` need display id **4** (as mib-cluster-map notes). Cockpit context = **73**, stock content = displayable
**33** `DISPLAYABLE_KOMBI_MAP_VIEW` (800×298); mib-cluster-map's player puts its own displayable there while
CarPlay is shown.

## What happens when you reverse (measured)

Our first scans showed nothing, because **the camera does not pop up while the green menu is open**.
The scan therefore runs in the background: press the button, close the green menu, then shift.

| state | display 0 (centre) | cockpit |
|---|---|---|
| camera off | context 0 / 24 (`16 DISPLAYABLE_HMI`, + `41 DISPLAYABLE_3D`) | context 73 / displayable 33 |
| **camera on** | **context 3** = `16 DISPLAYABLE_HMI` + **`17 DISPLAYABLE_REAR_VIEW_CAM`** | unchanged 73 / 33 |

`gd` while the camera is on: `17  stream  800 x 480  0 buffers  DISPLAYABLE_REAR_VIEW_CAM`
(it only exists while the camera is shown). Other contexts with the camera: −2 (camera only) and 38 (HMI2 + camera).
The camera stays on in D/P until you drive a bit or press the camera button (normal Audi behaviour).

So **"is the camera on?" = "is display 0 on context 3?"** – no CAN or gear signal needed. Poll `dmdt gs` every
1–2 s.

## The test: put displayable 17 into the cockpit

`camera_to_cockpit_test.sh` (RAM only, restores after 90 s, aborts if the cockpit isn't 73 as expected):

```
camera on : dmdt dc 73 17 ; dmdt sc 4 73
camera off: dmdt dc 73 <previous> ; dmdt sc 4 73
```

Result in the car: the cockpit map **disappears** when reversing and **comes back** afterwards (2 cycles, logged)
– but the cockpit only shows its **empty grid**. Reason: displayable 17 is of type **`stream`** with **0 buffers** –
the camera picture is composed by hardware directly onto the centre display. The cockpit gets its picture from
an encoder that reads the **buffers** of `window` displayables (like 33 or mib-cluster-map's player window).
A stream layer has no buffer to read.

## How the paid solutions probably do it

Their feature lists (CarPlay in cluster, CarPlay mirror, videos in cluster, rear camera in cluster) all fit one
technique: **a native QNX program that draws pictures into its own window displayable** and puts that window
into context 73 – exactly what mib-cluster-map's player does for the phone map. For the camera such a program
would have to **grab the camera frames from the video input** (Tegra capture / video libraries on the MMX) and
copy them into its window, switched on when display 0 goes to context 3.

What that would need:
- a QNX 6.5 ARM toolchain (mib-cluster-map builds native code, see their `tools/qnx_cc.sh`, and has a probe that
  drives the Tegra video libraries directly: `tools/nvprobe`) – a good starting point,
- finding which library/device on the MMX delivers the camera frames (a read-only scan of `/eso/lib`, `/usr/lib`,
  `/dev` for capture/video entries would be step 1),
- many test rounds in the car. Risk is mostly a crashing test program (reboot fixes it) if it is started from the
  SD card and never installed.

### Confirmed by the mib-cluster-map developer

We asked in [mib-cluster-map #147](https://github.com/MacsInSpace/mib-cluster-map/issues/147). The developer
[confirmed](https://github.com/MacsInSpace/mib-cluster-map/issues/147#issuecomment-6990932792) this picture:

- The rear camera is **not a picture any program on the unit draws.** It goes straight from the camera input
  onto the centre screen as a hardware video layer. That is why the cockpit encoder sees nothing.
- A native program would be the way (he knows of no simpler one): open the **Tegra's video capture input**
  itself and draw the frames into a window in context 73, like their player does with the phone map.
- **The catch:** the factory camera software holds that capture input while reversing. A program would have to
  share it and **give it back cleanly, otherwise the centre screen loses its rear camera.** That makes this a
  safety-relevant mod, not just cosmetics.
- **Drive modes** in the cockpit are a different thing again: that is the car's own data from the car network,
  not a picture.
- He does not plan to build it, and advises caution with **closed tools sold for crypto that patch the camera
  path**, because there is no way to check what they change or how to undo it.

**So the choice is:** write (and carefully test) such a native capture program yourself, with the mib-cluster-map
sources and their QNX toolchain as the best starting point, or do without the camera in the cockpit.

We stopped here. **Important limit:** the cockpit shows MMI pictures **only in its navigation tab** – the other
tabs are drawn by the instrument cluster itself. Paid solutions have the same limit (their camera/video also
only appears in the nav tab).

## Files

- `camera_scan.sh` – read-only, background, 20 s delay, then `dmdt gs` every 2 s for 100 s; on change also
  `gd`/`gc`. Output `Camera/scan_N/timeline.txt` + `changed_*.txt` on the card.
- `camera_to_cockpit_test.sh` – the RAM-only switch test above. Output `Camera/test_N/log.txt`.
- `savelogs.sh` – replaces mib-cluster-map's `ClusterMap/GEM/savelogs.sh`: runs the test if `Camera/TEST_ACTIVE.txt`
  exists, else the scan if `Camera/SCAN_ACTIVE.txt` exists, else the original "save logs". Put the scripts in
  a `Camera/` folder on the card.
