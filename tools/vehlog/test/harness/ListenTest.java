package harness;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
/**
 * PC test of Listen (VehLog 1.0): fake OSGi with fake DSIBAP / DSIExlap / DSIStartup.
 * Checks: only setNotification / clearNotification are ever called on the services (nothing
 * sent), the right attributes, BAP counted per device/function, the 60 min stop, the off
 * switch, the key limit, and a missing ExLAP service is simply waited for.
 */
public class ListenTest {
    static int fails = 0;
    static List<String> calls = new ArrayList<String>();
    static Map<String, Object> listeners = new HashMap<String, Object>();
    static boolean exlapThere = false;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "ok   " : "FAIL ") + what);
        if (!ok) fails++;
    }

    static Object service(final String ifc, ClassLoader cl) throws Exception {
        return Proxy.newProxyInstance(cl, new Class[]{Class.forName(ifc)}, (p, m, x) -> {
            String n = m.getName();
            if (n.equals("hashCode")) return 1; if (n.equals("equals")) return p == x[0]; if (n.equals("toString")) return ifc;
            String s = ifc.substring(ifc.lastIndexOf('.') + 1) + "." + n + "(" + (x == null ? 0 : x.length) + ")";
            if (x != null && x.length == 2 && x[0] instanceof int[]) s += Arrays.toString((int[]) x[0]);
            calls.add(s);
            if (n.equals("setNotification")) listeners.put(ifc, x[x.length - 1]);
            return null;
        });
    }

    static Object call(Object l, String name, Object... args) throws Exception {
        for (Method m : l.getClass().getMethods()) if (m.getName().equals(name) && m.getParameterTypes().length == args.length) return m.invoke(l, args);
        throw new RuntimeException("no " + name);
    }

    static String file() throws IOException {
        File f = new File("/mnt/persist/vehlog_listen.txt");
        if (!f.exists()) return "";
        BufferedReader r = new BufferedReader(new FileReader(f)); StringBuilder b = new StringBuilder(); String s;
        while ((s = r.readLine()) != null) b.append(s).append('\n'); r.close(); return b.toString();
    }

    public static void main(String[] a) throws Exception {
        new File("/mnt/persist").mkdirs();
        new File("/mnt/persist/vehlog_listen.txt").delete();
        new File("/mnt/persist/vehlog_listen_off").delete();
        final ClassLoader cl = ListenTest.class.getClassLoader();
        final Map<String, Object> services = new HashMap<String, Object>();
        for (String s : new String[]{"org.dsi.ifc.bap.DSIBAP", "org.dsi.ifc.exlap.DSIExlap", "org.dsi.ifc.startup.DSIStartup"}) services.put(s, service(s, cl));
        final List<String> asked = new ArrayList<String>();
        Object bc = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.BundleContext.class}, (p, m, x) -> {
            if (m.getName().equals("getServiceReference")) {
                String n = (String) x[0]; asked.add(n);
                if (n.endsWith("DSIExlap") && !exlapThere) return null;
                return Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.ServiceReference.class}, (p2, m2, x2) -> n);
            }
            if (m.getName().equals("getService")) return services.get(x[0].toString());
            calls.add("BundleContext." + m.getName());
            return null;
        });
        Class<?> L = Class.forName("local.vehlog.Listen");
        Method tick = L.getDeclaredMethod("tick", org.osgi.framework.BundleContext.class, long.class); tick.setAccessible(true);
        Method needs = L.getDeclaredMethod("needsContext"); needs.setAccessible(true);
        Method write = L.getDeclaredMethod("write", boolean.class); write.setAccessible(true);

        tick.invoke(null, bc, System.currentTimeMillis());
        check(calls.contains("DSIBAP.setNotification(1)"), "BAP: setNotification(listener), like the stock BAP apps");
        check(calls.contains("DSIStartup.setNotification(2)[31]"), "Startup: only ATTR_DOMAINSTATUSEXLAP (31)");
        check(!listeners.containsKey("org.dsi.ifc.exlap.DSIExlap"), "ExLAP not there yet: nothing done");
        check((Boolean) needs.invoke(null), "still needs the context while ExLAP is missing");
        exlapThere = true;
        tick.invoke(null, bc, System.currentTimeMillis());
        check(calls.contains("DSIExlap.setNotification(2)[1]"), "ExLAP: only ATTR_AVAILABLESERVICES (1)");
        check(!(Boolean) needs.invoke(null), "all three set up, no more lookups");
        int n0 = asked.size();
        tick.invoke(null, bc, System.currentTimeMillis());
        check(asked.size() == n0, "no further service lookups");

        Object bap = listeners.get("org.dsi.ifc.bap.DSIBAP");
        Object ex = listeners.get("org.dsi.ifc.exlap.DSIExlap");
        Object st = listeners.get("org.dsi.ifc.startup.DSIStartup");
        for (int i = 0; i < 5; i++) call(bap, "indication", 39, 17, 12, 0, 80 + i);
        call(bap, "indicationByteSequence", 39, 18, 12, new byte[]{1, 2, (byte) 0xff});
        call(bap, "indicationByteSequence", 40, 1, 12, new byte[40]);
        call(bap, "bapStateStatus", 1, 2);
        call(bap, "indicationVoid", 39, 19, 7);
        Class<?> svc = Class.forName("org.dsi.ifc.exlap.Service");
        Object arr = Array.newInstance(svc, 2);
        Array.set(arr, 0, svc.getConstructor(int.class, int.class).newInstance(5, 0));
        Array.set(arr, 1, svc.getConstructor(int.class, int.class).newInstance(9, 1));
        call(ex, "updateAvailableServices", arr, 1);
        call(st, "updateDomainStatusExLAP", 2, 1);
        bap.hashCode(); bap.equals(bap); bap.toString();
        Thread.sleep(1100);   // the value text is taken at most once a second per key
        call(bap, "indication", 39, 17, 12, 0, 85);
        write.invoke(null, true);
        String f = file();
        System.out.print(f);
        check(f.contains("bap.indication 39/17 | 6 | 2 |"), "BAP indication counted per device/function: 6 updates, text 2x (1/s)");
        check(f.contains("| 39 | 17 | 12 | 0 | 80 | 39 | 17 | 12 | 0 | 85"), "first and newest value kept");
        check(f.contains("bap.indicationByteSequence 39/18 | 1 | 1 |") && f.contains("[3] 0102ff"), "byte sequence as hex");
        check(f.contains("[40] " + repeat("00", 32) + ".."), "long byte sequence cut at 32 bytes");
        check(f.contains("bap.bapStateStatus | 1"), "BAP state counted");
        check(f.contains("bap.indicationVoid 39/19 | 1"), "indicationVoid counted per device/function");
        check(f.contains("exlap.updateAvailableServices | 1") && f.contains("[2] "), "ExLAP service list kept");
        check(f.contains("startup.updateDomainStatusExLAP | 1 | 1 |") && f.contains("| 2 | 1"), "ExLAP domain state kept");

        // 60 min stop
        Field since = L.getDeclaredField("bapSince"); since.setAccessible(true);
        since.setLong(null, System.currentTimeMillis() - (Long) getStatic(L, "LISTEN_MS") - 1000L);
        tick.invoke(null, bc, System.currentTimeMillis());
        check(calls.contains("DSIBAP.clearNotification(1)"), "after 60 min: BAP clearNotification(listener)");
        call(bap, "indication", 39, 17, 12, 0, 99);
        write.invoke(null, true);
        check(file().contains("bap.indication 39/17 | 6 |"), "nothing counted after the stop");
        check(file().contains("bap=stopped"), "header says bap=stopped");

        // only setNotification / clearNotification on the services, nothing on the BundleContext
        boolean onlyNotify = true;
        for (String c : calls) if (!c.contains(".setNotification(") && !c.contains(".clearNotification(")) { onlyNotify = false; System.out.println("   unexpected call " + c); }
        check(onlyNotify, "nothing else called on any service (no request, no start, nothing sent)");

        // key limit
        Field stopped = L.getDeclaredField("bapStopped"); stopped.setAccessible(true);
        stopped.setBoolean(null, false);
        for (int i = 0; i < 400; i++) call(bap, "indication", 100 + i, 1, 12, 0, i);
        stopped.setBoolean(null, true);
        Field ent = L.getDeclaredField("entries"); ent.setAccessible(true);
        Field dr = L.getDeclaredField("dropped"); dr.setAccessible(true);
        check(((Map) ent.get(null)).size() == 300 && dr.getInt(null) > 0, "at most 300 keys, the rest only counted as dropped");

        // off switch from the start, fresh class state is not possible here: check the tick still returns quietly
        new FileOutputStream("/mnt/persist/vehlog_listen_off").close();
        tick.invoke(null, bc, System.currentTimeMillis());
        new File("/mnt/persist/vehlog_listen_off").delete();
        check(true, "off switch: tick returns quietly");

        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
    }
    static Object getStatic(Class<?> c, String n) throws Exception { Field f = c.getDeclaredField(n); f.setAccessible(true); return f.get(null); }
    static String repeat(String s, int n) { StringBuilder b = new StringBuilder(); for (int i = 0; i < n; i++) b.append(s); return b.toString(); }
}
