package local.vehlog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.dsi.ifc.base.DSIBase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * VehLog 0.4: "what does this car send to the MMI?" catalog.
 *
 * One generic, read-only listener (java.lang.reflect.Proxy) per vehicle DSI service,
 * subscribed to every ATTR_* attribute the interface declares. Every callback is
 * counted; the newest value is kept as text. Every 60 s the catalog is written to
 * /mnt/persist/vehlog_catalog.txt (overwritten, so it never grows).
 *
 * Only vehicle services are used. Services with personal data (phone, messages,
 * contacts, navigation, media, online) are left out on purpose, and methods whose
 * name contains VIN or Key are counted but their values are not stored, so the
 * catalog can be shared.
 */
final class Catalog implements InvocationHandler {
    private static final String OUT = "/mnt/persist/vehlog_catalog.txt";
    private static final String TMP = "/mnt/persist/vehlog_catalog.tmp";
    private static final int MAX_VALUE = 300;
    private static final long MIN_TEXT_INTERVAL_MS = 1000L;

    static final String[] SERVICES = {
        "carkombi.DSICarKombi",
        "carvehiclestates.DSICarVehicleStates",
        "generalvehiclestates.DSIGeneralVehicleStates",
        "cardrivingcharacteristics.DSICarDrivingCharacteristics",
        "cardriverassistance.DSICarDriverAssistance",
        "carlight.DSICarLight",
        "carcomfort.DSICarComfort",
        "caraircondition.DSICarAirCondition",
        "carparkingsystem.DSICarParkingSystem",
        "carseat.DSICarSeat",
        "careco.DSICarEco",
        "carhybrid.DSICarHybrid",
        "carstopwatch.DSICarStopWatch",
        "cartimeunitslanguage.DSICarTimeUnitsLanguage",
        "carauxheatercooler.DSICarAuxHeaterCooler",
        "powermanagement.DSIPowerManagement",
    };

    private static final Hashtable entries = new Hashtable();   // "Service.method" -> Entry
    private static final Vector subscribed = new Vector();
    private static long lastWrite;
    private static int updates;
    private static int updatesWritten = -1;

    private final String service;

    private Catalog(String service) {
        this.service = service;
    }

    private static final class Entry {
        int count;
        int changes;
        long firstSeen;
        long lastText;
        String value = "";
    }

    /** Tries every service not subscribed yet. Returns how many are subscribed now. */
    static int subscribeAll(BundleContext bc) {
        for (int i = 0; i < SERVICES.length; i++) {
            String s = SERVICES[i];
            if (subscribed.contains(s)) {
                continue;
            }
            try {
                String ifc = "org.dsi.ifc." + s;
                ServiceReference ref = bc.getServiceReference(ifc);
                if (ref == null) {
                    continue;
                }
                Object service = bc.getService(ref);
                if (!(service instanceof DSIBase)) {
                    continue;
                }
                Class dsi = Class.forName(ifc);
                Class listener = Class.forName(ifc + "Listener");
                int[] attrs = attributes(dsi);
                Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class[] {listener},
                        new Catalog(s.substring(s.indexOf('.') + 1)));
                ((DSIBase) service).setNotification(attrs, (org.dsi.ifc.base.DSIListener) proxy);
                subscribed.addElement(s);
                VehLog.line("catalog: subscribed " + s + " (" + attrs.length + " attributes)");
            } catch (Throwable t) {
                subscribed.addElement(s);   // do not try again
                VehLog.line("catalog: " + s + " not used: " + t);
            }
        }
        return subscribed.size();
    }

    private static int[] attributes(Class dsi) {
        Field[] f = dsi.getFields();
        Vector v = new Vector();
        for (int i = 0; i < f.length; i++) {
            if (f[i].getName().startsWith("ATTR_") && f[i].getType() == Integer.TYPE) {
                try {
                    v.addElement(new Integer(f[i].getInt(null)));
                } catch (Throwable t) {
                    // skip
                }
            }
        }
        int[] a = new int[v.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = ((Integer) v.elementAt(i)).intValue();
        }
        return a;
    }

    public Object invoke(Object proxy, Method m, Object[] args) {
        try {
            String name = m.getName();
            if (name.equals("hashCode") || name.equals("equals") || name.equals("toString")) {
                if (name.equals("hashCode")) {
                    return new Integer(System.identityHashCode(proxy));
                }
                if (name.equals("equals")) {
                    return (args != null && args.length == 1 && args[0] == proxy) ? Boolean.TRUE : Boolean.FALSE;
                }
                return "VehLogCatalog(" + service + ")";
            }
            String key = service + "." + name;
            Entry e;
            synchronized (entries) {
                e = (Entry) entries.get(key);
                if (e == null) {
                    e = new Entry();
                    e.firstSeen = System.currentTimeMillis() / 1000L;
                    entries.put(key, e);
                }
                e.count++;
                updates++;
            }
            long now = System.currentTimeMillis();
            if (now - e.lastText >= MIN_TEXT_INTERVAL_MS) {
                e.lastText = now;
                String v = (name.indexOf("VIN") >= 0 || name.indexOf("Key") >= 0) ? "(not stored)" : text(args);
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

    private static String text(Object[] args) {
        if (args == null) {
            return "";
        }
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < args.length && b.length() < MAX_VALUE; i++) {
            if (i > 0) {
                b.append(" | ");
            }
            Object a = args[i];
            if (a instanceof Object[]) {
                Object[] arr = (Object[]) a;
                b.append('[').append(arr.length).append("] ");
                for (int k = 0; k < arr.length && b.length() < MAX_VALUE; k++) {
                    b.append(arr[k]).append(' ');
                }
            } else if (a instanceof int[]) {
                int[] arr = (int[]) a;
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

    /** Writes the catalog at most every 60 s (or always when force). */
    static void write(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastWrite < 60000L) {
            return;
        }
        lastWrite = now;
        if (!force && updates == updatesWritten) {
            return;   // nothing new, spare the flash
        }
        updatesWritten = updates;
        try {
            StringBuffer b = new StringBuffer();
            b.append("# VehLog ").append(VehLog.VERSION).append(" catalog, written at ").append(now / 1000L)
                    .append(" s. Columns: service.method | updates | value changes | first seen (s) | newest value\n");
            b.append("# subscribed: ").append(subscribed).append('\n');
            Vector keys = new Vector();
            synchronized (entries) {
                for (Enumeration en = entries.keys(); en.hasMoreElements();) {
                    keys.addElement(en.nextElement());
                }
            }
            String[] k = new String[keys.size()];
            keys.copyInto(k);
            sort(k);
            for (int i = 0; i < k.length; i++) {
                Entry e = (Entry) entries.get(k[i]);
                if (e == null) {
                    continue;
                }
                b.append(k[i]).append(" | ").append(e.count).append(" | ").append(e.changes).append(" | ")
                        .append(e.firstSeen).append(" | ").append(e.value).append('\n');
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
            VehLog.line("catalog write failed: " + t);
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
}
