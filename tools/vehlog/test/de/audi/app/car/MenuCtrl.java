package de.audi.app.car;
/** PC test stand-in: returns a fake IFrameworkAccess (built in harness.Run). */
public final class MenuCtrl {
    public static Object fw;
    public static de.audi.atip.base.IFrameworkAccess getFramework() { return (de.audi.atip.base.IFrameworkAccess) fw; }
}
