    /** The only methods with code. Values are copied as text, nothing else is done here. */
    public void updateBCOilTemperatureValue(org.dsi.ifc.global.CarBCTemperature t, int validFlag) {
        try {
            VehLog.oilCount++;
            VehLog.oil = t == null ? "null/v" + validFlag
                    : t.temperatureValue + "(s" + t.state + ",u" + t.temperatureUnit + ",v" + validFlag + ")";
        } catch (Throwable e) {
            VehLog.lastError = "oil: " + e;
        }
    }

    public void updateBCCoolantTemperature(org.dsi.ifc.global.CarBCTemperature t, int validFlag) {
        try {
            VehLog.coolantCount++;
            VehLog.coolant = t == null ? "null/v" + validFlag
                    : t.temperatureValue + "(s" + t.state + ",u" + t.temperatureUnit + ",v" + validFlag + ")";
        } catch (Throwable e) {
            VehLog.lastError = "coolant: " + e;
        }
    }

    public void asyncException(int a0, String a1, int a2) {
        VehLog.lastError = "kombi asyncException " + a0 + " " + a1 + " " + a2;
    }

    /** 0.2: does the cluster offer the oil temperature at all (view options), and is the BC oil display on. */
    public void updateBCViewOptions(org.dsi.ifc.carkombi.BCViewOptions v, int validFlag) {
        try {
            VehLog.viewCount++;
            VehLog.oilView = v == null ? "null/v" + validFlag
                    : "oilTemp" + opt(v.oilTemperature) + " oilTempValue" + opt(v.oilTemperatureValue) + " v" + validFlag;
        } catch (Throwable e) {
            VehLog.lastError = "view: " + e;
        }
    }

    public void updateBCOilTemperature(boolean on, int validFlag) {
        VehLog.oilSetting = (on ? "on" : "off") + "/v" + validFlag;
    }

    private static String opt(org.dsi.ifc.global.CarViewOption o) {
        return o == null ? "(null)" : "(st" + o.state + ",r" + o.reason + ")";
    }

    /** 0.7: range and long term consumption for the estimated fuel in the overlay. */
    public void updateBCCurrentRange1(org.dsi.ifc.global.CarBCCurrentRange r, int validFlag) {
        try {
            VehLog.rangeNow(r, validFlag);
        } catch (Throwable e) {
            VehLog.lastError = "range: " + e;
        }
    }

    public void updateBCLongTermAverageConsumption1(org.dsi.ifc.global.CarBCConsumption c, int validFlag) {
        try {
            VehLog.consNow(c, validFlag);
        } catch (Throwable e) {
            VehLog.lastError = "consumption: " + e;
        }
    }
