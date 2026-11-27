package harness;
import java.lang.reflect.*;
import java.io.*;
import org.dsi.ifc.global.*;
/**
 * PC test of the fuel estimate (VehLog 1.1, class Fuel): start value from the car, repeated
 * values ignored, follows the car, counted down while driving, shown never up except after refuelling, 74 l
 * cap, saved and loaded across restarts, and /tmp/vehlog_fuel for the overlay.
 */
public class FuelTest {
    static Class<?> v, f;
    static int fails;
    static void check(boolean ok, String label) { if (!ok) fails++; System.out.println((ok ? "ok   " : "FAIL ") + label); }
    static void expect(String label, Object got, Object want) { boolean ok = String.valueOf(want).equals(String.valueOf(got)); if (!ok) fails++; System.out.println((ok ? "ok   " : "FAIL ") + label + ": [" + got + "] want [" + want + "]"); }
    static Object call(Class<?> c, String n, Object... a) throws Exception {
        for (Method m : c.getDeclaredMethods()) if (m.getName().equals(n) && m.getParameterTypes().length == a.length) { m.setAccessible(true); return m.invoke(null, a); }
        throw new RuntimeException("no " + n);
    }
    static void set(String n, Object val) throws Exception { Field x = f.getDeclaredField(n); x.setAccessible(true); x.set(null, val); }
    static Object get(String n) throws Exception { Field x = f.getDeclaredField(n); x.setAccessible(true); return x.get(null); }
    static void reset() throws Exception {
        set("ml", -1.0); set("shown", -1.0); set("carRange", -1); set("lastStep", 0L); set("lastSave", 0L);
        set("savedShown", -2L); set("savedMl", -2L); set("savedRange", -2);
        set("drivenKm", 0.0); set("usedMl", 0.0);
    }
    static int litres() throws Exception { return (Integer) call(f, "litres"); }
    static String lt() throws Exception { return (String) call(f, "litresText"); }
    static void car(int r, int c) throws Exception { call(f, "car", r, c); }
    /** drives for the given minutes at kmh, one step every 2 s */
    static long drive(long t, int minutes, int kmh, int shortC, int longC) throws Exception {
        for (int i = 0; i < minutes * 30; i++) { t += 2000L; call(f, "step", t, kmh, shortC, longC); }
        return t;
    }
    static String overlayFile() throws Exception {
        call(v, "writeFuel");
        BufferedReader b = new BufferedReader(new FileReader("/tmp/vehlog_fuel")); String s = b.readLine(); b.close();
        String[] t = s.split(" ");
        return t.length == 3 && Integer.parseInt(t[2]) > 0 ? t[0] + " " + t[1] : "BAD FORMAT " + s;
    }
    public static void main(String[] a) throws Exception {
        new File("/tmp").mkdirs(); new File("/mnt/persist").mkdirs();
        String state = "/mnt/persist/vehlog_fuel_state";
        new File(state).delete();
        v = Class.forName("local.vehlog.VehLog"); f = Class.forName("local.vehlog.Fuel");
        reset();

        call(f, "load");
        expect("no saved state: unknown", litres(), -1);
        expect("overlay before any value", overlayFile(), "-1 -1");
        car(570, 87);
        expect("start value 570 km x 8.7", lt(), "49.6");
        expect("whole litres", litres(), 50);
        car(570, 87);
        expect("same range again (stale value): unchanged", lt(), "49.6");
        car(-1, 87); car(560, -1);
        expect("invalid car values: unchanged", lt(), "49.6");

        long t = 1000000L;
        call(f, "step", t, 100, 80, 87);   // first step only sets the clock
        t = drive(t, 60, 100, 80, 87);       // 1 h at 100 on the speedometer = 96 km, 8.0 l/100km
        expect("1 h at 100 km/h, short term 8.0: 96 km x 8.0 = 7.68 l used", lt(), "41.9");
        double km = (Double) get("drivenKm");
        check(Math.abs(km - 96.0) < 0.01, "counted 96 km (speedometer minus 4 %), got " + km);
        t = drive(t, 30, 50, -1, 87);        // no short term value: long term 8.7
        expect("30 min at 50 km/h with long term 8.7: 24 km x 8.7 = 2.09 l", lt(), "39.8");
        t = drive(t, 10, 0, 80, 87);
        expect("standing still: nothing counted", lt(), "39.8");
        t += 60000L; call(f, "step", t, 100, 80, 87);
        expect("gap over 10 s (MMI busy): that step not counted", lt(), "39.8");

        car(500, 87);                        // 43.5 l: higher, but less than 5 l
        expect("car value a little higher: ignored, never goes up", lt(), "39.8");
        car(400, 87);                        // 34.8 l
        expect("car value lower: taken", lt(), "34.8");
        car(410, 87);                        // 35.7 l
        expect("then a little higher again: shown stays", lt(), "34.8");
        expect("  but the value behind it follows the car", Math.round((Double) get("ml")), 35670L);
        t = drive(t, 10, 60, 80, 87);        // 9.6 km x 8.0 = 0.77 l: 35.67 -> 34.90, still above 34.8
        expect("  counted down, still above what is shown: shown stays", lt(), "34.8");
        t = drive(t, 5, 60, 80, 87);         // 4.8 km x 8.0 = 0.38 l: 34.90 -> 34.52
        expect("  counted below what was shown: shown follows again", lt(), "34.5");
        car(400, 87);                        // 34.8 l, a little higher than shown
        expect("car value again a little higher: shown stays", lt(), "34.5");

        // real drive from 27 Nov: the car's range goes 390 -> 380 -> 390 -> 380, the shown
        // value must end close to the car (380 km x 8.7 = 33.06 l), not below it
        reset(); car(390, 87); t = 1000000L; call(f, "step", t, 50, 90, 87);
        t = drive(t, 12, 50, 90, 87);        // 9.6 km x 9.0 = 0.86 l: 33.93 -> 33.07
        car(380, 87);                        // 33.06 l, lower: taken
        expect("drive: range 380", lt(), "33.1");
        t = drive(t, 5, 50, 90, 87);         // 4 km x 9.0 = 0.36 l -> 32.70
        car(390, 87);                        // 33.93 l, a little higher
        expect("drive: range up to 390, shown stays", lt(), "32.7");
        t = drive(t, 10, 50, 90, 87);        // 8 km x 9.0 = 0.72 l: 33.93 -> 33.21
        car(380, 87);                        // 33.06 l, still above shown
        expect("drive: range back to 380, shown stays", lt(), "32.7");
        t = drive(t, 3, 50, 90, 87);         // 2.4 km x 9.0 = 0.22 l: 33.06 -> 32.84
        expect("drive: still held", lt(), "32.7");
        t = drive(t, 3, 50, 90, 87);         // -> 32.63
        expect("drive: then follows the car again", lt(), "32.6");
        reset(); car(420, 87); t = 1000000L; call(f, "step", t, 100, 80, 87);
        t = drive(t, 60, 100, 80, 87);       // 36.54 l - 7.68 = 28.86
        car(330, 87);                        // 28.71 l
        expect("a normal lower value is not a refuel", lt(), "28.7");
        car(400, 87);                        // 34.8 l = shown + 6.1
        expect("5 l or more above shown: refuelled", lt(), "34.8");

        car(850, 87);                        // 73.95 l, refuelled
        expect("refuelled (range jumps to 850): taken", lt(), "74.0");
        expect("whole litres after refuel", litres(), 74);
        car(1000, 87);                       // 87 l, cap 74
        expect("car value over the tank size: stays at 74", litres(), 74);
        reset(); car(1000, 87);
        expect("start value over the tank size: capped at 74", lt(), "74.0");

        reset(); car(30, 87);                // 2.6 l
        t = drive(1000000L, 60, 100, 80, 87);
        expect("never below 0", lt(), "0.0");

        // overlay file: long term consumption and whole litres
        reset(); car(570, 87);
        Method cn = v.getDeclaredMethod("consNow", CarBCConsumption.class, int.class); cn.setAccessible(true);
        cn.invoke(null, new CarBCConsumption(1, 8.7f, 0), 1);
        expect("overlay file", overlayFile(), "87 50");
        cn.invoke(null, new CarBCConsumption(0, 409.5f, 0), 1);
        expect("consumption state 0 (409.5): not shown", overlayFile(), "-1 50");
        cn.invoke(null, null, 2);
        Method rn = v.getDeclaredMethod("rangeNow", CarBCCurrentRange.class, int.class); rn.setAccessible(true);
        Field rk = v.getDeclaredField("rangeKm"); rk.setAccessible(true);
        rn.invoke(null, new CarBCCurrentRange(1, 400, 1), 1);
        expect("range in miles: not valid", rk.get(null), -1);
        rn.invoke(null, new CarBCCurrentRange(1, 670, 0), 2);
        expect("range with invalid flag: not valid", rk.get(null), -1);
        rn.invoke(null, new CarBCCurrentRange(1, 670, 0), 1);
        expect("range valid", rk.get(null), 670);
        Method sn = v.getDeclaredMethod("shortNow", CarBCConsumption.class, int.class); sn.setAccessible(true);
        Field sk = v.getDeclaredField("consShortTenths"); sk.setAccessible(true);
        sn.invoke(null, new CarBCConsumption(1, 10.4f, 0), 1);
        expect("short term 10.4", sk.get(null), 104);
        sn.invoke(null, new CarBCConsumption(1, 0f, 0), 1);
        expect("short term 0 (just started): not used", sk.get(null), -1);

        // save and load across a restart
        reset(); car(570, 87); drive(1000000L, 15, 60, 90, 87);
        String before = lt();
        call(f, "save", 5000000L, true);
        BufferedReader b = new BufferedReader(new FileReader(state)); String line = b.readLine(); b.close();
        System.out.println("     state file: " + line);
        reset(); call(f, "load");
        expect("after restart: same estimate", lt(), before);
        expect("after restart: last car range kept", get("carRange"), 570);
        car(570, 87);
        expect("after restart the car sends the old 570 again: ignored", lt(), before);
        reset(); car(570, 87); drive(1000000L, 15, 60, 90, 87); car(560, 87);   // held: shown below the car
        String hs = lt(); long hm = Math.round((Double) get("ml"));
        check(hm > Math.round((Double) get("shown")), "held state before the restart");
        call(f, "save", 5000000L, true);
        reset(); call(f, "load");
        expect("after restart while held: shown kept", lt(), hs);
        expect("after restart while held: value behind it kept", Math.round((Double) get("ml")), hm);
        FileOutputStream o = new FileOutputStream(state); o.write("31700 380\n".getBytes()); o.close();
        reset(); call(f, "load");
        expect("state file from 1.0 (two numbers): shown", lt(), "31.7");
        expect("state file from 1.0: value behind it the same", Math.round((Double) get("ml")), 31700L);
        expect("state file from 1.0: car range", get("carRange"), 380);
        car(380, 87);
        expect("state file from 1.0, car sends 380 again: ignored", lt(), "31.7");
        o = new FileOutputStream(state); o.write("31700 380 99999\n".getBytes()); o.close();
        reset(); call(f, "load");
        expect("state file with a third number over 74 l: ignored", litres(), -1);
        o = new FileOutputStream(state); o.write("99999 570\n".getBytes()); o.close();
        reset(); call(f, "load");
        expect("state file with more than 74 l: ignored", litres(), -1);
        o = new FileOutputStream(state); o.write("abc".getBytes()); o.close();
        reset(); call(f, "load");
        expect("broken state file: ignored", litres(), -1);
        new File(state).delete();

        // save only when changed, at most once a minute
        reset(); car(570, 87);
        call(f, "save", 100000L, false);
        check(new File(state).exists(), "first save writes the file");
        new File(state).delete();
        call(f, "save", 120000L, false);
        check(!new File(state).exists(), "no second save within a minute");
        call(f, "save", 200000L, false);
        check(!new File(state).exists(), "no save when nothing changed");
        drive(1000000L, 5, 50, 80, 87);
        call(f, "save", 300000L, false);
        check(new File(state).exists(), "save again after driving");
        new File(state).delete();

        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
    }
}
