package local.vehlog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import org.dsi.ifc.base.DSIBase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * VehLog: a read-only logger for oil temperature, coolant temperature and speed
 * on MHI2_ER_AU57x_K3663_1. It only listens (DSI setNotification), it never
 * sends anything to the car. Everything runs in one low-priority daemon thread,
 * every step is inside try/catch Throwable, and any failure just stops the logger.
 *
 * Started once by the replaced org.dsi.ifc.global.CarViewOption.
 * Boot guard: lsd.sh leaves /mnt/persist/vehlog_pending before the HMI starts;
 * this class deletes it after the HMI has run for a while. A flag still there on
 * the next start means that start failed, and lsd.sh no longer loads the jar.
 * Off switch until the next start: touch /tmp/vehlog-off
 *
 * Java 1.2 level for the unit's J9: no StringBuilder, no boxing, no class literals.
 */
public final class VehLog implements Runnable {
    static final String VERSION = "1.1";
    private static final long LISTEN_TRY_MS = 15L * 60L * 1000L;  // 1.0: look for the listen services this long
    private static final String FLAG_PENDING = "/mnt/persist/vehlog_pending";
    private static final String FLAG_LOADED = "/mnt/persist/vehlog_loaded";
    private static final String OFF = "/tmp/vehlog-off";
    private static final String LOG = "/mnt/persist/vehlog.log";
    /* 0.3: current speedometer value for the cockpit speed overlay (stream-player) */
    private static final String SPEED_FILE = "/tmp/vehlog_speed";
    /* 0.7: long term consumption and estimated fuel for the overlay's right box */
    private static final String FUEL_FILE = "/tmp/vehlog_fuel";
    private static final long MAX_BYTES = 524288L;          // stop writing after 512 KB
    private static final long CONFIRM_AFTER_MS = 90000L;     // boot guard: HMI ran 90 s
    private static final long FIRST_WAIT_MS = 20000L;        // let the HMI start first

    private static final String DSI_KOMBI = "org.dsi.ifc.carkombi.DSICarKombi";
    private static final String DSI_VEHICLE = "org.dsi.ifc.carvehiclestates.DSICarVehicleStates";
    private static final int ATTR_BCVIEWOPTIONS = 4;
    private static final int ATTR_BCOILTEMPERATURE = 30;
    private static final int ATTR_BCCOOLANTTEMPERATURE = 63;
    private static final int ATTR_BCOILTEMPERATUREVALUE = 64;
    private static final int ATTR_BCCURRENTRANGE1 = 8;
    private static final int ATTR_BCLONGTERMAVERAGECONSUMPTION1 = 14;
    private static final int ATTR_BCSHORTTERMAVERAGECONSUMPTION1 = 11;   // 1.0: for counting fuel
    private static final int ATTR_DYNAMICVEHICLEINFOHIGHFREQUENT = 14;

    private static final Object LOCK = new Object();
    private static boolean started;

    // Written by the listener callbacks (DSI threads), read by the logger thread
    static volatile String oil = "-";
    static volatile String coolant = "-";
    static volatile String speed = "-";
    static volatile String realSpeed = "-";
    static volatile int rpm = -1;
    static volatile String oilView = "-";
    static volatile String oilSetting = "-";
    static volatile int viewCount;
    static volatile int oilCount;
    static volatile int coolantCount;
    static volatile int dynCount;
    static volatile String lastError = null;

    private static final StringBuffer pending = new StringBuffer();
    private static long written;
    private static boolean full;

    private VehLog() {
    }

    /** Called from CarViewOption's constructors. Starts the thread once, never throws. */
    public static void kick() {
        try {
            synchronized (LOCK) {
                if (started) {
                    return;
                }
                started = true;
            }
            if (new File(OFF).exists()) {
                return;
            }
            Thread t = new Thread(new VehLog(), "VehLog");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        } catch (Throwable t) {
            // nothing, the HMI goes on as stock
        }
    }

