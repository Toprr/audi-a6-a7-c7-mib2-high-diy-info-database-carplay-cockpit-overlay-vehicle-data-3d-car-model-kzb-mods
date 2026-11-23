# 10: Star button skip and touchpad cursor in CarPlay (MIB2-Patcher)

**This is not our project.** Both patches come from
**[tfourj/MIB2-Patcher](https://github.com/tfourj/MIB2-Patcher)** (pointed out to us by a user in
[issue #1](https://github.com/Toprr/audi-mib2-high-c7-carplay-cluster-map-3d-car-model-diy/issues/1)). All credit goes to them; go there for downloads, docs and support.
We only report what we checked and what we saw on our car.

> Tested on **one** car: Audi A6 Avant C7.5, `MHI2_ER_AU57x_K3663_1` (MU1425), MIB2-Patcher **v1.3.0**,
> together with mib-cluster-map v1.2.6 and our CarLook `content.kzb`. Patches like these change the Java HMI:
> if something goes wrong the centre screen may not start. Full M.I.B. backup first, see [docs/00](00-safety.md).

## Our result

| Patch | What it does | Our result |
|---|---|---|
| **JOKERBTN** | The steering-wheel star (joker) button skips to the next track in **native** CarPlay | ✅ works |
| **TOUCHCURSOR** | The MMI touchpad moves a small cursor in CarPlay, a tap "touches" where it is | ✅ works |

- The **CarPlay map in the cockpit** (mib-cluster-map) keeps working with both patches installed.
- Stock, the C7 has no way to skip in CarPlay from the steering wheel, and the touchpad only jumps from item to
  item like the knob. With the two patches, native CarPlay on the MMI is much nicer to use.
- JOKERBTN also changes two other buttons by default: **INFO = previous track**, **voice button short press =
  next track** (long press still starts Siri). Each can be set back to `stock` in the green menu
  (MIB2_Patcher → JOKERBTN → INFO_button / Voice_button), no reboot needed.
- **Only native CarPlay** (iPhone on the car's USB). With CarPlay on an aftermarket tablet and sound via AUX the
  MMI has no CarPlay session, so JOKERBTN has nothing to skip there.

## Compatibility check with mib-cluster-map (before installing)

Both projects add Java jars to the HMI start script `lsd.sh` (`-Xbootclasspath`). If two jars replace the
**same class**, one of them silently loses. We listed the classes in every jar:

| Jar | Replaces |
|---|---|
| JOKERBTN | `AbstractTerminalModeHMIApplication`, `SDSManagerGUI` (+ own new classes) |
| TOUCHCURSOR | `DSICarplaySafeProxy`, `ATIPEvent` (+ own new classes) |
| mib-cluster-map `NavActiveIgnore` (Audi) | `AndroidAuto2NavHandler`, `org.dsi.ifc.carplay.AppState` |
| mib-cluster-map wheel zoom | `CtxShown$ZoomUpdateDelayTimer` |
| MIB2-Patcher **NAVIGNORE** | `org.dsi.ifc.carplay.AppState`: **the same class as mib-cluster-map's NavActiveIgnore** |

So: **JOKERBTN and TOUCHCURSOR do not overlap with mib-cluster-map.** NAVIGNORE does, and is not needed
anyway (mib-cluster-map already brings its own NavActiveIgnore). That's why we did **not** use
"Install all" in the MIB2-Patcher green-menu screen.

The `lsd.sh` edits also fit together: on K3663 mib-cluster-map adds its jars as their own
`BOOTCLASSPATH=...` lines, MIB2-Patcher adds marked blocks (`#JOKERBTN`, `#TOUCHCURSOR`) in front of the
Java start line and backs up `lsd.sh` first. Neither touches the other's lines.

## How we installed it (without SSH / laptop)

MIB2-Patcher needs M.I.B. once to get onto the unit. We used the same SD card and the same M.I.B. version
(3.6.0) we made our backup with.

1. Full M.I.B. backup (if not done yet). Back up whatever is on the card.
2. On the PC: card content = M.I.B. 3.6.0 + the `mib2-patcher` folder from the MIB2-Patcher release zip
   (we checked: the release is identical to the source on GitHub, jar checksums match).
3. `mib2-patcher/command.sh` copied to `mod/command.sh`, with `PATCHER_RUN` set to only what we want:

   ```sh
   PATCHER_RUN="
   update
   JOKERBTN install
   TOUCHCURSOR install
   "
   ```

4. MMI car settings: star button set to **skip forward** (JOKERBTN needs that, with the joker on `stock`).
5. Card in **SD1**, green menu → **M.I.B. → Advanced Settings → Run individual script**, wait for
   "it is all done!" (1-2 min, the firmware checksum takes a while). **Not** via the red menu: that would run
   the whole M.I.B. installation again.
6. Restart the MMI: hold **MENU + rotary knob (pressed) + the upper right button** until the screen goes black.
7. Check: green menu → **MIB2_Patcher** → JOKERBTN / TOUCHCURSOR → Status: "patched", boot guard ok.
8. Delete `mod/command.sh` from the card again. The logs are on the card in `mib2-patcher/logs/`.

What the logs showed: firmware fingerprint `lsd.jxe` sha1 `2be4758b…` = K3663 (the installer refuses anything
else), `lsd.sh` backed up as `lsd.sh.jokerbtn.bak` / `lsd.sh.touchcursor.bak`, both patches "loaded on the last
start".

## Rollback

- Green menu → MIB2_Patcher → JOKERBTN or TOUCHCURSOR → **Uninstall**, then restart the MMI.
- Without the green menu: same `mod/command.sh` trick with `JOKERBTN uninstall` / `TOUCHCURSOR uninstall`.
- **Boot guard** (their safety net): if the HMI does not start cleanly within a minute, the patch disables itself
  on the next start.
- Use **Uninstall**, not **Restore**, when more than one patch is installed: Restore puts back the `lsd.sh` from
  before that patch's first install and can take other patches' lines with it (their README says the same).

## Pitfall: the touchpad cursor is suddenly gone

After a few days the CarPlay touchpad cursor stopped working on our car, the touchpad was back to stock.
Nothing was broken: **TOUCHCURSOR's boot guard had switched it off.**

- Its guard leaves a "pending" flag at every HMI start and removes it **after 60 seconds**. If the MMI is
  restarted or switched off within that minute (unlock the car, the MMI starts, you leave again; or a quick
  MENU + knob + upper right restart), the next start counts that as a failed start and TOUCHCURSOR stays off.
- Check: green menu → MIB2_Patcher → TOUCHCURSOR → Status says **DISABLED** (flag
  `/mnt/persist/touchcursor_disabled`).
- Fix: same screen → **Install** (that re-arms it), restart the MMI, leave it on for a minute. Or the
  `mod/command.sh` trick with `TOUCHCURSOR install`.
- JOKERBTN behaves differently: its guard is confirmed as soon as its class is loaded.

**Automatic re-arm (our own addition, optional):** our read-only logger
[VehLog](11-vehicle-data-logger.md) also re-arms TOUCHCURSOR after such a false stop. Once an HMI start has
run cleanly for 90 s and `touchcursor_disabled` exists, it deletes that flag, so TOUCHCURSOR loads again on
the next start. It does this at most **3 times in a row**: if TOUCHCURSOR really broke the start, it stays off
after that. The count goes back to 0 after TOUCHCURSOR's next good start. Tested on the PC; on our car the
fix above brought TOUCHCURSOR back after exactly this kind of false stop. This changes another project's
safety net, so only use it if you understand the trade-off.
