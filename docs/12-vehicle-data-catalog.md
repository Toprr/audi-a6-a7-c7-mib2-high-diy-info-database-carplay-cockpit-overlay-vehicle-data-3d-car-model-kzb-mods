# 12: Vehicle data catalog: what the MMI receives on an A6 C7.5 (K3663)

Generated from two drives with our read-only logger [VehLog 0.4](11-vehicle-data-logger.md) (about 30 and 32 minutes, ignition on, CarPlay by cable). VehLog subscribed to every `ATTR_*` attribute of 16 vehicle DSI services; 13 of them exist on this car (`DSICarEco`, `DSICarHybrid`, `DSICarStopWatch`, `DSICarAuxHeaterCooler` do not).

**How to read it**

- **valid**: the last argument of every DSI update is a valid flag (`1` valid, `2` invalid, `0` unknown). `yes` = the car delivered a valid value at least once, `no` = only invalid, `n/a` = the method has no flag (acknowledge/response calls).
- **live** = the value changed while driving, **once** = sent once after subscribing.
- **updates** = how often the method was called in the longer of the two drives.
- One car, one equipment level, one software train. Another car may deliver more or less. Values are not listed on purpose (odometer, times, settings); see [docs/11](11-vehicle-data-logger.md) for the useful ones.
- Not logged at all, on purpose: phone, messages, contacts, navigation, media, online services.

**Summary:** 717 methods called, 145 with a valid value, 37 of them changing while driving.