    public void run() {
        try {
            long t0 = System.currentTimeMillis();
            sleep(FIRST_WAIT_MS);
            line("start VehLog " + VERSION);
            Fuel.load();
            boolean kombi = false;
            boolean vehicle = false;
            boolean confirmed = false;
            int tries = 0;
            int catalogTries = 0;
            int streetTries = 0;
            boolean streetStarted = false;
            String last = "";
            long lastLine = 0L;
            long lastStats = 0L;
            while (true) {
                if (new File(OFF).exists()) {
                    line("off switch found, stopping");
                    Catalog.write(true);
                    Listen.shutdown();
                    Fuel.save(System.currentTimeMillis(), true);
                    flush();
                    return;
                }
                long now = System.currentTimeMillis();
                if (!confirmed && now - t0 >= CONFIRM_AFTER_MS) {
                    confirmed = true;
                    confirm();
                }
                if ((!kombi || !vehicle) && tries < 60) {
                    tries++;
                    BundleContext bc = bundleContext();
                    if (bc != null) {
                        if (!kombi) {
                            kombi = subscribe(bc, DSI_KOMBI,
                                    new int[] {ATTR_BCVIEWOPTIONS, ATTR_BCOILTEMPERATURE, ATTR_BCCOOLANTTEMPERATURE, ATTR_BCOILTEMPERATUREVALUE,
                                            ATTR_BCCURRENTRANGE1, ATTR_BCLONGTERMAVERAGECONSUMPTION1,
                                            ATTR_BCSHORTTERMAVERAGECONSUMPTION1},
                                    new KombiListener());
                        }
                        if (!vehicle) {
                            vehicle = subscribe(bc, DSI_VEHICLE,
                                    new int[] {ATTR_DYNAMICVEHICLEINFOHIGHFREQUENT},
                                    new VehicleListener());
                        }
                    }
                    if (tries == 60 && (!kombi || !vehicle)) {
                        line("gave up subscribing: kombi=" + kombi + " vehicle=" + vehicle);
                    }
                }
                // 0.4: catalog of every vehicle DSI, after the speed path is up and before the
                // boot guard is confirmed (so a problem with it still trips the guard)
                if (kombi && vehicle && catalogTries < 30) {
                    catalogTries++;
                    BundleContext bc = bundleContext();
                    if (bc != null && Catalog.subscribeAll(bc) >= Catalog.SERVICES.length) {
                        catalogTries = 30;
                    }
                }
                // 0.6: blank street name on the cockpit while CarPlay is up (switchable)
                if (kombi && vehicle && !streetStarted && streetTries < 30) {
                    streetTries++;
                    BundleContext bc = bundleContext();
                    if (bc != null) {
                        streetStarted = StreetBlank.start(bc);
                    }
                }
                // 1.0: listen only (BAP, ExLAP service list, ExLAP domain state), looking for
                // a source of the oil temperature. Before the boot guard confirm, like the catalog.
                if (kombi && vehicle) {
                    Listen.tick(Listen.needsContext() && now - t0 < LISTEN_TRY_MS ? bundleContext() : null, now);
                }
                Catalog.write(false);
                String err = lastError;
                if (err != null) {
                    lastError = null;
                    line("listener error: " + err);
                }
                String values = "oil=" + oil + " coolant=" + coolant + " speed=" + speed
                        + " realSpeed=" + realSpeed + " rpm=" + rpm + " | " + oilView + " bcOil=" + oilSetting
                        + " | range=" + rangeKm + " cons=" + consTenths + " short=" + consShortTenths + " fuelL=" + fuelLitres();
                if (!values.equals(last) || now - lastLine >= 30000L) {
                    last = values;
                    lastLine = now;
                    line(values);
                }
                if (now - lastStats >= 60000L) {
                    lastStats = now;
                    line("updates so far: oil=" + oilCount + " coolant=" + coolantCount + " dyn=" + dynCount + " view=" + viewCount);
                    line(StreetBlank.stats());
                    line(Listen.stats());
                    line(Fuel.stats());
                    flush();
                }
                writeSpeed();
                // 1.0: fuel counted by VehLog (see Fuel), car values only when they make sense
                Fuel.car(rangeKm, consTenths);
                Fuel.step(now, speedKmh, consShortTenths, consTenths);
                Fuel.save(now, false);
                writeFuel();
                if (pending.length() > 2048) {
                    flush();
                }
                sleep(2000L);
            }
        } catch (Throwable t) {
            try {
                line("stopped: " + t);
                flush();
            } catch (Throwable ignored) {
                // nothing
            }
        }
    }

    /** IFrameworkAccess of the car app, through reflection so a missing method is just an exception. */
    private static BundleContext bundleContext() {
        try {
            Class menuCtrl = Class.forName("de.audi.app.car.MenuCtrl");
            Object fw = menuCtrl.getMethod("getFramework", new Class[0]).invoke(null, new Object[0]);
            if (fw == null) {
                return null;
            }
            Class access = Class.forName("de.audi.atip.base.IFrameworkAccess");
            Method m = access.getMethod("getBundleCxt", new Class[0]);
            Object bc = m.invoke(fw, new Object[0]);
            return (BundleContext) bc;
        } catch (Throwable t) {
            note("no framework yet: " + t);
            return null;
        }
    }

