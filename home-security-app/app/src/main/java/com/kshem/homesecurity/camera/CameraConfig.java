package com.kshem.homesecurity.camera;

import com.kshem.homesecurity.utils.LocalPropertyHelper;

public class CameraConfig {
    private static final String SEC_LIGHT_START_TIME = "sec_light_start_time";
    private static final String SEC_LIGHT_END_TIME = "sec_light_end_time";
    private static final String SEC_LIGHT_ENABLED = "sec_light_enabled";
    private static final String SEC_LIGHT_TURNED_ON = "sec_light_turned_on";

    public static String getSecLightStartTime(){
        return LocalPropertyHelper.getStringValue(SEC_LIGHT_START_TIME, "19:00:00");
    }

    public static void setSecLightStartTime(String startTime){
        LocalPropertyHelper.putProperty(SEC_LIGHT_START_TIME, startTime);
    }

    public static String getSecLightEndTime(){
        return LocalPropertyHelper.getStringValue(SEC_LIGHT_END_TIME, "06:30:00");
    }

    public static void setSecLightEndTime(String endTime){
        LocalPropertyHelper.putProperty(SEC_LIGHT_END_TIME, endTime);
    }

    public static boolean isSecLightEnabled(){
        return LocalPropertyHelper.getBooleanValue(SEC_LIGHT_ENABLED, true);
    }

    public static void setSecLightEnabled(boolean enabled){
        LocalPropertyHelper.putProperty(SEC_LIGHT_ENABLED, enabled);
    }

    public static boolean isSecLightTurnedOn(){
        return LocalPropertyHelper.getBooleanValue(SEC_LIGHT_TURNED_ON, false);
    }

    public static void setSecLightTurnedOn(boolean turnedOn){
        LocalPropertyHelper.putProperty(SEC_LIGHT_TURNED_ON, turnedOn);
    }
}