<details><summary><b>DSICarAirCondition</b>: 142 methods, 22 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateAirconAirCirculationAuto` | yes | once | 1 |
| `updateAirconAirCirculationSensitivity` | yes | once | 1 |
| `updateAirconAirDistributionZone1` | yes | once | 1 |
| `updateAirconAirDistributionZone2` | yes | once | 1 |
| `updateAirconAirDistributionZone3` | yes | once | 1 |
| `updateAirconAirDistributionZone4` | yes | once | 1 |
| `updateAirconAirVolumeZone1` | yes | live | 54 |
| `updateAirconAirVolumeZone2` | yes | live | 32 |
| `updateAirconAirVolumeZone3` | yes | live | 23 |
| `updateAirconAirVolumeZone4` | yes | live | 23 |
| `updateAirconBlowerCompensation` | yes | once | 1 |
| `updateAirconClimateStyleZone1` | yes | once | 1 |
| `updateAirconClimateStyleZone2` | yes | once | 1 |
| `updateAirconContent` | yes | live | 5 |
| `updateAirconFootwellTempZone1` | yes | once | 1 |
| `updateAirconFootwellTempZone2` | yes | once | 1 |
| `updateAirconHeater` | yes | once | 1 |
| `updateAirconRearControl` | yes | once | 1 |
| `updateAirconTempZone1` | yes | live | 5 |
| `updateAirconTempZone2` | yes | live | 3 |
| `updateAirconTempZone3` | yes | live | 3 |
| `updateAirconTempZone4` | yes | live | 3 |
| `updateAirconAC` | no |  | 1 |
| `updateAirconAirCirculationMan` | no |  | 1 |
| `updateAirconAirCirculationMiddleExhaustion` | no |  | 1 |
| `updateAirconAirDistributionZone5` | no |  | 1 |
| `updateAirconAirDistributionZone6` | no |  | 1 |
| `updateAirconAirQuality` | no |  | 1 |
| `updateAirconAirVolumeZone5` | no |  | 1 |
| `updateAirconAirVolumeZone6` | no |  | 1 |
| `updateAirconBodyCloseMeasuresZone1` | no |  | 1 |
| `updateAirconBodyCloseMeasuresZone2` | no |  | 1 |
| `updateAirconBodyCloseMeasuresZone3` | no |  | 1 |
| `updateAirconBodyCloseMeasuresZone4` | no |  | 1 |
| `updateAirconBodyCloseMeasuresZone5` | no |  | 1 |
| `updateAirconBodyCloseMeasuresZone6` | no |  | 1 |
| `updateAirconClimateStateZone1` | no |  | 1 |
| `updateAirconClimateStateZone2` | no |  | 1 |
| `updateAirconClimateStateZone3` | no |  | 1 |
| `updateAirconClimateStateZone4` | no |  | 1 |
| `updateAirconClimateStateZone5` | no |  | 1 |
| `updateAirconClimateStateZone6` | no |  | 1 |
| `updateAirconClimateStyleZone3` | no |  | 1 |
| `updateAirconClimateStyleZone4` | no |  | 1 |
| `updateAirconClimateStyleZone5` | no |  | 1 |
| `updateAirconClimateStyleZone6` | no |  | 1 |
| `updateAirconDefrost` | no |  | 1 |
| `updateAirconEcoAC` | no |  | 1 |
| `updateAirconFootwellTempZone3` | no |  | 1 |
| `updateAirconFootwellTempZone4` | no |  | 1 |
| `updateAirconFootwellTempZone5` | no |  | 1 |
| `updateAirconFootwellTempZone6` | no |  | 1 |
| `updateAirconFreshAirConfig` | no |  | 1 |
| `updateAirconFreshAirState` | no |  | 1 |
| `updateAirconFrontWindowHeater` | no |  | 1 |
| `updateAirconFrontWindowHeaterAuto` | no |  | 1 |
| `updateAirconIndirectVentilation` | no |  | 1 |
| `updateAirconIndividualClimatisationZone1` | no |  | 1 |
| `updateAirconIndividualClimatisationZone2` | no |  | 1 |
| `updateAirconIndividualClimatisationZone3` | no |  | 1 |
| `updateAirconIndividualClimatisationZone4` | no |  | 1 |
| `updateAirconIndividualClimatisationZone5` | no |  | 1 |
| `updateAirconIndividualClimatisationZone6` | no |  | 1 |
| `updateAirconIonisatorZone1` | no |  | 1 |
| `updateAirconIonisatorZone2` | no |  | 1 |
| `updateAirconIonisatorZone3` | no |  | 1 |
| `updateAirconIonisatorZone4` | no |  | 1 |
| `updateAirconIonisatorZone5` | no |  | 1 |
| `updateAirconIonisatorZone6` | no |  | 1 |
| `updateAirconMaxAC` | no |  | 1 |
| `updateAirconMaxDefrost` | no |  | 1 |
| `updateAirconNozzleListTotalNumberOfElementsRow1` | no |  | 1 |
| `updateAirconNozzleListTotalNumberOfElementsRow2` | no |  | 1 |
| `updateAirconNozzleListTotalNumberOfElementsRow3` | no |  | 1 |
| `updateAirconNozzleListUpdateInfoRow1` | no |  | 1 |
| `updateAirconNozzleListUpdateInfoRow2` | no |  | 1 |
| `updateAirconNozzleListUpdateInfoRow3` | no |  | 1 |
| `updateAirconNozzleStatusRow1` | no |  | 1 |
| `updateAirconNozzleStatusRow2` | no |  | 1 |
| `updateAirconNozzleStatusRow3` | no |  | 1 |
| `updateAirconPopupTime` | no |  | 1 |
| `updateAirconPureAir` | no |  | 1 |
| `updateAirconRearAuxHeater` | no |  | 1 |
| `updateAirconRearControlFondPlus` | no |  | 1 |
| `updateAirconRearWindowHeater` | no |  | 1 |
| `updateAirconResidualHeat` | no |  | 1 |
| `updateAirconSeatHeaterDistributionZone1` | no |  | 1 |
| `updateAirconSeatHeaterDistributionZone2` | no |  | 1 |
| `updateAirconSeatHeaterDistributionZone3` | no |  | 1 |
| `updateAirconSeatHeaterDistributionZone4` | no |  | 1 |
| `updateAirconSeatHeaterDistributionZone5` | no |  | 1 |
| `updateAirconSeatHeaterDistributionZone6` | no |  | 1 |
| `updateAirconSeatHeaterZone1` | no |  | 1 |
| `updateAirconSeatHeaterZone2` | no |  | 1 |
| `updateAirconSeatHeaterZone3` | no |  | 1 |
| `updateAirconSeatHeaterZone4` | no |  | 1 |
| `updateAirconSeatHeaterZone5` | no |  | 1 |
| `updateAirconSeatHeaterZone6` | no |  | 1 |
| `updateAirconSeatNeckHeaterZone1` | no |  | 1 |
| `updateAirconSeatNeckHeaterZone2` | no |  | 1 |
| `updateAirconSeatNeckHeaterZone3` | no |  | 1 |
| `updateAirconSeatNeckHeaterZone4` | no |  | 1 |
| `updateAirconSeatNeckHeaterZone5` | no |  | 1 |
| `updateAirconSeatNeckHeaterZone6` | no |  | 1 |
| `updateAirconSeatSurfaceHeaterZone1` | no |  | 1 |
| `updateAirconSeatSurfaceHeaterZone2` | no |  | 1 |
| `updateAirconSeatSurfaceHeaterZone3` | no |  | 1 |
| `updateAirconSeatSurfaceHeaterZone4` | no |  | 1 |
| `updateAirconSeatSurfaceHeaterZone5` | no |  | 1 |
| `updateAirconSeatSurfaceHeaterZone6` | no |  | 1 |
| `updateAirconSeatVentilationDistributionZone1` | no |  | 1 |
| `updateAirconSeatVentilationDistributionZone2` | no |  | 1 |
| `updateAirconSeatVentilationDistributionZone3` | no |  | 1 |
| `updateAirconSeatVentilationDistributionZone4` | no |  | 1 |
| `updateAirconSeatVentilationDistributionZone5` | no |  | 1 |
| `updateAirconSeatVentilationDistributionZone6` | no |  | 1 |
| `updateAirconSeatVentilationZone1` | no |  | 1 |
| `updateAirconSeatVentilationZone2` | no |  | 1 |
| `updateAirconSeatVentilationZone3` | no |  | 1 |
| `updateAirconSeatVentilationZone4` | no |  | 1 |
| `updateAirconSeatVentilationZone5` | no |  | 1 |
| `updateAirconSeatVentilationZone6` | no |  | 1 |
| `updateAirconSideWindowDefrost` | no |  | 1 |
| `updateAirconSolar` | no |  | 1 |
| `updateAirconSteeringWheelHeater` | no |  | 1 |
| `updateAirconSuppressVisualisation` | no |  | 1 |
| `updateAirconSynchronisation` | no |  | 1 |
| `updateAirconSystemOnOffRow1` | no |  | 1 |
| `updateAirconSystemOnOffRow2` | no |  | 1 |
| `updateAirconSystemOnOffRow3` | no |  | 1 |
| `updateAirconTempStepZone1` | no |  | 1 |
| `updateAirconTempStepZone2` | no |  | 1 |
| `updateAirconTempStepZone3` | no |  | 1 |
| `updateAirconTempStepZone4` | no |  | 1 |
| `updateAirconTempStepZone5` | no |  | 1 |
| `updateAirconTempStepZone6` | no |  | 1 |
| `updateAirconTempZone5` | no |  | 1 |
| `updateAirconTempZone6` | no |  | 1 |
| `updateAirconViewOptionsMaster` | n/a |  | 1 |
| `updateAirconViewOptionsRow1` | n/a |  | 1 |
| `updateAirconViewOptionsRow2` | n/a |  | 1 |
| `updateAirconViewOptionsRow3` | no |  | 1 |

</details>

<details><summary><b>DSICarComfort</b>: 66 methods, 11 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateDoorLockingAutoLock` | yes | once | 1 |
| `updateDoorLockingAutoUnlock` | yes | once | 1 |
| `updateDoorLockingClBootLock` | yes | once | 1 |
| `updateDoorLockingComfortOpenSettings` | yes | once | 1 |
| `updateDoorLockingMirrorProtection` | yes | once | 1 |
| `updateDoorLockingUnlockingMode` | yes | once | 1 |
| `updateRGSBeltPretensionDataFront` | yes | once | 1 |
| `updateRGSBeltPretensionDataRear` | yes | once | 1 |
| `updateWiperRainSensorConfig` | yes | once | 1 |
| `updateWiperRainSensorOnOff` | yes | once | 1 |
| `updateWiperServicePosition` | yes | once | 1 |
| `updateBrakeAutoHold` | no |  | 1 |
| `updateBrakeElectricalParking` | no |  | 1 |
| `updateBrakeEscMode` | no |  | 1 |
| `updateBrakeHdcMode` | no |  | 1 |
| `updateBrakeViewOptions` | no |  | 1 |
| `updateDoorLockingActiveUser` | no |  | 1 |
| `updateDoorLockingBlindsControl` | no |  | 1 |
| `updateDoorLockingBootClose` | no |  | 1 |
| `updateDoorLockingBootOpen` | no |  | 1 |
| `updateDoorLockingClBootOpen` | no |  | 1 |
| `updateDoorLockingConfirmation` | no |  | 1 |
| `updateDoorLockingLockStatus` | no |  | 1 |
| `updateDoorLockingMessage` | no |  | 1 |
| `updateDoorLockingRainClosing` | no |  | 1 |
| `updateDoorLockingRearBlind` | no |  | 1 |
| `updateDoorLockingTheftWarningSettings` | no |  | 1 |
| `updateDoorLockingUserListTotalNumberOfElements` | no |  | 1 |
| `updateDoorLockingUserListUpdateInfo` | no |  | 1 |
| `updateDoorLockingUserProfileOnOff` | no |  | 1 |
| `updateDoorLockingViewOptions` | n/a |  | 1 |
| `updateDoorLockingWindowAutoClose` | no |  | 1 |
| `updateDoorLockingWindowStatus` | no |  | 1 |
| `updateEasyEntrySteeringColumn` | no |  | 1 |
| `updateMirrorDimming` | no |  | 1 |
| `updateMirrorFolding` | no |  | 1 |
| `updateMirrorHeating` | no |  | 1 |
| `updateMirrorLowering` | no |  | 1 |
| `updateMirrorSyncAdjust` | no |  | 1 |
| `updateMirrorViewOptions` | no |  | 1 |
| `updateRDKDifferentialPressure` | no |  | 1 |
| `updateRDKPressureLevel` | no |  | 1 |
| `updateRDKResidualBatteryLifetime` | no |  | 1 |
| `updateRDKSpeedLimit` | no |  | 1 |
| `updateRDKSystemOnOff` | no |  | 1 |
| `updateRDKTireDisplay` | no |  | 1 |
| `updateRDKTireSetupSelectedTire` | no |  | 1 |
| `updateRDKTireSetupTireList` | no |  | 1 |
| `updateRDKViewOptions` | n/a |  | 1 |
| `updateRGSLocalHazardDetection` | no |  | 1 |
| `updateRGSPreCrashSystem` | no |  | 1 |
| `updateRGSPreSenseSystem` | no |  | 1 |
| `updateRGSPreSenseWarning` | no |  | 1 |
| `updateRGSViewOptions` | n/a |  | 1 |
| `updateUGDOButtonListTotalNumberOfElements` | no |  | 1 |
| `updateUGDOButtonListUpdateInfo` | no |  | 1 |
| `updateUGDOContent` | no |  | 1 |
| `updateUGDODestinationReached` | no |  | 1 |
| `updateUGDOLearningData` | no |  | 1 |
| `updateUGDOOpenDoor` | no |  | 1 |
| `updateUGDOVersionData` | no |  | 1 |
| `updateUGDOViewOptions` | no |  | 1 |
| `updateWiperRearWiping` | no |  | 1 |
| `updateWiperTearsWiping` | no |  | 1 |
| `updateWiperViewOptions` | n/a |  | 14 |
| `updateWiperWinterPosition` | no |  | 1 |

