package local.vehlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * VehLog 1.0: fuel left for the overlay's right box, counted by VehLog itself.
 *
 * The car does not send the tank content (BCTankLevel is always invalid), and in our logs
 * the range came only once per start and was the same (570 km) for days, so range x
 * consumption alone stays frozen. So:
 *  - Start value and corrections come from the car: range km x long term l/100km.
 *  - While driving, fuel used is counted down: distance (speedometer speed, minus
 *    SPEEDO_FACTOR for the speedometer reading high) x short term l/100km (long term
 *    when there is no short term value).
 *  - A car value is used only when the range is a new number (not the same as the last
 *    one we saw, also across restarts, so a stale value after a start changes nothing), and
 *    only when it is lower (more exact), or at least REFUEL_ML higher (refuelled). A value
 *    a little higher is ignored, so the number never goes up while driving.
 *  - Never more than TANK_ML (A6 C7 tank about 73 to 75 l), never below 0.
 *  - Saved in /mnt/persist/vehlog_fuel_state ("<ml> <last car range>") at most once a
 *    minute, and only when it changed, so it goes on after a restart.
 */
final class Fuel {
    static final String STATE = "/mnt/persist/vehlog_fuel_state";
    private static final String TMP = "/mnt/persist/vehlog_fuel_state.tmp";
    static final int TANK_ML = 74000;
    static final int REFUEL_ML = 5000;
    static final double SPEEDO_FACTOR = 0.96;
    static final long MAX_STEP_MS = 10000L;
    private static final long SAVE_MS = 60000L;

    static double ml = -1;          // estimate in millilitres, -1 = unknown
    static int carRange = -1;       // the last range number seen from the car
    private static long lastStep;
    private static long lastSave;
    private static long savedMl = -2;
    private static int savedRange = -2;
    static double drivenKm;
    static double usedMl;

    private Fuel() {
    }

    static void load() {
        try {
            FileInputStream in = new FileInputStream(STATE);
            String s;
            try {
                byte[] b = new byte[64];
                int len = in.read(b);
                s = len > 0 ? new String(b, 0, len, "ISO-8859-1").trim() : "";
            } finally {
                in.close();
            }
            int sp = s.indexOf(' ');
            int m = Integer.parseInt(sp > 0 ? s.substring(0, sp) : s);
            int r = sp > 0 ? Integer.parseInt(s.substring(sp + 1).trim()) : -1;
            if (m >= 0 && m <= TANK_ML && r >= -1 && r <= 3000) {
                ml = m;
                carRange = r;
                savedMl = m;
                savedRange = r;
                VehLog.line("fuel: saved estimate " + litresText() + " l, last car range " + r + " km");
            } else {
                VehLog.line("fuel: saved state not plausible, ignored: " + s);
            }
        } catch (java.io.FileNotFoundException e) {
            VehLog.line("fuel: no saved state yet, waiting for the car's range");
        } catch (Throwable t) {
            VehLog.line("fuel: saved state not readable, ignored: " + t);
        }
    }

    /** Range (km) and long term consumption (l/100km x10) from the car, -1 = not valid. */
    static void car(int range, int consLong) {
        if (range < 0 || consLong <= 0 || range == carRange) {
            return;
        }
        int prev = carRange;
        carRange = range;
        int carMl = range * consLong;            // km x l/100km x10 = ml
        if (carMl > TANK_ML) {
            carMl = TANK_ML;
        }
        String car = "car " + range + " km x " + (consLong / 10) + "." + (consLong % 10) + " l = "
                + (carMl / 1000) + "." + ((carMl % 1000) / 100) + " l (range before " + prev + ")";
        if (ml < 0) {
            ml = carMl;
            VehLog.line("fuel: start value from the " + car);
        } else if (carMl >= ml + REFUEL_ML) {
            VehLog.line("fuel: refuelled, " + litresText() + " l -> " + car);
            ml = carMl;
        } else if (carMl < ml) {
            VehLog.line("fuel: lower value from the " + car + ", was " + litresText() + " l");
            ml = carMl;
        } else {
            VehLog.line("fuel: " + car + " ignored, it would go up (" + litresText() + " l kept)");
        }
    }

    /** Called about every 2 s: counts down what was used since the last call. */
    static void step(long now, int speedKmh, int consShort, int consLong) {
        long dt = lastStep == 0L ? 0L : now - lastStep;
        lastStep = now;
        if (ml < 0 || dt <= 0L || dt > MAX_STEP_MS || speedKmh <= 0) {
            return;
        }
        int c = consShort > 0 ? consShort : consLong;
        if (c <= 0) {
            return;
        }
        double km = speedKmh * SPEEDO_FACTOR * dt / 3600000.0;
        double used = km * c;
        drivenKm += km;
        usedMl += used;
        ml -= used;
        if (ml < 0) {
            ml = 0;
        }
    }

    /** Whole litres for the overlay, -1 = unknown. */
    static int litres() {
        double m = ml;
        if (m < 0) {
            return -1;
        }
        int l = (int) ((m + 500.0) / 1000.0);
        return l <= TANK_ML / 1000 ? l : TANK_ML / 1000;
    }

    static String litresText() {
        double m = ml;
        if (m < 0) {
            return "?";
        }
        long d = Math.round(m / 100.0);
        return (d / 10) + "." + (d % 10);
    }

    static String stats() {
        long km = Math.round(drivenKm * 10.0);
        long l = Math.round(usedMl / 100.0);
        return "fuel: " + litresText() + " l, counted this start " + (km / 10) + "." + (km % 10) + " km and "
                + (l / 10) + "." + (l % 10) + " l, last car range " + carRange;
    }

    /** At most once a minute (always when force), only when something changed. */
    static void save(long now, boolean force) {
        if (ml < 0 || (!force && now - lastSave < SAVE_MS)) {
            return;
        }
        long m = Math.round(ml);
        if (Math.abs(m - savedMl) < 50L && carRange == savedRange) {
            return;
        }
        lastSave = now;
        try {
            FileOutputStream out = new FileOutputStream(TMP);
            try {
                out.write((m + " " + carRange + "\n").getBytes("ISO-8859-1"));
            } finally {
                out.close();
            }
            File f = new File(STATE);
            if (!new File(TMP).renameTo(f)) {
                f.delete();
                new File(TMP).renameTo(f);
            }
            savedMl = m;
            savedRange = carRange;
        } catch (Throwable t) {
            VehLog.line("fuel: save failed: " + t);
        }
    }
}
