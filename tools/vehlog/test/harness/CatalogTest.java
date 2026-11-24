package harness;
import java.lang.reflect.*;
import java.util.*;
/** PC test of the catalog: fake OSGi + DSI services, every listener method called with dummy values. */
public class CatalogTest {
    static String lastName = "";
    static List<Object> listeners = new ArrayList<Object>();
    static List<int[]> attrs = new ArrayList<int[]>();
    public static void main(String[] a) throws Exception {
        final ClassLoader cl = CatalogTest.class.getClassLoader();
        final Object ref = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.ServiceReference.class}, (p, m, x) -> null);
        Object bc = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.BundleContext.class}, (p, m, x) -> {
            if (m.getName().equals("getServiceReference")) { lastName = (String) x[0]; return lastName.endsWith("DSICarEco") ? null : ref; }
            if (m.getName().equals("getService")) {
                Class<?> ifc = Class.forName(lastName);
                return Proxy.newProxyInstance(cl, new Class[]{ifc}, (p2, m2, x2) -> {
                    if (m2.getName().equals("setNotification") && x2.length == 2) { attrs.add((int[]) x2[0]); listeners.add(x2[1]); }
                    return null;
                });
            }
            return null;
        });
        Class<?> cat = Class.forName("local.vehlog.Catalog");
        Method sub = cat.getDeclaredMethod("subscribeAll", org.osgi.framework.BundleContext.class); sub.setAccessible(true);
        System.out.println("subscribed (first pass) = " + sub.invoke(null, bc));
        int calls = 0, failed = 0;
        for (Object l : listeners) {
            l.hashCode(); l.equals(l); l.toString();
            for (Class<?> ifc : l.getClass().getInterfaces()) for (Method m : ifc.getMethods()) {
                Object[] args = new Object[m.getParameterTypes().length];
                for (int i = 0; i < args.length; i++) {
                    Class<?> t = m.getParameterTypes()[i];
                    if (t == int.class) args[i] = 1; else if (t == boolean.class) args[i] = true; else if (t == byte.class) args[i] = (byte) 1;
                    else if (t == short.class) args[i] = (short) 1; else if (t == long.class) args[i] = 1L; else if (t == float.class) args[i] = 1f;
                    else if (t == double.class) args[i] = 1d; else if (t == char.class) args[i] = 'x'; else if (t == String.class) args[i] = "text";
                    else if (t == int[].class) args[i] = new int[]{1, 2, 3}; else args[i] = null;
                }
                try { m.invoke(l, args); calls++; } catch (Throwable e) { failed++; }
            }
        }
        System.out.println("listener calls=" + calls + " failed=" + failed + " attr lists=" + attrs.size());
        Method w = cat.getDeclaredMethod("write", boolean.class); w.setAccessible(true); w.invoke(null, true);
        Method f = Class.forName("local.vehlog.VehLog").getDeclaredMethod("flush"); f.setAccessible(true); f.invoke(null);
    }
}
