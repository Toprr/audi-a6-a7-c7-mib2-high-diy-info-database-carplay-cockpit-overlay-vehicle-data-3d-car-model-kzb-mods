package local.vehlog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.Hashtable;

import org.dsi.ifc.base.DSIBase;
import org.dsi.ifc.base.DSIListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * VehLog 1.0: listen only, looking for a source of the engine oil temperature.
 *
 * Three read-only listeners (java.lang.reflect.Proxy), the same way the stock HMI and
 * our Catalog listen:
 *  - DSIBAP: every BAP message that reaches the Java HMI (stock users: the ENI and eCall
 *    apps). Counted per method and the first two numbers (logical device, function),
 *    with the first and the newest value. Stops listening (clearNotification) after
 *    LISTEN_MS, so a long drive does not keep it busy.
 *  - DSIExlap: the list of available ExLAP services (ATTR_AVAILABLESERVICES), only if the
 *    HMI already has the service. We never call startDSIService for it: that would ask
 *    the system to start the whole ExLAP domain. We never call start() or stop().
 *  - DSIStartup: the state of the ExLAP domain (ATTR_DOMAINSTATUSEXLAP).
 * No request, no set, nothing is sent. Written to /mnt/persist/vehlog_listen.txt at most
 * once a minute and only when something changed. Switch off: /mnt/persist/vehlog_listen_off.
 */
final class Listen implements InvocationHandler {
    static final String OFF = "/mnt/persist/vehlog_listen_off";
    private static final String OUT = "/mnt/persist/vehlog_listen.txt";
    private static final String TMP = "/mnt/persist/vehlog_listen.tmp";
    static final long LISTEN_MS = 60L * 60L * 1000L;
    private static final int MAX_KEYS = 300;
    private static final int MAX_BYTES = 32;
    private static final int MAX_VALUE = 300;
    private static final long MIN_TEXT_INTERVAL_MS = 1000L;

    static final String BAP = "org.dsi.ifc.bap.DSIBAP";
    static final String EXLAP = "org.dsi.ifc.exlap.DSIExlap";
    static final String STARTUP = "org.dsi.ifc.startup.DSIStartup";
    private static final int ATTR_AVAILABLESERVICES = 1;
    private static final int ATTR_DOMAINSTATUSEXLAP = 31;

    private static final Hashtable entries = new Hashtable();   // key -> Entry
    private static boolean bapDone;
    private static boolean exlapDone;
    private static boolean startupDone;
    private static DSIBase bapService;
    private static Object bapProxy;
    private static long bapSince;
    private static volatile boolean bapStopped;
    private static int dropped;
    private static int updates;
    private static int updatesWritten = -1;
    private static long lastWrite;

    private final String name;

    private Listen(String name) {
        this.name = name;
    }

    private static final class Entry {
        int count;
        int changes;
        long firstSeen;
        long lastText;
        String first;
        String value = "";
    }

    static boolean needsContext() {
        return !bapDone || !exlapDone || !startupDone;
    }

    /** One try for every listener not set up yet. Cheap, called from VehLog's loop. */
    static void tick(BundleContext bc, long now) {
        try {
            if (new File(OFF).exists()) {
                if (!bapStopped && bapService != null) {
                    stopBap("off switch");
                }
                return;
            }
            if (bc != null) {
                if (!bapDone) {
                    bapDone = listen(bc, BAP, null, "bap");
                }
                if (!exlapDone) {
                    exlapDone = listen(bc, EXLAP, new int[] {ATTR_AVAILABLESERVICES}, "exlap");
                }
                if (!startupDone) {
                    startupDone = listen(bc, STARTUP, new int[] {ATTR_DOMAINSTATUSEXLAP}, "startup");
                }
            }
            if (bapService != null && !bapStopped && now - bapSince >= LISTEN_MS) {
                stopBap("listened for " + (LISTEN_MS / 60000L) + " min");
            }
            write(false);
        } catch (Throwable t) {
            VehLog.line("listen: " + t);
        }
    }