</details>

<details><summary><b>DSICarDriverAssistance</b>: 81 methods, 8 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateAWVSystem` | yes | once | 1 |
| `updateAWVWarning` | yes | once | 1 |
| `updateHCAInterventionStyle` | yes | once | 1 |
| `updateHCAToleranceLevel` | yes | once | 1 |
| `updateMKESystemOnOff` | yes | once | 1 |
| `updateMKEViewOptions` | yes | once | 1 |
| `updateSWABrightness` | yes | once | 1 |
| `updateTSDRoadSignFilter` | yes | once | 1 |
| `updateACCCurveAssist` | no |  | 1 |
| `updateACCDefaultMode` | no |  | 1 |
| `updateACCDistanceWarning` | no |  | 1 |
| `updateACCDrivingProgram` | no |  | 1 |
| `updateACCGongState` | no |  | 1 |
| `updateACCGongVolume` | no |  | 1 |
| `updateACCSpeedLimitAdoption` | no |  | 1 |
| `updateACCSpeedLimitOffset` | no |  | 1 |
| `updateACCTimeGap` | no |  | 1 |
| `updateACCTrafficJamAssist` | no |  | 1 |
| `updateACCViewOptions` | n/a |  | 1 |
| `updateAWVBrakeJerk` | no |  | 1 |
| `updateAWVDistanceWarning` | no |  | 1 |
| `updateAWVEmergencyBrake` | no |  | 1 |
| `updateAWVGong` | no |  | 1 |
| `updateAWVGongVolume` | no |  | 1 |
| `updateAWVViewOptions` | n/a |  | 1 |
| `updateAWVWarningTimegap` | no |  | 1 |
| `updateCurveAssistSystemOnOff` | no |  | 1 |
| `updateFTASensorData` | no |  | 1 |
| `updateFTASystemOnOff` | no |  | 1 |
| `updateFTAViewOptions` | no |  | 1 |
| `updateLDWHCASystemOnOff` | no |  | 1 |
| `updateLDWHCAViewOptions` | n/a |  | 1 |
| `updateLDWHCAWarningSound` | no |  | 1 |
| `updateLDWSteeringWheelVibration` | no |  | 1 |
| `updateLDWWarningTime` | no |  | 1 |
| `updateNVActivation` | no |  | 1 |
| `updateNVBrightness` | no |  | 1 |
| `updateNVColorPA` | no |  | 1 |
| `updateNVContrast` | no |  | 1 |
| `updateNVDesignPA` | no |  | 1 |
| `updateNVDisplay` | no |  | 1 |
| `updateNVObjectDetection` | no |  | 1 |
| `updateNVSound` | no |  | 1 |
| `updateNVSymbol` | no |  | 1 |
| `updateNVSystem` | no |  | 1 |
| `updateNVViewOptions` | no |  | 1 |
| `updateNVWarningTimegap` | no |  | 1 |
| `updateNVZoomPanning` | no |  | 1 |
| `updatePACCCoastingPercentage` | no |  | 1 |
| `updatePACCDrivingProgram` | no |  | 1 |
| `updatePACCMaxSpeed` | no |  | 1 |
| `updatePACCMeanConsumption` | no |  | 1 |
| `updatePACCMeanVelocity` | no |  | 1 |
| `updatePACCSensibility` | no |  | 1 |
| `updatePACCSystemState` | no |  | 1 |
| `updatePAConfigInformation` | no |  | 1 |
| `updatePAConfigWarning` | no |  | 1 |
| `updatePASystemOnOff` | no |  | 1 |
| `updatePAViewOptions` | no |  | 1 |
| `updatePAWarningTimegap` | no |  | 1 |
| `updateSWAExitAssist` | no |  | 1 |
| `updateSWAFrequency` | no |  | 1 |
| `updateSWAGongState` | no |  | 1 |
| `updateSWAGongVolume` | no |  | 1 |
| `updateSWARCTA` | no |  | 1 |
| `updateSWARCTASensorData` | no |  | 1 |
| `updateSWASystem` | no |  | 1 |
| `updateSWAViewOptions` | n/a |  | 1 |
| `updateSWAWarningTime` | no |  | 1 |
| `updateTSDSign1` | no |  | 1 |
| `updateTSDSign2` | no |  | 1 |
| `updateTSDSign3` | no |  | 1 |
| `updateTSDSign4` | no |  | 1 |
| `updateTSDSign5` | no |  | 1 |
| `updateTSDSpeedWarningAcoustics` | no |  | 1 |
| `updateTSDSpeedWarningThreshold` | no |  | 1 |
| `updateTSDSystemMessages` | no |  | 1 |
| `updateTSDSystemOnOff` | no |  | 1 |
| `updateTSDTrailerDetection` | no |  | 1 |
| `updateTSDTrailerSpeedLimit` | no |  | 1 |
| `updateTSDViewOptions` | n/a |  | 1 |

</details>

<details><summary><b>DSICarDrivingCharacteristics</b>: 50 methods, 3 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateCharismaActiveProfile` | yes | once | 1 |
| `updateCharismaContent` | yes | once | 1 |
| `updateCharismaListUpdateInfo` | yes | once | 1 |
| `updateCharismaActiveOperationMode` | no |  | 1 |
| `updateCharismaProgButton` | no |  | 1 |
| `updateCharismaSound` | no |  | 1 |
| `updateCharismaTrailerDetection` | no |  | 1 |
| `updateCharismaTrailerSetting` | no |  | 1 |
| `updateCharismaViewOptions` | n/a |  | 1 |
| `updateSoundOnOff` | no |  | 1 |
| `updateSoundStyle` | no |  | 1 |
| `updateSoundSystemOnOff` | no |  | 1 |
| `updateSoundViewOptions` | no |  | 1 |
| `updateSpoilerActuation` | no |  | 1 |
| `updateSpoilerMessages` | no |  | 1 |
| `updateSpoilerPositionSelection` | no |  | 1 |
| `updateSpoilerState` | no |  | 1 |
| `updateSpoilerSystemOnOff` | no |  | 1 |
| `updateSpoilerViewOptions` | no |  | 1 |
| `updateSuspensionControlAccessibleAirProfiles` | no |  | 1 |
| `updateSuspensionControlAccessibleDRCProfiles` | no |  | 1 |
| `updateSuspensionControlActiveMode` | no |  | 1 |
| `updateSuspensionControlActiveProfile` | no |  | 1 |
| `updateSuspensionControlActuatorInfo` | no |  | 1 |
| `updateSuspensionControlCarJackMode` | no |  | 1 |
| `updateSuspensionControlCurrentLevel` | no |  | 1 |
| `updateSuspensionControlHeightInfo` | no |  | 1 |
| `updateSuspensionControlLiftMode` | no |  | 1 |
| `updateSuspensionControlLoadingMode` | no |  | 1 |
| `updateSuspensionControlOperationMessages` | no |  | 1 |
| `updateSuspensionControlSnowChainMode` | no |  | 1 |
| `updateSuspensionControlTargetLevel` | no |  | 1 |
| `updateSuspensionControlTrailerMode` | no |  | 1 |
| `updateSuspensionControlVehicleStateControl` | no |  | 1 |
| `updateSuspensionControlVehicleStatus` | no |  | 1 |
| `updateSuspensionControlViewOptions` | no |  | 1 |
| `updateTADContent` | no |  | 1 |
| `updateTADCurrentPitchAngle` | no |  | 1 |
| `updateTADCurrentRollAngle` | no |  | 1 |
| `updateTADNegMaxPitchAngle` | no |  | 1 |
| `updateTADNegMaxRollAngle` | no |  | 1 |
| `updateTADPosMaxPitchAngle` | no |  | 1 |
| `updateTADPosMaxRollAngle` | no |  | 1 |
| `updateTADVehicleInfo` | no |  | 1 |
| `updateTADViewOptions` | no |  | 1 |
| `updateeABCEasyEntry` | no |  | 1 |
| `updateeABCPitchControl` | no |  | 1 |
| `updateeABCPreview` | no |  | 1 |
| `updateeABCPreviewState` | no |  | 1 |
| `updateeABCSpecialPosition` | no |  | 1 |

