package local.vehlog;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.dsi.ifc.base.DSIBase;
import org.dsi.ifc.komonavinfo.DSIKOMONavInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * While mib-cluster-map shows the CarPlay map in the cockpit, the Audi navigation
 * still sends its current street to the cluster, which draws it as a label at the
 * bottom. This keeps that label as small as it gets (a single space). Without
 * CarPlay nothing changes.
 *
 * 0.8, filter: the navigation's ClusterService asks its DSIKOMOManager for the
 * cluster interface every time it sends a street. VehLog hands the manager a thin
 * wrapper around the real interface (public setDSIKOMONavInfo, the same call the
 * navigation's own Activator uses). The wrapper passes every call through unchanged,
 * except setCurrentStreet / setTurnToStreet while CarPlay is up: those go out as a
 * blank. So the Audi street never reaches the cluster, instead of being overwritten
 * a moment later (0.6 showed it in between, e.g. "offroad"). When CarPlay goes away
 * the last real street is sent again at once. The wrapper is checked every 3 s and
 * put back if the navigation replaced it.
 *
 * 0.6, fallback (only while the wrapper is not in place): the navigation's position
 * description (DSINavigation attribute 29) and turn-to street (21) are triggers; the
 * blank goes out 10, 50, 100 and 400 ms later and every 3 s.
 *
 * On only while /mnt/persist/vehlog_street_blank exists (command.sh writes it from
 * vehlog/street_blank.txt on the SD card). Every error is caught; the worst case is
 * that the street name stays visible.
 */
final class StreetBlank implements Runnable, InvocationHandler {
    private static final String SWITCH = "/mnt/persist/vehlog_street_blank";
    private static final String CP_STATE = "/tmp/mibr-carplay111.state";
    private static final String KOMO = "org.dsi.ifc.komonavinfo.DSIKOMONavInfo";
    private static final String NAV = "org.dsi.ifc.navigation.DSINavigation";
    private static final int ATTR_SOPOSPOSITIONDESCRIPTION = 29;
    private static final int ATTR_RGTURNTOSTREET = 21;
    private static final long[] BURST_MS = {10L, 50L, 100L, 400L};
    private static final long CHECK_MS = 3000L;
    private static final long TICK_MS = 500L;
    private static final String BLANK = " ";

    private static final Object LOCK = new Object();
    private static DSIKOMONavInfo komo;
    private static boolean triggered;
    private static boolean started;
    private static int triggers;
    private static int sends;
    private static boolean sendErrorLogged;

    // 0.8 filter
    private static volatile boolean active;          // switch on and CarPlay map up
    private static volatile Object wrapper;           // our proxy, as handed to the manager
    private static volatile DSIKOMONavInfo real;      // the interface behind it
    private static volatile String lastStreet;        // last street the navigation sent
    private static volatile String lastTurn1;
    private static volatile String lastTurn2;
    private static int filtered;
    private static int wraps;
    private static boolean wrapErrorLogged;

    /** 0 = navigation listener (trigger), 1 = cluster wrapper, 2 = thread. */
    private final int role;
    private final DSIKOMONavInfo target;

    private StreetBlank(int role, DSIKOMONavInfo target) {
        this.role = role;
        this.target = target;
    }

    /** Called by VehLog's thread until it returns true (subscribed and running). */
    static boolean start(BundleContext bc) {
        if (started) {
            return true;
        }
        try {
            ServiceReference kr = bc.getServiceReference(KOMO);
            ServiceReference nr = bc.getServiceReference(NAV);
            if (kr == null || nr == null) {
                return false;
            }
            Object k = bc.getService(kr);
            Object n = bc.getService(nr);
            if (!(k instanceof DSIKOMONavInfo) || !(n instanceof DSIBase)) {
                return false;
            }
            komo = (DSIKOMONavInfo) k;
            Class listener = Class.forName(NAV + "Listener");
            Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class[] {listener}, new StreetBlank(0, null));
            ((DSIBase) n).setNotification(new int[] {ATTR_SOPOSPOSITIONDESCRIPTION, ATTR_RGTURNTOSTREET},
                    (org.dsi.ifc.base.DSIListener) proxy);
            started = true;
            Thread t = new Thread(new StreetBlank(2, null), "VehLogStreet");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            t.start();
            VehLog.line("street blank: ready (on=" + new File(SWITCH).exists() + ")");
            return true;
        } catch (Throwable t) {
            VehLog.line("street blank: not started: " + t);
            started = true;   // do not try again
            return true;
        }
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        String name = m.getName();
        if (name.equals("hashCode")) {
            return new Integer(System.identityHashCode(proxy));
        }
        if (name.equals("equals")) {
            return (args != null && args.length == 1 && args[0] == proxy) ? Boolean.TRUE : Boolean.FALSE;
        }
        if (name.equals("toString")) {
            return role == 1 ? "VehLogStreetFilter(" + target + ")" : "VehLogStreetTrigger";
        }
        if (role == 1) {
            return filter(m, name, args);
        }
        if (name.equals("updateSoPosPositionDescription") || name.equals("updateRgTurnToStreet")) {
            synchronized (LOCK) {
                triggers++;
                triggered = true;
                LOCK.notifyAll();
            }
        }
        return null;
    }

    /** The cluster wrapper: everything passes through, streets are blanked while active. */
    private Object filter(Method m, String name, Object[] args) throws Throwable {
        Object[] a = args;
        try {
            if (name.equals("setCurrentStreet") && args != null && args.length == 1) {
                lastStreet = (String) args[0];
                if (active) {
                    a = new Object[] {BLANK};
                    filtered++;
                }
            } else if (name.equals("setTurnToStreet") && args != null && args.length == 2) {
                lastTurn1 = (String) args[0];
                lastTurn2 = (String) args[1];
                if (active) {
                    a = new Object[] {"", ""};
                    filtered++;
                }
            }
        } catch (Throwable t) {
            a = args;   // never change a call we do not understand
        }
        try {
            return m.invoke(target, a);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public void run() {
        long lastCheck = 0L;
        long lastSwitchRead = 0L;
        long lastWrapCheck = 0L;
        boolean on = false;
        boolean wasActive = false;
        while (true) {
            try {
                boolean burst;
                synchronized (LOCK) {
                    if (!triggered) {
                        LOCK.wait(TICK_MS);
                    }
                    burst = triggered;
                    triggered = false;
                }
                long now = System.currentTimeMillis();
                if (now - lastSwitchRead >= CHECK_MS) {
                    lastSwitchRead = now;
                    on = new File(SWITCH).exists();
                }
                boolean act = on && carPlayUp();
                active = act;
                if (now - lastWrapCheck >= CHECK_MS) {
                    lastWrapCheck = now;
                    ensureWrapper();
                }
                if (act != wasActive) {
                    wasActive = act;
                    if (act) {
                        blank();
                    } else {
                        restore();
                    }
                }
                if (!act || filterInPlace()) {
                    continue;
                }
                // fallback while the wrapper is not in place
                if (burst) {
                    long t0 = now;
                    for (int i = 0; i < BURST_MS.length; i++) {
                        long wait = t0 + BURST_MS[i] - System.currentTimeMillis();
                        if (wait > 0) {
                            Thread.sleep(wait);
                        }
                        blank();
                    }
                    lastCheck = System.currentTimeMillis();
                } else if (now - lastCheck >= CHECK_MS) {
                    lastCheck = now;
                    blank();
                }
            } catch (Throwable t) {
                try {
                    Thread.sleep(1000L);
                } catch (Throwable ignored) {
                    // nothing
                }
            }
        }
    }

    /** True while the navigation's manager still hands out our wrapper. */
    private static boolean filterInPlace() {
        try {
            Object w = wrapper;
            if (w == null) {
                return false;
            }
            de.audi.tghu.navi.app.Navigation nav = de.audi.tghu.navi.app.Navigation.getInstance();
            if (nav == null) {
                return false;
            }
            de.audi.tghu.navi.app.cluster.DSIKOMOManager mgr = nav.getDSIKOMOManager();
            return mgr != null && mgr.getDSIKOMONavInfo() == w;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Hands the manager our wrapper around whatever real interface it holds now. */
    private static void ensureWrapper() {
        try {
            de.audi.tghu.navi.app.Navigation nav = de.audi.tghu.navi.app.Navigation.getInstance();
            if (nav == null) {
                return;
            }
            de.audi.tghu.navi.app.cluster.DSIKOMOManager mgr = nav.getDSIKOMOManager();
            if (mgr == null) {
                return;
            }
            DSIKOMONavInfo cur = mgr.getDSIKOMONavInfo();
            if (cur == null || cur == wrapper) {
                return;
            }
            if (Proxy.isProxyClass(cur.getClass()) && Proxy.getInvocationHandler(cur) instanceof StreetBlank) {
                return;
            }
            Class iface = Class.forName(KOMO);
            Object w = Proxy.newProxyInstance(iface.getClassLoader(), new Class[] {iface}, new StreetBlank(1, cur));
            real = cur;
            wrapper = w;
            mgr.setDSIKOMONavInfo((DSIKOMONavInfo) w);
            wraps++;
            VehLog.line("street filter: in place (" + wraps + ")");
            if (active) {
                blank();
            }
        } catch (Throwable t) {
            if (!wrapErrorLogged) {
                wrapErrorLogged = true;
                VehLog.line("street filter: not possible, using the fallback: " + t);
            }
        }
    }

    private static DSIKOMONavInfo out() {
        DSIKOMONavInfo r = real;
        return r != null ? r : komo;
    }

    private static void blank() {
        try {
            DSIKOMONavInfo k = out();
            k.setTurnToStreet("", "");
            k.setCurrentStreet(BLANK);
            sends++;
        } catch (Throwable t) {
            if (!sendErrorLogged) {
                sendErrorLogged = true;
                VehLog.line("street blank: send failed: " + t);
            }
        }
    }

    /** CarPlay gone or switched off: show the navigation's last street again. */
    private static void restore() {
        try {
            String s = lastStreet;
            String t1 = lastTurn1;
            String t2 = lastTurn2;
            DSIKOMONavInfo k = out();
            if (t1 != null && t1.length() > 0) {
                k.setTurnToStreet(t1, t2 == null ? "" : t2);
            } else if (s != null) {
                k.setTurnToStreet("", "");
                k.setCurrentStreet(s);
            }
            VehLog.line("street blank: CarPlay off, street restored");
        } catch (Throwable t) {
            // the navigation sends its next street soon anyway
        }
    }

    /** mib-cluster-map writes "streaming" while the CarPlay map is on the cockpit. */
    private static boolean carPlayUp() {
        try {
            FileInputStream in = new FileInputStream(CP_STATE);
            try {
                byte[] b = new byte[16];
                int n = in.read(b);
                return n >= 9 && new String(b, 0, 9, "ISO-8859-1").equals("streaming");
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return false;
        }
    }

    static String stats() {
        return "street blank: triggers=" + triggers + " sends=" + sends + " filter=" + (wrapper != null)
                + " wraps=" + wraps + " filtered=" + filtered + " active=" + active;
    }
}