    private static boolean subscribe(BundleContext bc, String name, int[] attrs, Object listener) {
        try {
            ServiceReference ref = bc.getServiceReference(name);
            if (ref == null) {
                return false;
            }
            Object service = bc.getService(ref);
            if (!(service instanceof DSIBase)) {
                return false;
            }
            ((DSIBase) service).setNotification(attrs, (org.dsi.ifc.base.DSIListener) listener);
            line("subscribed " + name);
            return true;
        } catch (Throwable t) {
            line("subscribe " + name + " failed: " + t);
            return false;
        }
    }

    private static void confirm() {
        try {
            new File(FLAG_PENDING).delete();
            FileOutputStream out = new FileOutputStream(FLAG_LOADED);
            out.close();
            line("boot guard confirmed");
        } catch (Throwable t) {
            line("boot guard confirm failed: " + t);
        }
        rearm("TOUCHCURSOR", "/mnt/persist/touchcursor", TC_REARMS);
        rearm("JOKERBTN", "/mnt/persist/jokerbtn", JB_REARMS);
    }

    /*
     * 0.2: MIB2-Patcher's TOUCHCURSOR boot guard turns the patch off when the MMI is
     * switched off or restarted within a minute of a start, even when nothing was wrong.
     * This HMI start ran fine for CONFIRM_AFTER_MS, so if TOUCHCURSOR is disabled we
     * remove its disabled flag (what its own "install" does with the flags) and it loads
     * again on the next start. At most MAX_REARMS times in a row: if TOUCHCURSOR really
     * broke the start, it stays off after that. The count goes back to 0 as soon as
     * TOUCHCURSOR has had a good start (no pending and no disabled flag any more).
     * 0.9: the same for MIB2-Patcher's JOKERBTN, which has the same kind of boot guard
     * (flags /mnt/persist/jokerbtn_pending and _disabled) and was found switched off too.
     */
    private static final String TC_REARMS = "/mnt/persist/vehlog_tc_rearms";
    private static final String JB_REARMS = "/mnt/persist/vehlog_jb_rearms";
    private static final int MAX_REARMS = 3;

    private static void rearm(String name, String flag, String countFile) {
        try {
            String disabledFlag = flag + "_disabled";
            boolean disabled = new File(disabledFlag).exists();
            int n = readInt(countFile);
            if (!disabled) {
                if (n != 0 && !new File(flag + "_pending").exists()) {
                    writeInt(countFile, 0);
                    line(name + " ok again, re-arm count reset");
                }
                return;
            }
            if (n >= MAX_REARMS) {
                line(name + " is disabled and was re-armed " + n + " times in a row, leaving it off");
                return;
            }
            writeInt(countFile, n + 1);
            if (new File(disabledFlag).delete()) {
                line(name + " was disabled by its boot guard, re-armed (" + (n + 1) + "/" + MAX_REARMS + "), active after the next start");
            } else {
                line(name + " re-arm failed, could not delete " + disabledFlag);
            }
        } catch (Throwable t) {
            line(name + " re-arm error: " + t);
        }
    }

    private static int readInt(String path) {
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(path);
            try {
                byte[] b = new byte[16];
                int len = in.read(b);
                return len > 0 ? Integer.parseInt(new String(b, 0, len, "ISO-8859-1").trim()) : 0;
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return 0;
        }
    }

    private static void writeInt(String path, int v) throws java.io.IOException {
        FileOutputStream out = new FileOutputStream(path);
        try {
            out.write(String.valueOf(v).getBytes("ISO-8859-1"));
        } finally {
            out.close();
        }
    }

    private static String lastNote = null;

    /** Like line(), but the same text only once in a row. */
    private static void note(String s) {
        if (!s.equals(lastNote)) {
            lastNote = s;
            line(s);
        }
    }

    static void line(String s) {
        synchronized (pending) {
            pending.append(System.currentTimeMillis() / 1000L).append(' ').append(s).append('\n');
        }
    }

    private static void flush() {
        String data;
        synchronized (pending) {
            data = pending.toString();
            pending.setLength(0);
        }
        if (full || data.length() == 0) {
            return;
        }
        try {
            byte[] b = data.getBytes("ISO-8859-1");
            if (written == 0L) {
                written = new File(LOG).length();
            }
            if (written + b.length > MAX_BYTES) {
                full = true;
                b = (System.currentTimeMillis() / 1000L + " log full, stopped writing\n").getBytes("ISO-8859-1");
            }
            FileOutputStream out = new FileOutputStream(LOG, true);
            try {
                out.write(b);
            } finally {
                out.close();
            }
            written += b.length;
        } catch (Throwable t) {
            // logging is best effort
        }
    }

