package de.audi.tghu.navi.app.cluster;
/** PC test stand-in: like the real one, setDSIKOMONavInfo stores it and sends setCapabilities {true, true}. */
public class DSIKOMOManager {
    private org.dsi.ifc.komonavinfo.DSIKOMONavInfo dsi;
    public void setDSIKOMONavInfo(org.dsi.ifc.komonavinfo.DSIKOMONavInfo d) { dsi = d; if (d != null) d.setCapabilities(new boolean[] {true, true}); }
    public org.dsi.ifc.komonavinfo.DSIKOMONavInfo getDSIKOMONavInfo() { return dsi; }
}