</details>

<details><summary><b>DSICarKombi</b>: 108 methods, 16 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateBCCurrentRange1` | yes | live | 7 |
| `updateBCDigitalSpeed` | yes | once | 1 |
| `updateBCLongTermAverageConsumption1` | yes | once | 1 |
| `updateBCOilTemperature` | yes | once | 1 |
| `updateBCShortTermAverageConsumption1` | yes | live | 280 |
| `updateBCShortTermGeneral` | yes | live | 148 |
| `updateBCSpeedWarning` | yes | once | 1 |
| `updateBCStatisticsConfig` | yes | once | 1 |
| `updateBCStatisticsTimeAC1` | yes | live | 7 |
| `updateBCStatisticsTimeCurrentPeriodAC1` | yes | live | 2029 |
| `updateBCStopwatch` | yes | once | 1 |
| `updateBCTotalDistance` | yes | live | 34 |
| `updateBCVZADisplay` | yes | live | 3 |
| `updateSIADailyAverageMileage` | yes | once | 1 |
| `updateSIAOilInspection` | yes | once | 1 |
| `updateSIAServiceData` | yes | once | 1 |
| `updateBCAstaMFA` | no |  | 1 |
| `updateBCComfortPowerConsumption` | no |  | 1 |
| `updateBCConsumerDisplay` | no |  | 1 |
| `updateBCCoolantTemperature` | no |  | 1 |
| `updateBCCurrentConsumption1` | no |  | 1 |
| `updateBCCurrentConsumption2` | no |  | 1 |
| `updateBCCurrentRange2` | no |  | 1 |
| `updateBCCycleAverageConsumption1` | no |  | 1 |
| `updateBCCycleAverageConsumption2` | no |  | 1 |
| `updateBCCycleGeneral` | no |  | 1 |
| `updateBCGearRecommendation` | no |  | 1 |
| `updateBCIndications` | no |  | 1 |
| `updateBCLifeTipsDisplay` | no |  | 1 |
| `updateBCLongTermAverageConsumption2` | no |  | 1 |
| `updateBCLongTermGeneral` | no |  | 1 |
| `updateBCMaxValues` | no |  | 1 |
| `updateBCMenue1Config` | n/a |  | 1 |
| `updateBCMenue2Config` | n/a |  | 1 |
| `updateBCMenue3Config` | no |  | 1 |
| `updateBCOilTemperatureValue` | no |  | 1 |
| `updateBCOutsideTemperature` | no |  | 1 |
| `updateBCRearSeatbeltWarning` | no |  | 1 |
| `updateBCRefuelVolume1` | no |  | 1 |
| `updateBCRefuelVolume2` | no |  | 1 |
| `updateBCResetTimeStampCY` | no |  | 1 |
| `updateBCResetTimeStampLT` | no |  | 1 |
| `updateBCResetTimeStampST` | no |  | 1 |
| `updateBCShortTermAverageConsumption2` | no |  | 1 |
| `updateBCStatisticDistanceEUkm` | no |  | 1 |
| `updateBCStatisticDistanceEUmls` | no |  | 1 |
| `updateBCStatisticsDistanceAC1` | no |  | 1 |
| `updateBCStatisticsDistanceAC2` | no |  | 1 |
| `updateBCStatisticsDistanceCurrentIntervalAC1` | no |  | 1 |
| `updateBCStatisticsDistanceCurrentIntervalAC2` | no |  | 1 |
| `updateBCStatisticsDistanceCurrentIntervalRE` | no |  | 1 |
| `updateBCStatisticsDistanceCurrentIntervalZE` | no |  | 1 |
| `updateBCStatisticsDistanceRE` | no |  | 1 |
| `updateBCStatisticsDistanceZE` | no |  | 1 |
| `updateBCStatisticsTimeAC2` | no |  | 1 |
| `updateBCStatisticsTimeCurrentPeriodAC2` | no |  | 1 |
| `updateBCStatisticsTimeCurrentPeriodRE` | no |  | 1 |
| `updateBCStatisticsTimeCurrentPeriodZE` | no |  | 1 |
| `updateBCStatisticsTimeRE` | no |  | 1 |
| `updateBCStatisticsTimeZE` | no |  | 1 |
| `updateBCTankLevel1` | no |  | 1 |
| `updateBCTankLevel2` | no |  | 1 |
| `updateBCTotalCurrentRange` | no |  | 1 |
| `updateBCVehicleStateListTotalNumberOfElements` | no |  | 1 |
| `updateBCViewOptions` | n/a |  | 1 |
| `updateBCVzaMFA` | no |  | 1 |
| `updateBCZeroEmissionDistanceCY` | no |  | 1 |
| `updateBCZeroEmissionDistanceLT` | no |  | 1 |
| `updateBCZeroEmissionDistanceST` | no |  | 1 |
| `updateBCZeroEmissionTimeCY` | no |  | 1 |
| `updateBCZeroEmissionTimeLT` | no |  | 1 |
| `updateBCZeroEmissionTimeST` | no |  | 1 |
| `updateCompassInfo` | no |  | 1 |
| `updateDCActiveDisplayPreset` | no |  | 1 |
| `updateDCAdditionalInstrument2Setup` | no |  | 1 |
| `updateDCAdditionalInstrumentSetup` | no |  | 1 |
| `updateDCBrightness` | no |  | 1 |
| `updateDCDisplay1MainSelection` | no |  | 1 |
| `updateDCDisplay1Setup` | no |  | 1 |
| `updateDCDisplay2MainSelection` | no |  | 1 |
| `updateDCDisplay2Setup` | no |  | 1 |
| `updateDCDisplay3MainSelection` | no |  | 1 |
| `updateDCDisplay3Setup` | no |  | 1 |
| `updateDCDisplayDependencySetup` | no |  | 1 |
| `updateDCDisplayPresetsListTotalNumberOfElements` | no |  | 1 |
| `updateDCDisplayPresetsListUpdateInfo` | no |  | 1 |
| `updateDCDisplayViewConfiguration` | no |  | 1 |
| `updateDCElementContentSelectionListTotalNumberOfElements` | no |  | 1 |
| `updateDCElementContentSelectionListUpdateInfo` | no |  | 1 |
| `updateDCLEDConfiguration` | no |  | 1 |
| `updateDCViewOptions` | no |  | 1 |
| `updateDCVolume` | no |  | 1 |
| `updateHUDBrightness` | no |  | 1 |
| `updateHUDColour` | no |  | 1 |
| `updateHUDContent` | no |  | 1 |
| `updateHUDHeightAdjustment` | no |  | 1 |
| `updateHUDInfo` | no |  | 1 |
| `updateHUDLicense` | no |  | 1 |
| `updateHUDRotationAdjustment` | no |  | 1 |
| `updateHUDSystemOnOff` | no |  | 1 |
| `updateHUDViewOptions` | no |  | 1 |
| `updateSIADistanceAirFilterUser` | no |  | 1 |
| `updateSIADistanceOilFilterUser` | no |  | 1 |
| `updateSIADistanceOilUser` | no |  | 1 |
| `updateSIAHistoryListTotalNumberOfElements` | no |  | 1 |
| `updateSIAHistoryListUpdateInfo` | no |  | 1 |
| `updateSIAInspectionDistanceUser` | no |  | 1 |
| `updateSIAViewOptions` | n/a |  | 1 |

</details>

<details><summary><b>DSICarLight</b>: 48 methods, 6 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateExtLightComingHome` | yes | once | 1 |
| `updateExtLightLeavingHome` | yes | once | 1 |
| `updateExtLightMaskedHighBeam` | yes | once | 1 |
| `updateExtLightSwitchOnSensitivity` | yes | once | 1 |
| `updateExtLightTourist` | yes | once | 1 |
| `updateIntLightIlluminationSet1` | yes | once | 1 |
| `updateExtLightAdaptive` | no |  | 1 |
| `updateExtLightAutomaticLight` | no |  | 1 |
| `updateExtLightDaylight` | no |  | 1 |
| `updateExtLightGlidingSystem` | no |  | 1 |
| `updateExtLightHeadLightSystem` | no |  | 1 |
| `updateExtLightHeadlightRange` | no |  | 1 |
| `updateExtLightLampErrorDetection` | no |  | 1 |
| `updateExtLightLampErrorDetectionTrailer` | no |  | 1 |
| `updateExtLightLaserLight` | no |  | 1 |
| `updateExtLightMotorwayBlinking` | no |  | 1 |
| `updateExtLightSensorErrorDetection` | no |  | 1 |
| `updateExtLightSignatureLight` | no |  | 1 |
| `updateExtLightViewOptions` | n/a |  | 12 |
| `updateIntLightActiveProfile` | no |  | 1 |
| `updateIntLightAmbientLightColor` | no |  | 1 |
| `updateIntLightBrightness` | no |  | 1 |
| `updateIntLightColour` | no |  | 1 |
| `updateIntLightContourLightColor` | no |  | 1 |
| `updateIntLightDoorContact` | no |  | 1 |
| `updateIntLightEnvironment` | no |  | 1 |
| `updateIntLightFollowUpTime` | no |  | 1 |
| `updateIntLightIlluminationProfile1` | no |  | 1 |
| `updateIntLightIlluminationProfile2` | no |  | 1 |
| `updateIntLightIlluminationProfile3` | no |  | 1 |
| `updateIntLightIlluminationProfile4` | no |  | 1 |
| `updateIntLightIlluminationProfile5` | no |  | 1 |
| `updateIntLightIlluminationProfile6` | no |  | 1 |
| `updateIntLightIlluminationProfile7` | no |  | 1 |
| `updateIntLightIlluminationProfile8` | no |  | 1 |
| `updateIntLightIlluminationSet2` | no |  | 1 |
| `updateIntLightIlluminationSet3` | no |  | 1 |
| `updateIntLightIlluminationSet4` | no |  | 1 |
| `updateIntLightIlluminationSet5` | no |  | 1 |
| `updateIntLightIlluminationSet6` | no |  | 1 |
| `updateIntLightIlluminationSet7` | no |  | 1 |
| `updateIntLightIlluminationSet8` | no |  | 1 |
| `updateIntLightRGBColorListTotalNumberOfElements` | no |  | 1 |
| `updateIntLightRGBColorListUpdateInfo` | no |  | 1 |
| `updateIntLightSpeed` | no |  | 1 |
| `updateIntLightState` | no |  | 1 |
| `updateIntLightTemperature` | no |  | 1 |
| `updateIntLightViewOptions` | n/a |  | 1 |

