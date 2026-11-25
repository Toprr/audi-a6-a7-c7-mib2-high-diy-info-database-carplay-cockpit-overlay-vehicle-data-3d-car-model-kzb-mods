# 11: Vehicle data in the HMI: speed yes, oil temperature no (VehLog, catalog, speed overlay)

Goal: show speed and oil temperature as a small bar over the CarPlay map in the cockpit. Before building
anything that draws, we wanted to know **which values the MMI actually gets**. So we wrote VehLog, a small
**read-only** logger inside the Java HMI.

> **Update:** the cockpit overlay built on this (speed, consumption, fuel, quiet street label) now works in
> the car. How to use it: **[docs/13](13-cockpit-overlay.md)**. Since 0.8, VehLog is no longer purely
> read-only: its optional street filter (switch `street_blank.txt`) changes the street name the navigation
> sends to the cluster while the CarPlay map is up. Vehicle values are still only read.

> Tested on **one** car: Audi A6 Avant C7.5, `MHI2_ER_AU57x_K3663_1` (MU1425), together with mib-cluster-map
> v1.2.6 and MIB2-Patcher JOKERBTN + TOUCHCURSOR. VehLog changes the Java HMI start (`lsd.sh` boot classpath).
> A mistake there can stop the centre screen from starting. Full M.I.B. backup first, see [docs/00](00-safety.md).
> It refuses to install on any other `lsd.jxe` (sha1 check).

## Results on our car

| Value | Where it comes from in the HMI | Result |
|---|---|---|
| **Speed** | `DSICarVehicleStates`, attribute 14 `DynamicVehicleInfoHighFrequent.vehicleSpeed` (`CarBCSpeed`) | ✅ valid, whole km/h, about one update per 1-2 s. It is the **speedometer value** (needle at 50 = 50 in the log, with the usual speedometer lead) |
| `realVehicleSpeed` (same object) | same | ❌ always 0, state invalid |
| Engine speed (rpm) | same object, `engineSpeed` | ❌ always 0 |
| **Oil temperature** | `DSICarKombi`, attribute 64 `BCOilTemperatureValue` (`CarBCTemperature`) | ❌ one update after start, **invalid** (valid flag 2), never again in 15 min of driving |
| Coolant temperature | `DSICarKombi`, attribute 63 | ❌ same |
| Does the cluster offer oil temperature? | `DSICarKombi`, attribute 4 `BCViewOptions.oilTemperatureValue` | ❌ state **0 = `VIEWOPTIONSTATE_NOT_EXISTING`** |
| Board computer oil display setting | `DSICarKombi`, attribute 30 | off |

**Conclusion:** on this car the instrument cluster tells the MMI that it has **no oil temperature** for it.
The lap timer in the cockpit shows the oil temperature, but that stays inside the cluster. We found no other
interface in the HMI (`lsd.jxe`) that carries it. So a cockpit overlay can get the speed from the MMI, but the
oil temperature would need another source (for example OBD). Speed is enough for a first overlay.

Side notes:
- The stock HMI does not even subscribe to attribute 64. Its board computer list in `BCProcessor` does not
  contain it, and the navigation's `updateBCOilTemperatureValue` is empty.
- The log timestamps are seconds since the HMI start: the MMX has no real clock time at that point.

## Everything else the car sends (VehLog 0.4 catalog)

VehLog 0.4 listens to **every attribute of 16 vehicle DSI services** with one generic, read-only listener
(`java.lang.reflect.Proxy`) and writes a catalog: which method was called, how often, and the newest value.
Two drives of about 30 minutes: **717 methods called, 145 with a valid value, 37 of them changing while
driving.** The full list (names, valid or not, live or once) is in
[docs/12](12-vehicle-data-catalog.md). The useful ones:

