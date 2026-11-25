package local.vehlog;

/** Generated: empty methods of org.dsi.ifc.carkombi.DSICarKombiListener, see KombiListener.java for the ones used. */
public class KombiListener implements org.dsi.ifc.carkombi.DSICarKombiListener {
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

    public void updateSIAViewOptions(org.dsi.ifc.carkombi.SIAViewOptions a0, int a1) { }
    public void updateSIAServiceData(org.dsi.ifc.carkombi.SIAServiceData a0, int a1) { }
    public void updateSIAOilInspection(org.dsi.ifc.carkombi.SIAOilInspection a0, int a1) { }
    public void indicateEndOfSIAReset(boolean a0) { }
    public void updateSIAHistoryListUpdateInfo(org.dsi.ifc.global.CarArrayListUpdateInfo a0, int[] a1, int a2) { }
    public void responseSIAHistoryList(org.dsi.ifc.global.CarArrayListUpdateInfo a0, org.dsi.ifc.carkombi.SIAHistoryListRecord[] a1) { }
    public void updateSIAHistoryListTotalNumberOfElements(int a0, int a1) { }
    public void updateSIADistanceOilUser(org.dsi.ifc.carkombi.SIADistanceData a0, int a1) { }
    public void updateSIADistanceAirFilterUser(org.dsi.ifc.carkombi.SIADistanceData a0, int a1) { }
    public void updateSIADistanceOilFilterUser(org.dsi.ifc.carkombi.SIADistanceData a0, int a1) { }
    public void updateSIAInspectionDistanceUser(org.dsi.ifc.carkombi.SIADistanceData a0, int a1) { }
    public void updateSIADailyAverageMileage(int a0, int a1, int a2) { }
    public void updateBCIndications(org.dsi.ifc.carkombi.BCIndications a0, int a1) { }
    public void updateBCCurrentConsumption1(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCCurrentConsumption2(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCCurrentRange2(org.dsi.ifc.global.CarBCCurrentRange a0, int a1) { }
    public void updateBCTotalDistance(org.dsi.ifc.global.CarBCDistance a0, int a1) { }
    public void updateBCShortTermAverageConsumption1(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCShortTermAverageConsumption2(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCShortTermGeneral(org.dsi.ifc.carkombi.BCShortTermGeneralData a0, int a1) { }
    public void updateBCLongTermAverageConsumption2(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCLongTermGeneral(org.dsi.ifc.carkombi.BCLongTermGeneralData a0, int a1) { }
    public void updateBCCycleAverageConsumption1(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCCycleAverageConsumption2(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCCycleGeneral(org.dsi.ifc.carkombi.BCCycleGeneralData a0, int a1) { }
    public void updateBCVZADisplay(boolean a0, int a1) { }
    public void updateBCLifeTipsDisplay(boolean a0, int a1) { }
    public void updateBCConsumerDisplay(boolean a0, int a1) { }
    public void updateBCTankLevel1(org.dsi.ifc.carkombi.BCTankLevel a0, int a1) { }
    public void updateBCTankLevel2(org.dsi.ifc.carkombi.BCTankLevel a0, int a1) { }
    public void updateBCRefuelVolume1(org.dsi.ifc.carkombi.BCRefuelVolume a0, int a1) { }
    public void updateBCRefuelVolume2(org.dsi.ifc.carkombi.BCRefuelVolume a0, int a1) { }
    public void updateBCMenue1Config(org.dsi.ifc.carkombi.BCMenueConfiguration a0, int a1) { }
    public void updateBCMenue2Config(org.dsi.ifc.carkombi.BCMenueConfiguration a0, int a1) { }
    public void updateBCMenue3Config(org.dsi.ifc.carkombi.BCMenueConfiguration a0, int a1) { }
    public void updateBCDigitalSpeed(boolean a0, int a1) { }
    public void updateBCStopwatch(boolean a0, int a1) { }
    public void updateBCVzaMFA(boolean a0, int a1) { }
    public void updateBCSpeedWarning(org.dsi.ifc.carkombi.BCSpeedWarningSettings a0, int a1) { }
    public void updateBCGearRecommendation(boolean a0, int a1) { }
    public void updateBCRearSeatbeltWarning(boolean a0, int a1) { }
    public void updateBCOutsideTemperature(org.dsi.ifc.global.CarBCTemperature a0, int a1) { }
    public void indicateEndOfBCMenuReset(boolean a0) { }
    public void updateBCVehicleStateListTotalNumberOfElements(int a0, int a1) { }
    public void responseVehicleStateUpdateInfo(org.dsi.ifc.carkombi.BCVehicleStateUpdateInfoAH a0, int[] a1) { }
    public void responseVehicleStateListWarningIDdynValueAlternativeText(org.dsi.ifc.carkombi.BCVehicleStateUpdateInfoAH a0, int a1, int a2, java.lang.String a3, java.lang.String a4) { }
    public void responseVehicleStateListWarningIDdynValue(org.dsi.ifc.carkombi.BCVehicleStateUpdateInfoAH a0, org.dsi.ifc.carkombi.ListWarningIDsDynValues[] a1) { }
    public void responseVehicleStateListAlternativeText(org.dsi.ifc.carkombi.BCVehicleStateUpdateInfoAH a0, int a1, java.lang.String a2) { }
    public void responseVehicleStateListdynValue(org.dsi.ifc.carkombi.BCVehicleStateUpdateInfoAH a0, org.dsi.ifc.carkombi.ListDynValues[] a1) { }
    public void responseVehicleStateListPos(org.dsi.ifc.carkombi.BCVehicleStateUpdateInfoAH a0, int[] a1) { }
    public void acknowledgeBcSetFactoryDefault(boolean a0) { }
    public void acknowledgeHUDSetFactoryDefault(boolean a0) { }
    public void acknowledgeDCSetFactoryDefault(boolean a0) { }
    public void acknowledgeBcStatisticsReset(int a0) { }
    public void updateBCStatisticsDistanceAC1(org.dsi.ifc.carkombi.BCStatisticsAC a0, int a1) { }
    public void updateBCStatisticsDistanceAC2(org.dsi.ifc.carkombi.BCStatisticsAC a0, int a1) { }
    public void updateBCStatisticsDistanceRE(org.dsi.ifc.carkombi.BCStatisticsRE a0, int a1) { }
    public void updateBCStatisticsDistanceZE(org.dsi.ifc.carkombi.BCStatisticsZE a0, org.dsi.ifc.carkombi.BCZeroEmissionAbsoluteDistance a1, int a2) { }
    public void updateBCStatisticsDistanceCurrentIntervalAC1(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCStatisticsDistanceCurrentIntervalAC2(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCStatisticsDistanceCurrentIntervalRE(org.dsi.ifc.carkombi.BCAverageRecoveredEnergy a0, int a1) { }
    public void updateBCStatisticsDistanceCurrentIntervalZE(org.dsi.ifc.carkombi.BCZeroEmissionRelative a0, org.dsi.ifc.carkombi.BCZeroEmissionAbsoluteDistance a1, int a2) { }
    public void updateBCStatisticsTimeAC1(org.dsi.ifc.carkombi.BCStatisticsAC a0, int a1) { }
    public void updateBCStatisticsTimeAC2(org.dsi.ifc.carkombi.BCStatisticsAC a0, int a1) { }
    public void updateBCStatisticsTimeRE(org.dsi.ifc.carkombi.BCStatisticsRE a0, int a1) { }
    public void updateBCStatisticsTimeZE(org.dsi.ifc.carkombi.BCStatisticsZE a0, org.dsi.ifc.carkombi.BCZeroEmissionAbsoluteTime a1, int a2) { }
    public void updateBCStatisticsTimeCurrentPeriodAC1(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCStatisticsTimeCurrentPeriodAC2(org.dsi.ifc.global.CarBCConsumption a0, int a1) { }
    public void updateBCStatisticsTimeCurrentPeriodRE(org.dsi.ifc.carkombi.BCAverageRecoveredEnergy a0, int a1) { }
    public void updateBCStatisticsTimeCurrentPeriodZE(org.dsi.ifc.carkombi.BCZeroEmissionRelative a0, org.dsi.ifc.carkombi.BCZeroEmissionAbsoluteTime a1, int a2) { }
    public void updateBCStatisticsConfig(org.dsi.ifc.carkombi.BCStatisticsConfig a0, int a1) { }
    public void updateBCStatisticDistanceEUkm(org.dsi.ifc.carkombi.BCStatisticsDistanceEU a0, int a1) { }
    public void updateBCStatisticDistanceEUmls(org.dsi.ifc.carkombi.BCStatisticsDistanceEU a0, int a1) { }
    public void updateBCComfortPowerConsumption(org.dsi.ifc.carkombi.BCComfortPowerConsumption a0, int a1) { }
    public void updateBCTotalCurrentRange(org.dsi.ifc.global.CarBCDistance a0, int a1) { }
    public void updateBCZeroEmissionDistanceST(org.dsi.ifc.global.CarBCDistance a0, int a1) { }
    public void updateBCZeroEmissionDistanceLT(org.dsi.ifc.global.CarBCDistance a0, int a1) { }
    public void updateBCZeroEmissionDistanceCY(org.dsi.ifc.global.CarBCDistance a0, int a1) { }
    public void updateBCZeroEmissionTimeST(org.dsi.ifc.global.CarBCTime a0, int a1) { }
    public void updateBCZeroEmissionTimeLT(org.dsi.ifc.global.CarBCTime a0, int a1) { }
    public void updateBCZeroEmissionTimeCY(org.dsi.ifc.global.CarBCTime a0, int a1) { }
    public void updateBCMaxValues(org.dsi.ifc.carkombi.BCMaxValues a0, int a1) { }
    public void updateBCResetTimeStampST(org.dsi.ifc.carkombi.BCResetTimeStamp a0, int a1) { }
    public void updateBCResetTimeStampLT(org.dsi.ifc.carkombi.BCResetTimeStamp a0, int a1) { }
    public void updateBCResetTimeStampCY(org.dsi.ifc.carkombi.BCResetTimeStamp a0, int a1) { }
    public void updateBCAstaMFA(boolean a0, int a1) { }
    public void updateHUDViewOptions(org.dsi.ifc.carkombi.HUDViewOptions a0, int a1) { }
    public void updateHUDHeightAdjustment(byte a0, int a1) { }
    public void updateHUDBrightness(byte a0, int a1) { }
    public void updateHUDColour(int a0, int a1, int a2) { }
    public void updateHUDContent(org.dsi.ifc.carkombi.HUDContent a0, int a1) { }
    public void updateHUDInfo(boolean a0, int a1) { }
    public void updateHUDSystemOnOff(boolean a0, int a1) { }
    public void updateHUDRotationAdjustment(int a0, int a1) { }
    public void updateDCViewOptions(org.dsi.ifc.carkombi.DCViewOptions a0, int a1) { }
    public void updateDCBrightness(int a0, int a1) { }
    public void updateDCVolume(int a0, int a1) { }
    public void updateDCElementContentSelectionListTotalNumberOfElements(int a0, int a1) { }
    public void updateDCDisplay1Setup(org.dsi.ifc.carkombi.DCMainItems a0, org.dsi.ifc.carkombi.DCDisplayedAdditionalInfos a1, org.dsi.ifc.carkombi.DCAdditionalInfo a2, org.dsi.ifc.carkombi.DCAdditionalInfo a3, int a4) { }
    public void updateDCDisplay2Setup(org.dsi.ifc.carkombi.DCMainItems a0, org.dsi.ifc.carkombi.DCDisplayedAdditionalInfos a1, org.dsi.ifc.carkombi.DCAdditionalInfo a2, org.dsi.ifc.carkombi.DCAdditionalInfo a3, int a4) { }
    public void updateDCDisplay3Setup(org.dsi.ifc.carkombi.DCMainItems a0, org.dsi.ifc.carkombi.DCDisplayedAdditionalInfos a1, org.dsi.ifc.carkombi.DCAdditionalInfo a2, org.dsi.ifc.carkombi.DCAdditionalInfo a3, int a4) { }
    public void updateDCDisplay1MainSelection(org.dsi.ifc.carkombi.DCMainItems a0, int a1) { }
    public void updateDCDisplay2MainSelection(org.dsi.ifc.carkombi.DCMainItems a0, int a1) { }
    public void updateDCDisplay3MainSelection(org.dsi.ifc.carkombi.DCMainItems a0, int a1) { }
    public void responseDCElementContentSelectionListRAF(org.dsi.ifc.carkombi.DCElementContentSelectionListUpdateInfo a0, int[] a1) { }
    public void responseDCElementContentSelectionListRA1(org.dsi.ifc.carkombi.DCElementContentSelectionListUpdateInfo a0, org.dsi.ifc.carkombi.DCElementContentSelectionListRA1[] a1) { }
    public void responseDCElementContentSelectionListRA2(org.dsi.ifc.carkombi.DCElementContentSelectionListUpdateInfo a0, org.dsi.ifc.carkombi.DCElementContentSelectionListRA2[] a1) { }
    public void updateDCElementContentSelectionListUpdateInfo(org.dsi.ifc.carkombi.DCElementContentSelectionListUpdateInfo a0, int[] a1, int a2) { }
    public void updateDCAdditionalInstrumentSetup(org.dsi.ifc.carkombi.DCAdditionalInstrument a0, int a1) { }
    public void updateDCAdditionalInstrument2Setup(org.dsi.ifc.carkombi.DCAdditionalInstrument2 a0, int a1) { }
    public void updateDCDisplayPresetsListTotalNumberOfElements(int a0, int a1) { }
    public void responseDCDisplayPresetsList(org.dsi.ifc.global.CarArrayListUpdateInfo a0, org.dsi.ifc.carkombi.DCDisplayPresetsListRecord[] a1) { }
    public void updateDCDisplayPresetsListUpdateInfo(org.dsi.ifc.global.CarArrayListUpdateInfo a0, org.dsi.ifc.carkombi.DCDisplayPresetsListRecord[] a1, int a2) { }
    public void updateDCDisplayDependencySetup(org.dsi.ifc.carkombi.DCDisplayDependency a0, int a1) { }
    public void updateDCActiveDisplayPreset(int a0, int a1) { }
    public void updateDCDisplayViewConfiguration(org.dsi.ifc.carkombi.DCDisplayViewConfiguration a0, int a1) { }
    public void updateCompassInfo(int a0, int a1, int a2) { }
    public void updateHUDLicense(boolean a0, int a1) { }
    public void updateDCLEDConfiguration(boolean a0, int a1) { }
}
