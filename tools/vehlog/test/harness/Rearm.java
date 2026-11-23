package harness;
import java.io.*;
import java.lang.reflect.Method;
/** PC test of the TOUCHCURSOR re-arm rules (flags in C:\mnt\persist). */
public class Rearm {
    static File d = new File("/mnt/persist");
    static void touch(String n) throws Exception { new FileOutputStream(new File(d, n)).close(); }
    static boolean ex(String n) { return new File(d, n).exists(); }
    static String cnt() { try { BufferedReader r = new BufferedReader(new FileReader(new File(d, "vehlog_tc_rearms"))); String s = r.readLine(); r.close(); return s; } catch (Exception e) { return "none"; } }
    public static void main(String[] a) throws Exception {
        Class c = Class.forName("local.vehlog.VehLog");
        Method m = c.getDeclaredMethod("rearmTouchCursor", new Class[0]); m.setAccessible(true);
        Method f = c.getDeclaredMethod("flush", new Class[0]); f.setAccessible(true);
        new File(d, "vehlog_tc_rearms").delete(); new File(d, "touchcursor_disabled").delete(); new File(d, "touchcursor_pending").delete();
        for (int i = 1; i <= 5; i++) {   // TOUCHCURSOR really broken: disabled again after every start
            touch("touchcursor_disabled");
            m.invoke(null, new Object[0]);
            System.out.println("broken start " + i + ": disabled still there=" + ex("touchcursor_disabled") + " count=" + cnt());
        }
        new File(d, "touchcursor_disabled").delete(); touch("touchcursor_pending");
        m.invoke(null, new Object[0]); System.out.println("TC start in progress (pending): count=" + cnt());
        new File(d, "touchcursor_pending").delete();
        m.invoke(null, new Object[0]); System.out.println("TC good start: count=" + cnt());
        touch("touchcursor_disabled"); m.invoke(null, new Object[0]);
        System.out.println("one false disable later: disabled still there=" + ex("touchcursor_disabled") + " count=" + cnt());
        f.invoke(null, new Object[0]);
    }
}
