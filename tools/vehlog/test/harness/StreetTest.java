package harness;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
/** PC test of StreetBlank: fake navigation and cluster services, measures when the blanks go out. */
public class StreetTest {
    static final List<String> sent = Collections.synchronizedList(new ArrayList<String>());
    static volatile long t0;
    static Object navListener;
    static void state(boolean streaming, boolean on) throws Exception {
        new File("/tmp").mkdirs(); new File("/mnt/persist").mkdirs();
        File st = new File("/tmp/mibr-carplay111.state");
        if (streaming) { FileWriter w = new FileWriter(st); w.write("streaming\n"); w.close(); } else st.delete();
        File sw = new File("/mnt/persist/vehlog_street_blank");
        if (on) new FileOutputStream(sw).close(); else sw.delete();
    }
    static void trigger(String label) throws Exception {
        sent.clear();
        t0 = System.currentTimeMillis();
        Method m = null;
        for (Method x : navListener.getClass().getInterfaces()[0].getMethods()) if (x.getName().equals("updateSoPosPositionDescription")) m = x;
        m.invoke(navListener, new Object[]{null, true, 1});
        Thread.sleep(700);
        System.out.println(label + ": " + sent);
    }
    public static void main(String[] a) throws Exception {
        final ClassLoader cl = StreetTest.class.getClassLoader();
        final Object refK = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.ServiceReference.class}, (p, m, x) -> m.getName().equals("toString") ? "refK" : null);
        final Object refN = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.ServiceReference.class}, (p, m, x) -> m.getName().equals("toString") ? "refN" : null);
        final Object komo = Proxy.newProxyInstance(cl, new Class[]{org.dsi.ifc.komonavinfo.DSIKOMONavInfo.class}, (p, m, x) -> {
            if (m.getName().startsWith("set") && !m.getName().equals("setNotification")) sent.add((System.currentTimeMillis() - t0) + "ms " + m.getName() + Arrays.toString(x));
            return null;
        });
        final Object nav = Proxy.newProxyInstance(cl, new Class[]{org.dsi.ifc.navigation.DSINavigation.class}, (p, m, x) -> {
            if (m.getName().equals("setNotification")) { navListener = x[1]; System.out.println("nav subscribed to " + Arrays.toString((int[]) x[0])); }
            return null;
        });
        Object bc = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.BundleContext.class}, (p, m, x) -> {
            if (m.getName().equals("getServiceReference")) return ((String) x[0]).contains("komonavinfo") ? refK : refN;
            if (m.getName().equals("getService")) return x[0] == refK ? komo : nav;
            return null;
        });
        state(true, true);
        Class<?> sb = Class.forName("local.vehlog.StreetBlank");
        Method start = sb.getDeclaredMethod("start", org.osgi.framework.BundleContext.class); start.setAccessible(true);
        System.out.println("started = " + start.invoke(null, bc));
        Thread.sleep(3500);   // let the thread read the switch once
        trigger("CarPlay up, switch on");
        state(false, true); Thread.sleep(3200); trigger("no CarPlay, switch on");
        state(true, false); Thread.sleep(3200); trigger("CarPlay up, switch off");
        state(true, true); Thread.sleep(3200); sent.clear(); t0 = System.currentTimeMillis(); Thread.sleep(6500);
        System.out.println("6.5 s without trigger (quiet check): " + sent.size() + " sends");
        Method st = sb.getDeclaredMethod("stats"); st.setAccessible(true); System.out.println(st.invoke(null));
        state(false, false);
        System.exit(0);
    }
}
