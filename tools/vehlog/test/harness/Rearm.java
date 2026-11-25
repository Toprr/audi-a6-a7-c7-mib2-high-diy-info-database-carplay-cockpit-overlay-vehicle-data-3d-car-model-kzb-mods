package harness;
import java.io.*;
import java.lang.reflect.Method;
/** PC test of the re-arm rules for TOUCHCURSOR and JOKERBTN (flags in C:\mnt\persist). */
public class Rearm {
    static File d = new File("/mnt/persist");
    static int fails;
    static void touch(String n) throws Exception { new FileOutputStream(new File(d, n)).close(); }
    static boolean ex(String n) { return new File(d, n).exists(); }
    static String cnt(String f) { try { BufferedReader r = new BufferedReader(new FileReader(new File(d, f))); String s = r.readLine(); r.close(); return s; } catch (Exception e) { return "none"; } }
    static void check(String what, boolean ok) { System.out.println((ok ? "ok   " : "FAIL ") + what); if (!ok) fails++; }
    static void run(Method m, String name, String flag, String count) throws Exception {
        Object[] args = new Object[] {name, "/mnt/persist/" + flag, "/mnt/persist/" + count};
        new File(d, count).delete(); new File(d, flag + "_disabled").delete(); new File(d, flag + "_pending").delete();
        for (int i = 1; i <= 5; i++) {   // patch really broken: disabled again after every start
            touch(flag + "_disabled");
            m.invoke(null, args);
            boolean still = ex(flag + "_disabled");
            check(name + " broken start " + i + ": " + (i <= 3 ? "re-armed" : "left off") + " (count " + cnt(count) + ")", i <= 3 ? !still : still);
        }
        new File(d, flag + "_disabled").delete(); touch(flag + "_pending");
        m.invoke(null, args);
        check(name + " start in progress (pending): count kept", "3".equals(cnt(count)));
        new File(d, flag + "_pending").delete();
        m.invoke(null, args);
        check(name + " good start: count back to 0", "0".equals(cnt(count)));
        touch(flag + "_disabled"); m.invoke(null, args);
        check(name + " one false disable later: re-armed, count 1", !ex(flag + "_disabled") && "1".equals(cnt(count)));
    }
    public static void main(String[] a) throws Exception {
        Class c = Class.forName("local.vehlog.VehLog");
        Method m = c.getDeclaredMethod("rearm", new Class[] {String.class, String.class, String.class}); m.setAccessible(true);
        Method f = c.getDeclaredMethod("flush", new Class[0]); f.setAccessible(true);
        run(m, "TOUCHCURSOR", "touchcursor", "vehlog_tc_rearms");
        run(m, "JOKERBTN", "jokerbtn", "vehlog_jb_rearms");
        new File(d, "touchcursor_disabled").delete(); new File(d, "jokerbtn_disabled").delete();
        touch("jokerbtn_disabled"); new File(d, "vehlog_jb_rearms").delete(); new File(d, "vehlog_tc_rearms").delete();
        m.invoke(null, new Object[] {"TOUCHCURSOR", "/mnt/persist/touchcursor", "/mnt/persist/vehlog_tc_rearms"});
        m.invoke(null, new Object[] {"JOKERBTN", "/mnt/persist/jokerbtn", "/mnt/persist/vehlog_jb_rearms"});
        check("only Joker disabled: Joker re-armed, TOUCHCURSOR count untouched", !ex("jokerbtn_disabled") && "none".equals(cnt("vehlog_tc_rearms")));
        f.invoke(null, new Object[0]);
        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
    }
}