</details>

<details><summary><b>DSICarParkingSystem</b>: 79 methods, 29 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updatePDCDefaultParkingMode` | yes | once | 1 |
| `updatePDCDistanceValuesFront` | yes | once | 1 |
| `updatePDCDistanceValuesLeft` | yes | once | 1 |
| `updatePDCDistanceValuesRear` | yes | once | 1 |
| `updatePDCDistanceValuesRight` | yes | once | 1 |
| `updatePDCFailure` | yes | once | 1 |
| `updatePDCFrequenceFront` | yes | once | 1 |
| `updatePDCFrequenceRear` | yes | once | 1 |
| `updatePDCInfo` | yes | once | 1 |
| `updatePDCSoundFront` | yes | once | 1 |
| `updatePDCSoundRear` | yes | once | 1 |
| `updatePDCSoundReproduction` | yes | once | 1 |
| `updatePDCStatusLevelFront` | yes | live | 3 |
| `updatePDCStatusLevelLeft` | yes | live | 3 |
| `updatePDCStatusLevelRear` | yes | live | 3 |
| `updatePDCStatusLevelRight` | yes | live | 3 |
| `updatePDCSteeringInformation` | yes | live | 163 |
| `updatePDCVolumeFront` | yes | once | 1 |
| `updatePDCVolumeRear` | yes | once | 1 |
| `updateParkingPopupContent` | yes | live | 4 |
| `updateVPSBrightness` | yes | once | 1 |
| `updateVPSCameraStates` | yes | once | 1 |
| `updateVPSColor` | yes | once | 1 |
| `updateVPSContrast` | yes | once | 1 |
| `updateVPSDefaultModeRV` | yes | once | 1 |
| `updateVPSFailure` | yes | once | 1 |
| `updateVPSFollowUpTime` | yes | once | 1 |
| `updateVPSSystemOnOff` | yes | once | 1 |
| `updateVPSVideoInfo` | yes | live | 6 |
| `updateARACurrentTrailerAngle` | no |  | 1 |
| `updateARAFailure` | no |  | 1 |
| `updateARAInfo` | no |  | 1 |
| `updateARATargetTrailerAngle` | no |  | 1 |
| `updatePDCContinueDrivingAssist` | no |  | 1 |
| `updatePDCCrashWarning` | no |  | 1 |
| `updatePDCDistanceValuesFrontExt` | no |  | 1 |
| `updatePDCDistanceValuesRearExt` | no |  | 1 |
| `updatePDCFlankGuard` | no |  | 1 |
| `updatePDCFrequenceLeft` | no |  | 1 |
| `updatePDCFrequenceRight` | no |  | 1 |
| `updatePDCIPAMessage` | no |  | 1 |
| `updatePDCIpaConfig` | no |  | 1 |
| `updatePDCManeuverAssist` | no |  | 1 |
| `updatePDCManeuverAssistConfig` | no |  | 1 |
| `updatePDCManeuverAssistMessage` | no |  | 1 |
| `updatePDCManeuverAssistState` | no |  | 1 |
| `updatePDCMute` | no |  | 1 |
| `updatePDCOPSAutoActivation` | no |  | 1 |
| `updatePDCOPSVisualisationPosition` | no |  | 1 |
| `updatePDCOffroadMode` | no |  | 1 |
| `updatePDCPLABargraph` | no |  | 1 |
| `updatePDCPLAMessage` | no |  | 1 |
| `updatePDCPLAParkmodeSelection` | no |  | 1 |
| `updatePDCPLAStatus` | no |  | 1 |
| `updatePDCPLASystemState` | no |  | 1 |
| `updatePDCParkboxVisualisation` | no |  | 1 |
| `updatePDCPiloPaSystemState` | no |  | 1 |
| `updatePDCSoundLeft` | no |  | 1 |
| `updatePDCSoundRight` | no |  | 1 |
| `updatePDCStatusLevelFrontExt` | no |  | 1 |
| `updatePDCStatusLevelRearExt` | no |  | 1 |
| `updatePDCSystemOnOff` | no |  | 1 |
| `updatePDCTrailerHitched` | no |  | 1 |
| `updatePDCVolumeLeft` | no |  | 1 |
| `updatePDCVolumeRight` | no |  | 1 |
| `updatePDCWallDetection` | no |  | 1 |
| `updateParkingSystemViewOptions` | n/a |  | 1 |
| `updateVPS3DBirdview` | no |  | 1 |
| `updateVPSCameraCleaning` | no |  | 1 |
| `updateVPSDefaultModeBV` | no |  | 1 |
| `updateVPSDefaultModeFV` | no |  | 1 |
| `updateVPSDefaultModeSV` | no |  | 1 |
| `updateVPSDefaultView` | no |  | 1 |
| `updateVPSDynamicParkingMode` | no |  | 1 |
| `updateVPSExtCamConfig` | no |  | 1 |
| `updateVPSExtCamManActivation` | no |  | 1 |
| `updateVPSOPSOverlay` | no |  | 1 |
| `updateVPSRimProtection` | no |  | 1 |
| `updateVPSSystemState` | no |  | 1 |

</details>

<details><summary><b>DSICarSeat</b>: 68 methods, 7 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateSeatContent` | yes | once | 1 |
| `updateSeatFrontLeftStopButton` | yes | once | 1 |
| `updateSeatFrontRightStopButton` | yes | once | 1 |
| `updateSeatSwitcherDataBack1RL` | yes | once | 1 |
| `updateSeatSwitcherDataDown1RL` | yes | once | 1 |
| `updateSeatSwitcherDataForward1RL` | yes | once | 1 |
| `updateSeatSwitcherDataUp1RL` | yes | once | 1 |
| `updateSeatAdjustment1RL` | no |  | 1 |
| `updateSeatAdjustment1RR` | no |  | 1 |
| `updateSeatAdjustment2RL` | no |  | 1 |
| `updateSeatAdjustment2RR` | no |  | 1 |
| `updateSeatCoDriverSettingsFromRearActivation` | no |  | 1 |
| `updateSeatCodriverSettingsFromDriver` | no |  | 1 |
| `updateSeatCodriverSettingsFromRear` | no |  | 1 |
| `updateSeatEasyEntryFrontLeft` | no |  | 1 |
| `updateSeatEasyEntryFrontRight` | no |  | 1 |
| `updateSeatEasyEntryRearLeft` | no |  | 1 |
| `updateSeatEasyEntryRearRight` | no |  | 1 |
| `updateSeatFoldHeadRestRearCoDriver` | no |  | 1 |
| `updateSeatFoldHeadRestRearDriver` | no |  | 1 |
| `updateSeatMassageData1RL` | no |  | 1 |
| `updateSeatMassageData1RR` | no |  | 1 |
| `updateSeatMassageData2RL` | no |  | 1 |
| `updateSeatMassageData2RR` | no |  | 1 |
| `updateSeatMassageSwitcher1RL` | no |  | 1 |
| `updateSeatMassageSwitcher1RR` | no |  | 1 |
| `updateSeatMassageSwitcher2RL` | no |  | 1 |
| `updateSeatMassageSwitcher2RR` | no |  | 1 |
| `updateSeatPneumaticCodriverSettingsFromDriver` | no |  | 1 |
| `updateSeatPneumaticContent` | no |  | 1 |
| `updateSeatPneumaticMassageData1RL` | no |  | 1 |
| `updateSeatPneumaticMassageData1RR` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataBack1RL` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataBack1RR` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataDown1RL` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataDown1RR` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataForward1RL` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataForward1RR` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataUp1RL` | no |  | 1 |
| `updateSeatPneumaticSwitcherDataUp1RR` | no |  | 1 |
| `updateSeatPneumaticViewOptions` | no |  | 1 |
| `updateSeatPremiumMassageData1RL` | no |  | 1 |
| `updateSeatPremiumMassageData1RR` | no |  | 1 |
| `updateSeatPremiumMassageData2RL` | no |  | 1 |
| `updateSeatPremiumMassageData2RR` | no |  | 1 |
| `updateSeatPremiumMassageSwitcher1RL` | no |  | 1 |
| `updateSeatPremiumMassageSwitcher1RR` | no |  | 1 |
| `updateSeatPremiumMassageSwitcher2RL` | no |  | 1 |
| `updateSeatPremiumMassageSwitcher2RR` | no |  | 1 |
| `updateSeatRadioKeyAutomatic` | n/a |  | 1 |
| `updateSeatRearLeftStopButton` | no |  | 1 |
| `updateSeatRearRightStopButton` | no |  | 1 |
| `updateSeatRestSeatStatus` | no |  | 1 |
| `updateSeatSpecialPosition` | no |  | 1 |
| `updateSeatSpecialPositionRearCoDriver` | no |  | 1 |
| `updateSeatSwitcherDataBack1RR` | no |  | 1 |
| `updateSeatSwitcherDataBack2RL` | no |  | 1 |
| `updateSeatSwitcherDataBack2RR` | no |  | 1 |
| `updateSeatSwitcherDataDown1RR` | no |  | 1 |
| `updateSeatSwitcherDataDown2RL` | no |  | 1 |
| `updateSeatSwitcherDataDown2RR` | no |  | 1 |
| `updateSeatSwitcherDataForward1RR` | no |  | 1 |
| `updateSeatSwitcherDataForward2RL` | no |  | 1 |
| `updateSeatSwitcherDataForward2RR` | no |  | 1 |
| `updateSeatSwitcherDataUp1RR` | no |  | 1 |
| `updateSeatSwitcherDataUp2RL` | no |  | 1 |
| `updateSeatSwitcherDataUp2RR` | no |  | 1 |
| `updateSeatViewOptions` | n/a |  | 1 |

</details>

<details><summary><b>DSICarTimeUnitsLanguage</b>: 24 methods, 18 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateClockDate` | yes | once | 1 |
| `updateClockDayLightSaving` | yes | once | 1 |
| `updateClockDayLightSavingData` | yes | once | 1 |
| `updateClockFormat` | yes | once | 1 |
| `updateClockGPSSyncData` | yes | once | 1 |
| `updateClockSource` | yes | once | 1 |
| `updateClockTime` | yes | live | 1901 |
| `updateClockTimeSourcesAvailable` | yes | once | 1 |
| `updateClockTimeZoneOffset` | yes | once | 1 |
| `updateConsumptionPetrolUnit` | yes | once | 1 |
| `updateDateFormat` | yes | once | 1 |
| `updateDistanceUnit` | yes | once | 1 |
| `updateMenuLanguage` | yes | once | 1 |
| `updatePressureUnit` | yes | once | 1 |
| `updateSpeedUnit` | yes | once | 1 |
| `updateTemperatureUnit` | yes | once | 1 |
| `updateUTCOffset` | yes | once | 1 |
| `updateVolumeUnit` | yes | once | 1 |
| `updateClockViewOptions` | n/a |  | 1 |
| `updateConsumptionElectricUnit` | no |  | 1 |
| `updateConsumptionGasUnit` | no |  | 1 |
| `updateSkin` | no |  | 1 |
| `updateUnitmasterViewOptions` | n/a |  | 1 |
| `updateWeightUnit` | no |  | 1 |

