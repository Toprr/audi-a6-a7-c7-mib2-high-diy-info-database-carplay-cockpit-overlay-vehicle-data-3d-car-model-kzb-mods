package harness;
import java.lang.reflect.*;
import org.dsi.ifc.global.*;
/** PC test: fake framework, OSGi and DSI services that push values into VehLog's listeners. */
public class Run {
    static int subs;
    public static void main(String[] a) throws Exception {
        final ClassLoader cl = Run.class.getClassLoader();
        final Object ref = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.ServiceReference.class}, new InvocationHandler(){public Object invoke(Object p, Method m, Object[] x){return null;}});
        final InvocationHandler dsi = new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] x) {
                if (m.getName().equals("setNotification") && x.length == 2) {
                    subs++;
                    final Object l = x[1];
                    System.out.println("TEST setNotification " + java.util.Arrays.toString((int[]) x[0]) + " listener=" + l.getClass().getName());
                    new Thread() { public void run() {
                        for (int i = 0; i < 40; i++) {
                            try {
                                if (l instanceof org.dsi.ifc.carkombi.DSICarKombiListener) {
                                    ((org.dsi.ifc.carkombi.DSICarKombiListener) l).updateBCOilTemperatureValue(new CarBCTemperature(1, 80f + i, 0), 1);
                                    ((org.dsi.ifc.carkombi.DSICarKombiListener) l).updateBCCoolantTemperature(i == 3 ? null : new CarBCTemperature(1, 90f, 0), 1);
                                    org.dsi.ifc.carkombi.BCViewOptions vo = new org.dsi.ifc.carkombi.BCViewOptions(); if (i == 2) vo.oilTemperatureValue = new CarViewOption(2, 1); ((org.dsi.ifc.carkombi.DSICarKombiListener) l).updateBCViewOptions(i == 4 ? null : vo, 1); ((org.dsi.ifc.carkombi.DSICarKombiListener) l).updateBCOilTemperature(i % 2 == 0, 1);
                                } else {
                                    org.dsi.ifc.carvehiclestates.DynamicVehicleInfoHighFrequent d = new org.dsi.ifc.carvehiclestates.DynamicVehicleInfoHighFrequent();
                                    d.vehicleSpeed = new CarBCSpeed(1, 52f + i, 0); d.realVehicleSpeed = i == 5 ? null : new CarBCSpeed(1, 50f + i, 0); d.engineSpeed = 1500 + i;
                                    ((org.dsi.ifc.carvehiclestates.DSICarVehicleStatesListener) l).updateDynamicVehicleInfoHighFrequent(d, 1);
                                    ((org.dsi.ifc.carvehiclestates.DSICarVehicleStatesListener) l).updateDynamicVehicleInfoHighFrequent(null, 1);
                                }
                                Thread.sleep(2500);
                            } catch (Throwable t) { System.out.println("TEST listener threw " + t); }
                        }
                    }}.start();
                }
                return null;
            }
        };
        final Object kombi = Proxy.newProxyInstance(cl, new Class[]{org.dsi.ifc.carkombi.DSICarKombi.class}, dsi);
        final Object veh = Proxy.newProxyInstance(cl, new Class[]{org.dsi.ifc.carvehiclestates.DSICarVehicleStates.class}, dsi);
        final Object bc = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.BundleContext.class}, new InvocationHandler() {
            int calls;
            public Object invoke(Object p, Method m, Object[] x) {
                if (m.getName().equals("getServiceReference")) { calls++; return calls <= 2 ? null : ref; } // not there at first
                if (m.getName().equals("getService")) { return lastName.indexOf("carkombi") >= 0 ? kombi : veh; }
                return null;
            }
        });
        de.audi.app.car.MenuCtrl.fw = Proxy.newProxyInstance(cl, new Class[]{de.audi.atip.base.IFrameworkAccess.class}, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] x) { return m.getName().equals("getBundleCxt") ? bc : null; }
        });
        // remember which service name was asked for last
        final InvocationHandler h = Proxy.getInvocationHandler(bc);
        Object bc2 = Proxy.newProxyInstance(cl, new Class[]{org.osgi.framework.BundleContext.class}, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] x) throws Throwable {
                if (m.getName().equals("getServiceReference")) lastName = (String) x[0];
                return h.invoke(p, m, x);
            }
        });
        Field f = de.audi.app.car.MenuCtrl.class.getField("fw");
        final Object bcFinal = bc2;
        f.set(null, Proxy.newProxyInstance(cl, new Class[]{de.audi.atip.base.IFrameworkAccess.class}, new InvocationHandler() {
            public Object invoke(Object p, Method m, Object[] x) { return m.getName().equals("getBundleCxt") ? bcFinal : null; }
        }));
        // The HMI creating CarViewOption objects from several threads
        for (int i = 0; i < 5; i++) new Thread() { public void run() { for (int k = 0; k < 1000; k++) { CarViewOption o = new CarViewOption(k, 2); if (o.getState() != k || o.getReason() != 2) System.out.println("TEST WRONG VALUE"); } } }.start();
        CarViewOption d = new CarViewOption();
        System.out.println("TEST toString " + d + " / " + new CarViewOption(3, 4));
        Thread.sleep(70000);
        System.out.println("TEST subscriptions=" + subs + " threads named VehLog=" + count());
        System.exit(0);
    }
    static volatile String lastName = "";
    static int count() { int n = 0; Thread[] t = new Thread[200]; int c = Thread.enumerate(t); for (int i = 0; i < c; i++) if ("VehLog".equals(t[i].getName())) n++; return n; }
}
