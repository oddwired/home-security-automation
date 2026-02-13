package com.kshem.homesecurity.controller;

import com.kshem.homesecurity.utils.LocalPropertyHelper;

public class ControllerConfig {
    /*Format: lock_timeout,data_send_interval,reset_interval*/
    public static String getFormattedConfig(){
        int lockTimeout = LocalPropertyHelper.getLockTimeout();
        int dataSendInterval = LocalPropertyHelper.getDataSendInterval();
        int resetInterval = LocalPropertyHelper.getResetInterval();
        int powerState = LocalPropertyHelper.getPowerState() ? 1 : 0;
        int alarmArmedState = LocalPropertyHelper.getAlarmArmedState() ? 1 : 0;
        int alarmTimeout = LocalPropertyHelper.getAlarmTimeout();

        return String.format("%d,%d,%d,%d,%d,%d", lockTimeout,
                dataSendInterval, resetInterval, powerState, alarmArmedState,alarmTimeout);
    }
}
