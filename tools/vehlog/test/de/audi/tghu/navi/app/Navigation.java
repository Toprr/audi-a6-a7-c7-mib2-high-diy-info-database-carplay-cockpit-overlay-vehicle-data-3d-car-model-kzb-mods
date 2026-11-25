package de.audi.tghu.navi.app;
/** PC test stand-in for the navigation singleton (same public methods VehLog uses). */
public final class Navigation {
    public static Navigation instance;
    public final de.audi.tghu.navi.app.cluster.DSIKOMOManager mgr = new de.audi.tghu.navi.app.cluster.DSIKOMOManager();
    public static Navigation getInstance() { return instance; }
    public de.audi.tghu.navi.app.cluster.DSIKOMOManager getDSIKOMOManager() { return mgr; }
}
