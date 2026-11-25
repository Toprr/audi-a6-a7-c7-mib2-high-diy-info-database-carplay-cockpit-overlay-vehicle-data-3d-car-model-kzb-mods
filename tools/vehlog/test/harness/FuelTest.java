package harness;
import java.lang.reflect.*;
import java.io.*;
import org.dsi.ifc.global.*;
/** PC test of the 0.7 fuel estimate: range and consumption in, /tmp/vehlog_fuel out. */
public class FuelTest {
    static Class<?> v;
    static String run(CarBCCurrentRange r, int rv, CarBCConsumption c, int cv) throws Exception {
        Method rn = v.getDeclaredMethod("rangeNow", CarBCCurrentRange.class, int.class); rn.setAccessible(true);
        Method cn = v.getDeclaredMethod("consNow", CarBCConsumption.class, int.class); cn.setAccessible(true);
        Method wf = v.getDeclaredMethod("writeFuel"); wf.setAccessible(true);
        rn.invoke(null, r, rv); cn.invoke(null, c, cv); wf.invoke(null);
        BufferedReader b = new BufferedReader(new FileReader("/tmp/vehlog_fuel")); String s = b.readLine(); b.close();
        String[] t = s.split(" ");
        if (t.length != 3 || Integer.parseInt(t[2]) <= 0) return "BAD FORMAT " + s;   // 0.8: counter last
        return t[0] + " " + t[1];
    }
    static int fails;
    static void expect(String label, String got, String want) { boolean ok = want.equals(got); if (!ok) fails++; System.out.println((ok ? "ok   " : "FAIL ") + label + ": [" + got + "] want [" + want + "]"); }
    public static void main(String[] a) throws Exception {
        new File("/tmp").mkdirs();
        v = Class.forName("local.vehlog.VehLog");
        expect("car values 670 km, 8.7", run(new CarBCCurrentRange(1, 670, 0), 1, new CarBCConsumption(1, 8.7f, 0), 1), "87 58");
        expect("740 km, 8.7", run(new CarBCCurrentRange(1, 740, 0), 1, new CarBCConsumption(1, 8.7f, 0), 1), "87 64");
        expect("range invalid flag", run(new CarBCCurrentRange(1, 670, 0), 2, new CarBCConsumption(1, 8.7f, 0), 1), "87 -1");
        expect("range null", run(null, 2, new CarBCConsumption(1, 8.7f, 0), 1), "87 -1");
        expect("range in miles", run(new CarBCCurrentRange(1, 400, 1), 1, new CarBCConsumption(1, 8.7f, 0), 1), "87 -1");
        expect("consumption state 0 (409.5)", run(new CarBCCurrentRange(1, 670, 0), 1, new CarBCConsumption(0, 409.5f, 0), 1), "-1 -1");
        expect("consumption other unit", run(new CarBCCurrentRange(1, 670, 0), 1, new CarBCConsumption(1, 27f, 1), 1), "-1 -1");
        expect("consumption null", run(new CarBCCurrentRange(1, 670, 0), 1, null, 2), "-1 -1");
        expect("range 0 (empty)", run(new CarBCCurrentRange(1, 0, 0), 1, new CarBCConsumption(1, 9.4f, 0), 1), "94 0");
        expect("rounding 55 km, 9.5", run(new CarBCCurrentRange(1, 55, 0), 1, new CarBCConsumption(1, 9.5f, 0), 1), "95 5");
        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
    }
}