    // 0.3: speed for the overlay. -1 = no valid value. Written on every change
    // (from the DSI thread, a few bytes to /tmp, which is RAM) and every 2 s from
    // the logger thread, so the player can tell a fresh value from an old one.
    static volatile int speedKmh = -1;
    private static int speedWritten = -2;
    private static final Object SPEED_LOCK = new Object();

    static void speedNow(org.dsi.ifc.global.CarBCSpeed s) {
        int v = -1;
        if (s != null && s.speedValueState == 1 && s.speedUnit == 0 && s.speedValue >= 0f && s.speedValue < 400f) {
            v = Math.round(s.speedValue);
        }
        speedKmh = v;
        if (v != speedWritten) {
            writeSpeed();
        }
    }

    private static void writeSpeed() {
        try {
            synchronized (SPEED_LOCK) {
                int v = speedKmh;
                if (v < 0) {
                    speedWritten = v;
                    return;
                }
                // 0.5: written in place. 0.4 wrote a .tmp file and renamed it, and on the
                // car /tmp/vehlog_speed never appeared (rename on the MMX's /tmp most
                // likely not supported). The player tolerates a half-written file.
                FileOutputStream out = new FileOutputStream(SPEED_FILE);
                try {
                    // 0.8: a counter after the value. The player takes a changed counter
                    // as "VehLog is alive" (file times on the MMX's /tmp were not usable).
                    speedSeq++;
                    out.write((v + " " + speedSeq + "\n").getBytes("ISO-8859-1"));
                } finally {
                    out.close();
                }
                speedWritten = v;
                if (!speedOk) {
                    speedOk = true;
                    line("speed file written: " + SPEED_FILE + " (" + v + " km/h)");
                }
            }
        } catch (Throwable t) {
            if (!speedErr) {
                speedErr = true;
                line("speed file write failed: " + t);
            }
        }
    }

    private static boolean speedOk;
    private static int speedSeq;
    private static int fuelSeq;

    // 0.7: right box of the overlay. The car does not send the tank content in litres
    // (BCTankLevel is always invalid), so it is estimated from the cluster's range and
    // the long term consumption: litres = range km * l/100km / 100. -1 = unknown.
    private static volatile int rangeKm = -1;
    private static volatile int consTenths = -1;
    private static volatile int consShortTenths = -1;
    private static boolean fuelOk;
    private static boolean fuelErr;

    static void rangeNow(org.dsi.ifc.global.CarBCCurrentRange r, int validFlag) {
        int v = -1;
        if (r != null && validFlag == 1 && r.state == 1 && r.currentRangeUnit == 0
                && r.currentRangeValue >= 0 && r.currentRangeValue <= 3000) {
            v = r.currentRangeValue;
        }
        rangeKm = v;
    }

    static void consNow(org.dsi.ifc.global.CarBCConsumption c, int validFlag) {
        consTenths = tenths(c, validFlag);
    }

    /** 1.0: short term average (since the start), used to count the fuel down. */
    static void shortNow(org.dsi.ifc.global.CarBCConsumption c, int validFlag) {
        consShortTenths = tenths(c, validFlag);
    }

    private static int tenths(org.dsi.ifc.global.CarBCConsumption c, int validFlag) {
        if (c != null && validFlag == 1 && c.state == 1 && c.consumptionUnit == 0
                && c.consumptionValue >= 1f && c.consumptionValue <= 50f) {
            return Math.round(c.consumptionValue * 10f);
        }
        return -1;
    }

    static int fuelLitres() {
        return Fuel.litres();
    }

    /** "<consumption in tenths> <litres>", -1 for unknown. Rewritten every 2 s as a heartbeat. */
    private static void writeFuel() {
        try {
            fuelSeq++;
            String text = consTenths + " " + fuelLitres() + " " + fuelSeq + "\n";
            FileOutputStream out = new FileOutputStream(FUEL_FILE);
            try {
                out.write(text.getBytes("ISO-8859-1"));
            } finally {
                out.close();
            }
            if (!fuelOk) {
                fuelOk = true;
                line("fuel file written: " + FUEL_FILE + " (" + text.trim() + ")");
            }
        } catch (Throwable t) {
            if (!fuelErr) {
                fuelErr = true;
                line("fuel file write failed: " + t);
            }
        }
    }
    private static boolean speedErr;

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Throwable t) {
            // nothing
        }
    }
}
