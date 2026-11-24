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
            VehLog.speedNow(d.vehicleSpeed);
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