    /** True when done (subscribed, or failed for good). False: service not there yet. */
    private static boolean listen(BundleContext bc, String ifc, int[] attrs, String tag) {
        try {
            ServiceReference ref = bc.getServiceReference(ifc);
            if (ref == null) {
                return false;
            }
            Object service = bc.getService(ref);
            if (!(service instanceof DSIBase)) {
                return false;
            }
            Class listener = Class.forName(ifc + "Listener");
            Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class[] {listener}, new Listen(tag));
            if (attrs == null) {
                ((DSIBase) service).setNotification((DSIListener) proxy);
            } else {
                ((DSIBase) service).setNotification(attrs, (DSIListener) proxy);
            }
            if (tag.equals("bap")) {
                bapService = (DSIBase) service;
                bapProxy = proxy;
                bapSince = System.currentTimeMillis();
            }
            VehLog.line("listen: " + tag + " subscribed (read only)");
            return true;
        } catch (Throwable t) {
            VehLog.line("listen: " + tag + " not used: " + t);
            return true;
        }
    }

    /** VehLog stops: stop listening to BAP, write what we have. */
    static void shutdown() {
        try {
            if (bapService != null && !bapStopped) {
                stopBap("VehLog stops");
            } else {
                write(true);
            }
        } catch (Throwable t) {
            // nothing
        }
    }

    private static void stopBap(String why) {
        bapStopped = true;
        try {
            bapService.clearNotification((DSIListener) bapProxy);
            VehLog.line("listen: bap stopped, " + why);
        } catch (Throwable t) {
            VehLog.line("listen: bap stop failed: " + t);
        }
        write(true);
    }

    public Object invoke(Object proxy, Method m, Object[] args) {
        try {
            String method = m.getName();
            if (method.equals("hashCode")) {
                return new Integer(System.identityHashCode(proxy));
            }
            if (method.equals("equals")) {
                return (args != null && args.length == 1 && args[0] == proxy) ? Boolean.TRUE : Boolean.FALSE;
            }
            if (method.equals("toString")) {
                return "VehLogListen(" + name + ")";
            }
            if (name.equals("bap") && bapStopped) {
                return null;
            }
            String key = name + "." + method;
            if (name.equals("bap") && method.startsWith("indication") && args != null && args.length >= 2
                    && args[0] instanceof Integer && args[1] instanceof Integer) {
                key = key + " " + args[0] + "/" + args[1];
            }
            Entry e;
            synchronized (entries) {
                e = (Entry) entries.get(key);
                if (e == null) {
                    if (entries.size() >= MAX_KEYS) {
                        dropped++;
                        return null;
                    }
                    e = new Entry();
                    e.firstSeen = System.currentTimeMillis() / 1000L;
                    entries.put(key, e);
                }
                e.count++;
                updates++;
            }
            long now = System.currentTimeMillis();
            if (e.first == null || now - e.lastText >= MIN_TEXT_INTERVAL_MS) {
                e.lastText = now;
                String v = text(args);
                if (e.first == null) {
                    e.first = v;
                }
                if (!v.equals(e.value)) {
                    e.value = v;
                    e.changes++;
                }
            }
        } catch (Throwable t) {
            // never disturb the DSI thread
        }
        return null;
    }

    static String text(Object[] args) {
        if (args == null) {
            return "";
        }
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < args.length && b.length() < MAX_VALUE; i++) {
            if (i > 0) {
                b.append(" | ");
            }
            Object a = args[i];
            if (a instanceof byte[]) {
                byte[] arr = (byte[]) a;
                b.append('[').append(arr.length).append("] ");
                for (int k = 0; k < arr.length && k < MAX_BYTES; k++) {
                    int x = arr[k] & 0xff;
                    b.append(HEX.charAt(x >> 4)).append(HEX.charAt(x & 15));
                }
                if (arr.length > MAX_BYTES) {
                    b.append("..");
                }
            } else if (a instanceof Object[]) {
                Object[] arr = (Object[]) a;
                b.append('[').append(arr.length).append("] ");
                for (int k = 0; k < arr.length && b.length() < MAX_VALUE; k++) {
                    b.append(arr[k]).append(' ');
                }
            } else {
                b.append(a);
            }
        }
        String s = b.toString();
        if (s.length() > MAX_VALUE) {
            s = s.substring(0, MAX_VALUE) + "...";
        }
        return s.replace('\n', ' ');
    }

    private static final String HEX = "0123456789abcdef";

    /** At most every 60 s (or always when force), and only when something changed. */
    static void write(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastWrite < 60000L) {
            return;
        }
        lastWrite = now;
        if (updates == updatesWritten && !force) {
            return;
        }
        if (updates == 0 && updatesWritten < 0 && !bapDone && !exlapDone && !startupDone) {
            return;
        }
        updatesWritten = updates;
        try {
            StringBuffer b = new StringBuffer();
            b.append("# VehLog ").append(VehLog.VERSION).append(" listen, written at ").append(now / 1000L)
                    .append(" s. bap=").append(bapService != null ? (bapStopped ? "stopped" : "on") : (bapDone ? "failed" : "not there"))
                    .append(" exlap=").append(exlapDone ? "done" : "not there")
                    .append(" startup=").append(startupDone ? "done" : "not there")
                    .append(" dropped=").append(dropped).append('\n');
            b.append("# key | updates | value changes | first seen (s) | first value | newest value\n");
            String[] k;
            synchronized (entries) {
                k = new String[entries.size()];
                int i = 0;
                for (Enumeration en = entries.keys(); en.hasMoreElements() && i < k.length;) {
                    k[i++] = (String) en.nextElement();
                }
            }
            sort(k);
            for (int i = 0; i < k.length; i++) {
                Entry e = (Entry) entries.get(k[i]);
                if (e == null) {
                    continue;
                }
                b.append(k[i]).append(" | ").append(e.count).append(" | ").append(e.changes).append(" | ")
                        .append(e.firstSeen).append(" | ").append(e.first).append(" | ").append(e.value).append('\n');
            }
            FileOutputStream out = new FileOutputStream(TMP);
            try {
                out.write(b.toString().getBytes("ISO-8859-1"));
            } finally {
                out.close();
            }
            File f = new File(OUT);
            if (!new File(TMP).renameTo(f)) {
                f.delete();
                new File(TMP).renameTo(f);
            }
        } catch (Throwable t) {
            VehLog.line("listen write failed: " + t);
        }
    }

    private static void sort(String[] a) {
        for (int i = 1; i < a.length; i++) {
            String x = a[i];
            int j = i - 1;
            while (j >= 0 && a[j].compareTo(x) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = x;
        }
    }

    static String stats() {
        return "listen: keys=" + entries.size() + " updates=" + updates + " bap="
                + (bapService != null ? (bapStopped ? "stopped" : "on") : "no") + " exlap=" + exlapDone + " startup=" + startupDone;
    }
}
