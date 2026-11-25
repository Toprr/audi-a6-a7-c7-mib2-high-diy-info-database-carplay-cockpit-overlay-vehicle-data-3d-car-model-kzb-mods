# VehLog: vehicle data logger and cockpit helper for the MHI2 Java HMI (K3663 only)

Reads vehicle values (never sends any to the car), feeds the cockpit overlay and, if switched on, keeps the
Audi street name off the cockpit while the CarPlay map is up.

Background, results and safety design: [docs/11](../../docs/11-vehicle-data-logger.md).
Written for and tested on `MHI2_ER_AU57x_K3663_1` (MU1425, `lsd.jxe` sha1 `2be4758b...`) only.

| Folder | What |
|---|---|
| `src/` | the jar (0.9): `org/dsi/ifc/global/CarViewOption` (stock class + one guarded start call), `local/vehlog/*` (logger, listeners, `Catalog` = generic listener for 16 vehicle services, `StreetBlank` = street label filter for the cockpit, see [docs/13](../../docs/13-cockpit-overlay.md)) |
| `sd/` | what goes on the M.I.B. SD card: `vehlog/vehlog.sh` (install, uninstall, status, collect, update; `update` also re-arms VehLog when its boot guard switched it off), `vehlog/speedoverlay.sh` (overlay player install/uninstall), `vehlog/dmdt_snapshot.sh` (read-only display manager snapshot, needs mib-cluster-map's `lib/libdmdt_flush.so` next to it), the settings `vehlog/speedoverlay.conf`, `vehlog/carplay_card.txt`, `vehlog/street_blank.txt`, `vehlog/carplay_safearea.txt`, and `mod/command.sh` (the M.I.B. launcher: re-arms the boot guards, installs, applies the settings; shell builtins only, the unit has no `tr`) |
| `build_tools/` | `gen_listener.py` (writes the empty listener methods from `javap`), `check_refs.py` (every stock member we use exists and is public), the hand-written listener methods |
| `test/` | PC tests with stand-ins: `harness.Run` (fake framework and DSI services), `harness.Rearm` (TOUCHCURSOR and JOKERBTN re-arm rules), `harness.CatalogTest` (every listener method of every service), `harness.FuelTest` (litre estimate), `harness.StreetTest` (fallback timing), `harness.FilterTest` (street filter between navigation and cluster, with stand-ins for `Navigation` and `DSIKOMOManager`) |

## Build (PC, JDK 8)

You need the `lsd.jar` converted from **your own** `lsd.jxe` (M.I.B. backup, then
[luka-dev/jxe2jar](https://github.com/luka-dev/jxe2jar)). It is Audi/Harman code, keep it to yourself.

```sh
LSD=/path/to/lsd.jar
javap -cp $LSD org.dsi.ifc.carkombi.DSICarKombiListener | python3 build_tools/gen_listener.py \
    org.dsi.ifc.carkombi.DSICarKombiListener local.vehlog.KombiListener build_tools/keep_kombi.java src/local/vehlog/KombiListener.java
javap -cp $LSD org.dsi.ifc.carvehiclestates.DSICarVehicleStatesListener | python3 build_tools/gen_listener.py \
    org.dsi.ifc.carvehiclestates.DSICarVehicleStatesListener local.vehlog.VehicleListener build_tools/keep_vehicle.java src/local/vehlog/VehicleListener.java
mkdir -p build/classes
javac -source 1.2 -target 1.2 -nowarn -bootclasspath $LSD -d build/classes $(find src -name '*.java')
(cd build/classes && jar cf0M ../VEHLOG.jar org local)
python3 build_tools/check_refs.py javap build/classes $LSD org.dsi.ifc.global.CarViewOption \
    local.vehlog.VehLog local.vehlog.KombiListener local.vehlog.VehicleListener local.vehlog.Catalog 'local.vehlog.Catalog$Entry' \
    local.vehlog.StreetBlank
sha1sum build/VEHLOG.jar | cut -d' ' -f1 > sd/vehlog/VEHLOG.jar.sha1 && cp build/VEHLOG.jar sd/vehlog/
```

Also compare `CarViewOption` with the stock class, for example with mib-cluster-map's
`tools/bytecode_diff.py`: only the two constructors may differ (the added call).

`vehlog.sh` can be tested on a PC against copies: `VEHLOG_TEST_ROOT=<folder> sh sd/vehlog/vehlog.sh install`
(the folder needs `net/mmx/mnt/app/eso/hmi/lsd/lsd.sh`, `net/mmx/ifs/lsd.jxe`, `net/mmx/mnt/persist/`).

## What it changes on the unit

- `/mnt/app/eso/hmi/lsd/jars/VEHLOG.jar`
- a 15-line block marked `#VEHLOG` in front of the J9 start line of `lsd.sh` (backup: `lsd.sh.vehlog.bak`)
- flags, the log and the catalog in `/mnt/persist` (`vehlog_*`, `vehlog.log`, `vehlog_catalog.txt`, written only when something changed, at most once a minute)
- `/tmp/vehlog_speed` and `/tmp/vehlog_fuel` (RAM): values plus a write counter for the cockpit overlay
- while `/mnt/persist/vehlog_street_blank` exists and the CarPlay map is up: the street name the navigation
  sends to the cluster goes out as a single space (wrapper around the navigation's `DSIKOMONavInfo`), see
  [docs/13](../../docs/13-cockpit-overlay.md)
- optional TOUCHCURSOR and JOKERBTN re-arm: deletes `/mnt/persist/touchcursor_disabled` /
  `jokerbtn_disabled` after a clean start, at most 3 times in a row (counters `vehlog_tc_rearms`,
  `vehlog_jb_rearms`), see [docs/10](../../docs/10-star-button-and-touchpad-in-carplay.md)
