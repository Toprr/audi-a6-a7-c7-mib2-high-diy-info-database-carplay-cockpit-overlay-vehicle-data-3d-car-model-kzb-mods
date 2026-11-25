# 13: Cockpit overlay over the CarPlay map: speed, consumption, fuel, quiet street label

**Status (snapshot of 2026-11-25): working in our car, still being tested.** Everything below runs on our A6
and looks right on the road. We keep driving with it and will update this page when something changes.

> Tested on **one** car: Audi A6 Avant C7.5, `MHI2_ER_AU57x_K3663_1` (MU1425), with mib-cluster-map **v1.2.6**
> and MIB2-Patcher JOKERBTN + TOUCHCURSOR. It changes the Java HMI start (`lsd.sh`) and replaces
> mib-cluster-map's stream player. Full M.I.B. backup first, see [docs/00](00-safety.md). The scripts refuse
> any other `lsd.jxe` and any other player (sha1 checks) and keep the originals for the way back.

![Cockpit overlay, rendered from the player's own bitmaps](images/cockpit_overlay_preview.png)

*Rendered on the PC from the exact bitmaps the player draws (window 800×298, 2× enlarged). The map behind is a
stand-in. On the car the boxes sit right next to the rev counter and the speedometer.*

## What you get

| Feature | What it does | Status |
|---|---|---|
| **Speed box** (left) | Speedometer value, `45 km/h`. One fixed size for 1 to 3 digits, "km/h" never moves | ✅ in the car |
| **Fuel box** (right) | Long term consumption `8,7 l/100km` over the estimated fuel left `50 L` with a jerrycan | ✅ in the car |
| **No flicker** | Boxes stay on, also with cruise control (nothing changes for minutes) | ✅ in the car |
| **Quiet street label** | While the CarPlay map is up, the Audi navigation's street name never reaches the cluster; the label stays as a small empty bar. Without CarPlay (phone flat, Audi nav) everything is as before | ✅ in the car |
| **Maneuver card right** | CarPlay's turn card moves away from the traffic sign at the top left | ✅ in the car |
| **Patch re-arm** | Re-arms TOUCHCURSOR and JOKERBTN when their own boot guard switched them off after a short start | TOUCHCURSOR ✅, JOKERBTN 🔧 testing |

## How it works

```
 Java HMI (lsd.jxe)                                       mib-cluster-map stream player (MMX)
 ┌──────────────────────────────┐   /tmp/vehlog_speed    ┌──────────────────────────────────┐
 │ VehLog                       │   "45 1234"            │ every 300 ms: read both files     │
 │  speed  <- DSICarVehicleStates│ ─────────────────────▶ │ new counter = VehLog alive        │
 │  range, consumption <- Kombi │   /tmp/vehlog_fuel     │ draw two boxes over the map       │
 │  street filter (KOMO)        │   "87 50 617"          │ (own GLES2 shader, texture unit 3)│
 └──────────────────────────────┘ ─────────────────────▶ └──────────────────────────────────┘
```

- **VehLog** ([`tools/vehlog/`](../tools/vehlog/), background in [docs/11](11-vehicle-data-logger.md)) listens
  to the car's values inside the Java HMI and writes two tiny files to `/tmp` (RAM): the value(s) plus a
  **write counter**. It rewrites them every 2 s and the speed also on every change.
- **The player** ([`tools/speedoverlay/`](../tools/speedoverlay/)) is mib-cluster-map v1.2.6 plus 3 lines and
  two headers. Once per frame, after the map and before `eglSwapBuffers`, it draws the boxes with its own
  small shader. A box is hidden only when VehLog has written nothing new for 6 s (VehLog stopped) or
  `/tmp/speedoverlay-off` exists. No file, no box: the map itself is never touched.
- **Speed** is the speedometer value from `DSICarVehicleStates` attribute 14. The car sends it about **once a
  second** (we measured at most 1.2 updates per second over whole minutes of driving). That is the fastest
  speed source the MMI has, so under hard acceleration the number steps about once a second.
- **Fuel left** is an estimate: our car never sends the tank content in litres (`BCTankLevel1` is always
  invalid), so VehLog computes `litres = range km × long term consumption / 100` from `BCCurrentRange1` and
  `BCLongTermAverageConsumption1`. The cluster computes the range from the real tank level, but with its own
  recent consumption, so expect roughly ±10 %. Whole litres only. A line stays empty when its value is invalid.
- **Street label filter:** the navigation's `ClusterService` asks `DSIKOMOManager.getDSIKOMONavInfo()` for the
  cluster interface every time it sends a street. VehLog hands the manager a thin wrapper
  (`setDSIKOMONavInfo`, the same public call the navigation's own Activator uses). It passes **every** call
  through unchanged, except `setCurrentStreet` / `setTurnToStreet` while the CarPlay map is up (mib-cluster-map
  writes `streaming` to `/tmp/mibr-carplay111.state`): those go out as a single space. When CarPlay goes away,
  the last real street is sent again at once. If the navigation replaces the interface, VehLog puts the
  wrapper back within 3 s; if wrapping is not possible, it falls back to sending a blank right after each
  street change.

### Why only a small empty bar and not nothing

The street label and the traffic sign are drawn **by the cluster itself**, over whatever map it gets (see
[docs/11](11-vehicle-data-logger.md#cockpit-window-what-is-drawn-by-whom)). Without any street the cluster
most likely shows "---" (we have not tested that); a single space gives the smallest label we have seen. `DSIKOMONavInfo.setCapabilities(boolean[2])` (the HMI
sends `{true, true}` when it connects) might switch the label off, but nothing in the HMI says what the two
flags mean, so we have not tried it yet.

## Settings (files on the SD card, applied by the next script run)

| File | Content | Meaning |
|---|---|---|
| `vehlog/speedoverlay.conf` | `pos=bl` `left=176` `bottom=12` `fuel=on` `fuelright=180` | speed box corner and distance from the edges in pixels (`pos=bl|br|tl|tr`, `left=`, `right=`, `top=`, `bottom=`), fuel box on/off and distance from the right edge. Read when the player starts (MMI restart) |
| `vehlog/carplay_card.txt` | `right` | CarPlay maneuver card: `left`, `right`, `top`, or `iphone` (back to the phone's choice). mib-cluster-map's own setting, active when the iPhone connects |
| `vehlog/street_blank.txt` | `on` | `off` lets the Audi street name through again, within a few seconds |

On the C7 the player window is 800×298; the rev counter covers about the left 175 pixels and the speedometer
about the right 175.

## Install and way back (no SSH needed)

Build the jar and the player first (see the READMEs in [`tools/vehlog/`](../tools/vehlog/) and
[`tools/speedoverlay/`](../tools/speedoverlay/); we do not publish binaries). Then, with M.I.B. on the unit:

1. Copy `vehlog/` and `mod/command.sh` from `tools/vehlog/sd/` to the M.I.B. SD card, together with your
   `VEHLOG.jar` + `.sha1` and `stream-player.speed` + `.sha1`.
2. Phone unplugged, ignition on: green menu → M.I.B. → Advanced Settings → **Run individual script**
   (3 to 4 minutes; it checks the firmware first).
3. Restart the MMI and **leave it on for 2 minutes** (boot guards).
4. Drive. Run the script once more at the end to copy the logs to `vehlog/logs/` on the card.

Way back: an empty file `vehlog/UNINSTALL` on the card, run the script, restart. That restores the original
player (kept as `stream-player.mcm126`, sha1 checked) and `lsd.sh` byte for byte.

## What we learned on the way (so you don't have to)

- **The RCC shell has no `tr`.** Our first launcher read `carplay_card.txt` with `head | tr` and silently got
  an empty string, so the card and the street setting never applied. The launcher now reads files with shell
  builtins only (`read`, `set --`, `case`), and Windows line ends are allowed for in the patterns.
- **Do not judge "is the writer alive" by the file time on the MMX's `/tmp`.** With a 6 s staleness check on
  `st_mtime`, the boxes were hidden most of the time and flashed up for about half a second every 2 s. Both
  boxes at the same moment, so it was not a read race. A write counter in the file plus the player's own
  `CLOCK_MONOTONIC` fixed it completely (PC test: 10 minutes of simulated cruise control with useless file
  times and reads caught mid-write: 0 of 2000 frames without a box).
- **A temp file renamed into place never showed up in `/tmp`** when VehLog did it (we did not dig into why),
  so VehLog writes the files in place. The player
  therefore ignores reads that are empty or have no line end yet and keeps the last value.
- **The cluster's street label comes back between our blanks** if you only overwrite it afterwards (we saw
  "offroad" flash). Filtering the call on its way to the cluster is the clean way.
- **MIB2-Patcher's boot guards** (TOUCHCURSOR, JOKERBTN) switch a patch off when the MMI is restarted or
  switched off within about a minute of a start, which happens a lot while installing. VehLog re-arms them
  after a start that ran fine for 90 s, at most 3 times in a row, and the launcher re-arms them with
  MIB2-Patcher's own `install`. JOKERBTN, TOUCHCURSOR and VehLog replace different classes, so they do not
  conflict.
- Building mib-cluster-map's player on Windows: see the pitfalls in [docs/11](11-vehicle-data-logger.md#speed-overlay).

## Still testing / ideas

- Confirm the JOKERBTN re-arm on the car.
- `DSICarKombi.updateBCStatisticsTimeCurrentPeriodAC1` arrives about once a second and looks like a live
  consumption (unit code 1, values like 712 / 859 while driving). Not confirmed, not shown yet.
- Hiding the street label completely (`setCapabilities`, see above).
- Faster CarPlay start after boot, and mirroring the rear camera into the cockpit.