| Value | DSI method | On our car |
|---|---|---|
| Speedometer speed | `DSICarVehicleStates.updateDynamicVehicleInfoHighFrequent` | ✅ live (see above) |
| Range | `DSICarKombi.updateBCCurrentRange1` | ✅ live, km |
| Average consumption since start / long term | `DSICarKombi.updateBCShortTermAverageConsumption1` / `...LongTerm...` | ✅ live, l/100 km |
| Trip since start: distance, average speed, time | `DSICarKombi.updateBCShortTermGeneral` | ✅ live |
| Probably current consumption | `DSICarKombi.updateBCStatisticsTimeCurrentPeriodAC1` | ✅ very often (about once a second); unit code 1, meaning not confirmed yet |
| Odometer | `DSICarKombi.updateBCTotalDistance` | ✅ live |
| Clock, date, time source (GPS) | `DSICarTimeUnitsLanguage.updateClockTime` / `...ClockDate` | ✅ live |
| **Reverse gear** | `DSIGeneralVehicleStates.updateReverseGear` | ✅ (useful for anything that should react to reversing) |
| Vehicle standing still | `DSIGeneralVehicleStates.updateVehicleStandstill` | ✅ live |
| Oil level and oil service due | `DSICarVehicleStates.updateOilLevelData`, `DSICarKombi.updateSIAOilInspection` | ✅ once |
| Climate: set temperatures, fan per zone | `DSICarAirCondition.updateAirconTempZone1..4`, `...AirVolumeZone1..4` | ✅ live |
| Drive select profile, light, comfort, assistance settings | `DSICarDrivingCharacteristics`, `DSICarLight`, `DSICarComfort`, `DSICarDriverAssistance` | ✅ once (settings) |
| Units and language | `DSICarTimeUnitsLanguage.*Unit` | ✅ once |

**Not delivered** on this car: oil and coolant temperature, engine speed, `realVehicleSpeed`, current gear,
outside temperature (none of the 16 services carried a valid one).

Notes:
- Every DSI update ends with a valid flag (`1` valid, `2` invalid, `0` unknown). Most of the 717 methods only
  sent one invalid value after subscribing: the function exists in the software, but not in this car.
- Personal data is kept out: phone, messages, contacts, navigation, media and online services are not
  subscribed at all, and methods with `VIN` or `Key` in the name are counted but their values are not stored.
- More values only show up in their situation (parking aid, auxiliary heater, ...). The catalog keeps
  growing with every drive.

## Cockpit window: what is drawn by whom

Read-only snapshots of the display manager (`dmdt gs`) during the drive, run from `mod/command.sh` on the MMX:

