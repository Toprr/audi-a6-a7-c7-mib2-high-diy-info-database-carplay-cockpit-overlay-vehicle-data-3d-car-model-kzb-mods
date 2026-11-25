package harness;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.dsi.ifc.komonavinfo.DSIKOMONavInfo;
/** PC test of the 0.8 street filter between the navigation and the cluster. */
public class FilterTest {
    static final List<String> sent = Collections.synchronizedList(new ArrayList<String>());
    static int fails;
    static void check(String what, boolean ok) { System.out.println((ok ? "ok   " : "FAIL ") + what); if (!ok) fails++; }
    static void state(boolean streaming, boolean on) throws Exception {
        new File("/tmp").mkdirs(); new File("/mnt/persist").mkdirs();
        File st = new File("/tmp/mibr-carplay111.state");
        if (streaming) { FileWriter w = new FileWriter(st); w.write("streaming\n"); w.close(); } else st.delete();
        File sw = new File("/mnt/persist/vehlog_street_blank");
        if (on) new FileOutputStream(sw).close(); else sw.delete();
    }
    static String last(String method) {
        synchronized (sent) { for (int i = sent.size() - 1; i >= 0; i--) if (sent.get(i).startsWith(method)) return sent.get(i); }
        return null;
    }
    public static void main(String[] a) throws Exception {
        ClassLoader cl = FilterTest.class.getClassLoader();
        final DSIKOMONavInfo realKomo = (DSIKOMONavInfo) Proxy.newProxyInstance(cl, new Class[]{DSIKOMONavInfo.class}, (p, m, x) -> {
            if (m.getName().equals("setMapScale")) throw new IllegalStateException("cluster says no");
            if (m.getName().equals("toString")) return "realKomo";
            if (m.getName().startsWith("set") && !m.getName().equals("setNotification")) sent.add(m.getName() + Arrays.deepToString(x == null ? new Object[0] : x));
            return null;
        });
        final Object refK = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.ServiceReference.class}, (p, m, x) -> m.getName().equals("toString") ? "refK" : null);
        final Object refN = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.ServiceReference.class}, (p, m, x) -> m.getName().equals("toString") ? "refN" : null);
        final Object nav = Proxy.newProxyInstance(cl, new Class[]{org.dsi.ifc.navigation.DSINavigation.class}, (p, m, x) -> null);
        Object bc = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.BundleContext.class}, (p, m, x) -> {
            if (m.getName().equals("getServiceReference")) return ((String) x[0]).contains("komonavinfo") ? refK : refN;
            if (m.getName().equals("getService")) return x[0] == refK ? realKomo : nav;
            return null;
        });
        de.audi.tghu.navi.app.Navigation.instance = new de.audi.tghu.navi.app.Navigation();
        de.audi.tghu.navi.app.cluster.DSIKOMOManager mgr = de.audi.tghu.navi.app.Navigation.instance.mgr;
        mgr.setDSIKOMONavInfo(realKomo);
        state(false, true);
        Class<?> sb = Class.forName("local.vehlog.StreetBlank");
        Method start = sb.getDeclaredMethod("start", org.osgi.framework.BundleContext.class); start.setAccessible(true);
        Method stats = sb.getDeclaredMethod("stats"); stats.setAccessible(true);
        check("started", (Boolean) start.invoke(null, bc));
        Thread.sleep(3600);
        DSIKOMONavInfo hmi = mgr.getDSIKOMONavInfo();
        check("filter in place in the manager", hmi != realKomo && hmi.toString().startsWith("VehLogStreetFilter"));
        // no CarPlay: everything unchanged
        sent.clear();
        hmi.setCurrentStreet("Main Street"); hmi.setTurnToStreet("", "");
        hmi.setDistanceToNextManeuver(300L, 1, true);
        check("no CarPlay: street passes unchanged", "setCurrentStreet[Main Street]".equals(last("setCurrentStreet")));
        check("no CarPlay: other calls pass unchanged", "setDistanceToNextManeuver[300, 1, true]".equals(last("setDistanceToNextManeuver")));
        // CarPlay up
        sent.clear();
        state(true, true);
        Thread.sleep(700);
        check("CarPlay up: blank sent at once", "setCurrentStreet[ ]".equals(last("setCurrentStreet")));
        sent.clear();
        hmi = mgr.getDSIKOMONavInfo();
        hmi.setCurrentStreet("offroad");
        check("CarPlay up: navigation's 'offroad' goes out as a blank", sent.contains("setCurrentStreet[ ]") && !sent.toString().contains("offroad"));
        hmi.setTurnToStreet("Park Lane", "B1");
        check("CarPlay up: turn-to street goes out empty", "setTurnToStreet[, ]".equals(last("setTurnToStreet")));
        hmi.setDistanceToNextManeuver(120L, 1, true);
        hmi.setCityName("Sampletown");
        check("CarPlay up: distance and city unchanged",
              "setDistanceToNextManeuver[120, 1, true]".equals(last("setDistanceToNextManeuver")) && "setCityName[Sampletown]".equals(last("setCityName")));
        boolean threw = false;
        try { hmi.setMapScale(1, 2, new boolean[0], 3, 4, 5); } catch (IllegalStateException e) { threw = true; }
        check("an exception from the cluster reaches the navigation unchanged", threw);
        // navigation replaces the interface (service came back)
        mgr.setDSIKOMONavInfo(realKomo);
        Thread.sleep(3600);
        check("navigation replaced it: filter back within 3 s", mgr.getDSIKOMONavInfo() != realKomo);
        sent.clear();
        mgr.getDSIKOMONavInfo().setCurrentStreet("offroad");
        check("after that, still blanked", "setCurrentStreet[ ]".equals(last("setCurrentStreet")));
        // phone unplugged: last real street comes back
        mgr.getDSIKOMONavInfo().setTurnToStreet("", "");
        mgr.getDSIKOMONavInfo().setCurrentStreet("Station Road");
        sent.clear();
        state(false, true);
        Thread.sleep(800);
        check("CarPlay off: last real street restored at once", "setCurrentStreet[Station Road]".equals(last("setCurrentStreet")));
        sent.clear();
        mgr.getDSIKOMONavInfo().setCurrentStreet("Mill Road");
        check("CarPlay off: streets pass again", "setCurrentStreet[Mill Road]".equals(last("setCurrentStreet")));
        // switch off with CarPlay up
        state(true, false);
        Thread.sleep(3600);
        sent.clear();
        mgr.getDSIKOMONavInfo().setCurrentStreet("Mill Road");
        check("switch off: streets pass even with CarPlay", "setCurrentStreet[Mill Road]".equals(last("setCurrentStreet")));
        System.out.println(stats.invoke(null));
        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
        System.exit(fails);
    }
}
