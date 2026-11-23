# 11: Vehicle data in the HMI: speed yes, oil temperature no (VehLog)

Goal: show speed and oil temperature as a small bar over the CarPlay map in the cockpit. Before building
anything that draws, we wanted to know **which values the MMI actually gets**. So we wrote VehLog, a small
**read-only** logger inside the Java HMI.

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