| Cockpit shows | Context 73 contains |
|---|---|
| CarPlay map (mib-cluster-map) | only displayable `3` (the stream player's window) |
| Audi navigation map | only displayable `33` (`DISPLAYABLE_KOMBI_MAP_VIEW`) |

So the **street name label** at the bottom ("B16", "offroad") and the **traffic sign** at the top left are
**not MMI windows**: the cluster draws them itself over whatever map it gets (the same answer the
mib-cluster-map developer gave in their issue #126). They cannot be switched off by changing the display
manager's contexts. The player's window is 800×298 pixels; on the C7 the rev counter covers about the
left 180 pixels and the speedometer about the right 150.

What can be moved: CarPlay's own maneuver card. mib-cluster-map (v1.2.6) has the setting
`CLUSTERMAP_CARPLAY_CARD_POSITION=left|right|top`, stored on the unit as
`/mnt/app/root/mibr-carplay111-nav.maneuverLayout` plus `/mnt/app/root/mibr-carplay111-nav-query.enabled`
and read when the iPhone connects.

## Speed overlay

**Works in the car now**, together with a fuel box and the quiet street label: see
**[docs/13](13-cockpit-overlay.md)** for what it does, the settings, install and what we learned.
Sources: [`tools/speedoverlay/`](../tools/speedoverlay/).

- Built with the open QNX 6.5 toolchain ([luka-dev/qnx65-armv7-toolchain](https://github.com/luka-dev/qnx65-armv7-toolchain))
  like mib-cluster-map. **Rebuilding the unchanged v1.2.6 player gave a byte-identical file** to the
  released one (sha1 `0e38137f...`), so the build is reproducible. The overlay adds 3 lines to
  `opengl_gpu.cc` plus two headers, no new libraries.
- On the way there: the first car test showed no box (a temp file renamed into `/tmp` never appeared, VehLog
  now writes in place), the second showed it flickering (the player judged freshness by the file time, now
  by a write counter). Details in docs/13.

Building it on Windows needed a few workarounds, written down for the next person: clone the toolchain with
`core.autocrlf=false` and feed `docker build` a `git archive` (Windows Git turns the toolchain's symlinks
into text files), and build inside a Docker volume, not a Windows folder (the 32-bit QNX `ar` fails there
with "Value too large for defined data type").

## How VehLog works

- **Entry point:** a replacement of the tiny stock data class `org.dsi.ifc.global.CarViewOption` (two `int`
  fields). Same members and the same bytecode as the stock class (checked with mib-cluster-map's
  `bytecode_diff.py`). The only addition is one call in each constructor, inside `try/catch Throwable`, that
  starts the logger thread once. The car app creates these objects while it starts up. Same idea as
  mib-cluster-map's `UpdateGfxState`. No other mod we use replaces this class.
- **Getting the DSI services:** `de.audi.app.car.MenuCtrl.getFramework().getBundleCxt()` (through
  reflection), then the OSGi service `org.dsi.ifc.carkombi.DSICarKombi` / `...DSICarVehicleStates`, and
  `setNotification(attributes, listener)`, the same call the HMI uses itself. MIB2-Patcher's TEL2ACP gets
  DSICarplay the same way.
- **Only listening:** the listeners copy the values into strings. A low-priority daemon thread writes changed
  values every 2 s to `/mnt/persist/vehlog.log` (max 512 KB, then it stops writing). Nothing is sent to the car.
- **Built for the unit's J9:** compiled with `javac -source 1.2 -target 1.2` and the **unit's own class
  library** as boot classpath (the converted `lsd.jar` contains `java.*`), class version 46, no
  StringBuilder. A script checks that every method and field we use on a stock class exists with the same
  descriptor and is public (and it catches deliberately wrong ones).
- **Boot guard** in `lsd.sh`, the same principle as MIB2-Patcher: a pending flag before every HMI start,
  removed by VehLog after 90 s. Still there on the next start → the jar is not loaded any more. The block joins
  the existing `-Xbootclasspath/p:` list, so it coexists with JOKERBTN and TOUCHCURSOR.
- **Installer** (`vehlog.sh`): checks the firmware sha1, the jar sha1 and the `lsd.sh` layout, backs up
  `lsd.sh`, writes the new file to a temp copy and only replaces `lsd.sh` if every line that is not ours is
  byte-identical to before. Uninstall restores `lsd.sh` byte for byte (tested on a copy of the real file).

To convert `lsd.jxe` to a jar on the PC we used [luka-dev/jxe2jar](https://github.com/luka-dev/jxe2jar)
(only its `src/` folder). The converted `lsd.jar` is Audi/Harman code: **keep it on your PC, never publish it**.

The sources and scripts are in [`tools/vehlog/`](../tools/vehlog/). They are written for and tested on
K3663 only.

## Install (without SSH)

Same way as MIB2-Patcher (see [docs/10](10-star-button-and-touchpad-in-carplay.md)): `vehlog/` and
`mod/command.sh` on the M.I.B. SD card, green menu → M.I.B. → Advanced Settings → **Run individual script**,
restart the MMI and **leave it on for 2 minutes**. Running the script again copies the log to
`vehlog/logs/` on the card. An empty file `vehlog/UNINSTALL` on the card makes the next run uninstall it.
