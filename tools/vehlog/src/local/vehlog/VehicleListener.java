package local.vehlog;

/** Generated: empty methods of org.dsi.ifc.carvehiclestates.DSICarVehicleStatesListener, see VehicleListener.java for the ones used. */
public class VehicleListener implements org.dsi.ifc.carvehiclestates.DSICarVehicleStatesListener {
    /** The only methods with code. Values are copied, nothing else is done here. */
    public void updateDynamicVehicleInfoHighFrequent(org.dsi.ifc.carvehiclestates.DynamicVehicleInfoHighFrequent d, int validFlag) {
        try {
            VehLog.dynCount++;
            if (d == null) {
                return;
            }
            VehLog.speed = speed(d.vehicleSpeed);
            VehLog.realSpeed = speed(d.realVehicleSpeed);
            VehLog.rpm = d.engineSpeed;
        } catch (Throwable e) {
            VehLog.lastError = "dyn: " + e;
        }
    }

    private static String speed(org.dsi.ifc.global.CarBCSpeed s) {
        if (s == null) {
            return "null";
        }
        return s.speedValue + "(s" + s.speedValueState + ",u" + s.speedUnit + ")";
    }

    public void asyncException(int a0, String a1, int a2) {
        VehLog.lastError = "vehicle asyncException " + a0 + " " + a1 + " " + a2;
    }

    public void updateOilLevelViewOption(org.dsi.ifc.global.CarViewOption a0, int a1) { }
    public void updateOilLevelData(org.dsi.ifc.carvehiclestates.OilLevelData a0, int a1) { }
    public void updateVINViewOption(org.dsi.ifc.global.CarViewOption a0, int a1) { }
    public void updateVINData(java.lang.String a0, int a1) { }
    public void updateKeyViewOption(org.dsi.ifc.global.CarViewOption a0, int a1) { }
    public void updateKeyData(org.dsi.ifc.carvehiclestates.KeyData a0, int a1) { }
    public void updateDrvSchoolSystem(boolean a0, int a1) { }
    public void updateVehicleInfoViewOptions(org.dsi.ifc.carvehiclestates.VehicleInfoViewOptions a0, int a1) { }
    public void updateDynamicVehicleInfoHighFrequentViewOptions(org.dsi.ifc.carvehiclestates.DynamicVehicleInfoHighFrequentViewOptions a0, int a1) { }
    public void updateDynamicVehicleInfoMidFrequentViewOptions(org.dsi.ifc.carvehiclestates.DynamicVehicleInfoMidFrequentViewOptions a0, int a1) { }
    public void updateDynamicVehicleInfoMidFrequent(org.dsi.ifc.carvehiclestates.DynamicVehicleInfoMidFrequent a0, int a1) { }
    public void updateSemiStaticVehicleDataViewOptions(org.dsi.ifc.carvehiclestates.SemiStaticDataViewOptions a0, int a1) { }
    public void updateSemiStaticVehicleData(org.dsi.ifc.carvehiclestates.SemiStaticVehicleData a0, int a1) { }
    public void updateDynamicVehicleInfoSCR(org.dsi.ifc.carvehiclestates.DynamicVehicleInfoSCR a0, int a1) { }
}