</details>

<details><summary><b>DSICarVehicleStates</b>: 15 methods, 4 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateOilLevelData` | yes | once | 1 |
| `updateOilLevelViewOption` | yes | once | 1 |
| `updateSemiStaticVehicleDataViewOptions` | yes | once | 1 |
| `updateVehicleInfoViewOptions` | yes | once | 1 |
| `updateDrvSchoolSystem` | no |  | 1 |
| `updateDynamicVehicleInfoHighFrequent` | n/a |  | 1171 |
| `updateDynamicVehicleInfoHighFrequentViewOptions` | n/a |  | 1 |
| `updateDynamicVehicleInfoMidFrequent` | n/a |  | 1 |
| `updateDynamicVehicleInfoMidFrequentViewOptions` | n/a |  | 1 |
| `updateDynamicVehicleInfoSCR` | no |  | 1 |
| `updateKeyData` | n/a |  | 1 |
| `updateKeyViewOption` | n/a |  | 1 |
| `updateSemiStaticVehicleData` | no |  | 1 |
| `updateVINData` | n/a |  | 1 |
| `updateVINViewOption` | n/a |  | 1 |

</details>

<details><summary><b>DSIGeneralVehicleStates</b>: 26 methods, 16 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateAcousticParkingSystem` | yes | live | 3 |
| `updateAirbagData` | yes | once | 1 |
| `updateBTBondingVelocityThreshold` | yes | live | 18 |
| `updateBWSVelocityThreshold` | yes | live | 12 |
| `updateBrowserBordBookVelocityThreshold` | yes | live | 11 |
| `updateBrowserWebVelocityThreshold` | yes | live | 12 |
| `updateCarVelocityThreshold` | yes | live | 10 |
| `updateDestinationInputVelocityThreshold` | yes | live | 11 |
| `updateDisplayDayNightDesign` | yes | live | 3 |
| `updateMessagingVelocityThreshold` | yes | live | 18 |
| `updatePersonalizationStatus` | yes | once | 1 |
| `updateReverseGear` | yes | live | 5 |
| `updateTLOViewOptions` | yes | once | 1 |
| `updateTVVelocityThreshold` | yes | live | 11 |
| `updateTankInfo` | yes | once | 1 |
| `updateVehicleStandstill` | yes | live | 12 |
| `updateBrowserSlideShowVelocityThreshold` | no |  | 1 |
| `updateBrowserTravelAgentVelocityThreshold` | no |  | 1 |
| `updateDSSSViewOption` | no |  | 1 |
| `updateDimmedHeadlight` | no |  | 1 |
| `updateEmergencyAssistVolLowering` | no |  | 1 |
| `updateHDDVelocityThreshold` | no |  | 1 |
| `updateParkingBrake` | no |  | 1 |
| `updateRadiotextVelocityThreshold` | no |  | 1 |
| `updateServiceKeyData` | n/a |  | 1 |
| `updateServiceKeyViewOption` | n/a |  | 1 |

</details>

<details><summary><b>DSIPowerManagement</b>: 10 methods, 5 valid</summary>

| method | valid | live | updates |
|---|---|---|---|
| `updateClampSignal` | yes | once | 1 |
| `updateLastOn` | yes | once | 1 |
| `updatePowerManagementState` | yes | once | 1 |
| `updateRVCActive` | yes | live | 3 |
| `updateSplashScreenAnimation` | yes | once | 1 |
| `updateBEMState` | no |  | 1 |
| `updateChildLockState` | no |  | 1 |
| `updatePowerManagementStateRight` | no |  | 1 |
| `updateTStandbyPopup` | no |  | 1 |
| `updateTelMaxPopup` | no |  | 1 |

</details>

