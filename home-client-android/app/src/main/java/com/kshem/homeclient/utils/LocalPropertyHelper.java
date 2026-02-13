package com.kshem.homeclient.utils;

import com.kshem.homeclient.beans.ObjectBox;
import com.kshem.homeclient.exceptions.LocalPropertyNotFound;
import com.kshem.homeclient.exceptions.WrongValueTypeException;
import com.kshem.homeclient.models.LocalProperty;
import com.kshem.homeclient.models.LocalProperty_;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.objectbox.Box;
import io.objectbox.query.QueryBuilder;

public class LocalPropertyHelper {
    public static final int INT_VALUE = 0;
    public static final int LONG_VALUE = 5;
    public static final int DOUBLE_VALUE = 1;
    public static final int STRING_VALUE = 2;
    public static final int BOOLEAN_VALUE = 3;
    public static final int JSON_VALUE = 4;

    public static final String LOCK_TIMEOUT = "lock_timeout";
    public static final String DATA_SEND_INTERVAL = "data_send_interval";
    public static final String RESET_INTERVAL = "reset_interval";
    public static final String MQTT_SERVER_IP = "mqtt_server_ip";
    public static final String MQTT_SERVER_PORT = "mqtt_server_port";
    public static final String MQTT_CLIENT_ID = "mqtt_client_id";
    public static final String MQTT_USERNAME = "mqtt_username";
    public static final String MQTT_PASSWORD = "mqtt_password";
    private static final String MQTT_COMMAND_TOPIC = "mqtt_command_topic";
    private static final String MQTT_CONTROLLER_DATA_TOPIC = "mqtt_controller_data_topic";
    private static final String MQTT_CONTROLLER_CONFIG_TOPIC = "mqtt_controller_config_topic";
    private static final String MQTT_DEVICE_STATUS_TOPIC = "mqtt_device_status_topic";
    private static final String MQTT_MOTION_SENSOR_TOPIC = "mqtt_motion_sensor_topic";
    private static final String MQTT_SMS_TOPIC = "mqtt_sms_topic";

    public static final String LAST_KNOWN_DOOR_STATUS = "last_known_door_status";
    public static final String LAST_KNOWN_DOOR_LOCK_STATUS = "last_known_door_lock_status";
    public static final String LAST_KNOWN_DC_POWER_STATUS = "last_known_dc_power_status";
    public static final String LAST_KNOWN_ALARM_STATUS = "last_known_alarm_status";
    public static final String LAST_KNOWN_LOCK_PAD_STATUS = "last_known_lock_pad_status";
    public static final String LAST_KNOWN_DOOR_LIGHT_STATUS = "last_known_door_light_status";
    public static final String LAST_KNOWN_ALARM_ARM_STATUS = "last_known_alarm_arm_status";
    public static final String LAST_UPDATE = "last_update";

    public static final String LAST_KNOWN_BATTERY_LEVEL = "last_known_battery_level";

    public static int getLockTimeout(){
        return getIntValue(LOCK_TIMEOUT, 30);
    }

    public static void setLockTimeout(int lockTimeout){
        putProperty(LOCK_TIMEOUT, lockTimeout);
    }

    public static int getDataSendInterval(){
        return getIntValue(DATA_SEND_INTERVAL, 10);
    }

    public static void setDataSendInterval(int dataSendInterval){
        putProperty(DATA_SEND_INTERVAL, dataSendInterval);
    }

    public static int getResetInterval(){
        return getIntValue(RESET_INTERVAL, 60 * 10);
    }

    public static void setResetInterval(int resetInterval){
        putProperty(RESET_INTERVAL, resetInterval);
    }

    public static void setMqttServerIp(String ip){
        putProperty(MQTT_SERVER_IP, ip);
    }

    public static String getMqttServerIp(){
        return getStringValue(MQTT_SERVER_IP, "142.93.234.10");
    }

    public static void setMqttServerPort(int port){
        putProperty(MQTT_SERVER_PORT, port);
    }

    public static int getMqttServerPort(){
        return getIntValue(MQTT_SERVER_PORT, 1883);
    }

    public static void setMqttClientId(String clientId){
        putProperty(MQTT_CLIENT_ID, clientId);
    }

    public static String getMqttClientId(){
        return getStringValue(MQTT_CLIENT_ID, "home_client_app");
    }

    public static void setMqttUsername(String username){
        putProperty(MQTT_USERNAME, username);
    }

    public static String getMqttUsername(){
        return getStringValue(MQTT_USERNAME,"");
    }

    public static void setMqttPassword(String password){
        putProperty(MQTT_PASSWORD, password);
    }

    public static String getMqttPassword(){
        return getStringValue(MQTT_PASSWORD, "");
    }

    public static void setMqttCommandTopic(String topic){
        putProperty(MQTT_COMMAND_TOPIC, topic);
    }

    public static String getMqttCommandTopic(){
        return getStringValue(MQTT_COMMAND_TOPIC, "/home/security/controller/commands");
    }

    public static void setMqttControllerDataTopic(String topic){
        putProperty(MQTT_CONTROLLER_DATA_TOPIC, topic);
    }

    public static String getMqttControllerDataTopic(){
        return getStringValue(MQTT_CONTROLLER_DATA_TOPIC, "/home/security/controller/data");
    }

    public static void setMqttControllerConfigTopic(String topic){
        putProperty(MQTT_CONTROLLER_CONFIG_TOPIC, topic);
    }

    public static String getMqttControllerConfigTopic(){
        return getStringValue(MQTT_CONTROLLER_CONFIG_TOPIC, "/home/security/controller/config");
    }

    public static void setMqttDeviceStatusTopic(String topic){
        putProperty(MQTT_DEVICE_STATUS_TOPIC, topic);
    }

    public static String getMqttDeviceStatusTopic(){
        return getStringValue(MQTT_DEVICE_STATUS_TOPIC, "home/security/device/status");
    }

    public static void setMqttMotionSensorTopic(String topic){
        putProperty(MQTT_MOTION_SENSOR_TOPIC, topic);
    }

    public static String getMqttMotionSensorTopic(){
        return getStringValue(MQTT_MOTION_SENSOR_TOPIC, "/home/security/sensors/motion");
    }

    public static String getMqttSmsTopic(){
        return getStringValue(MQTT_SMS_TOPIC, "/home/general/778266014/sms");
    }

    public static void setLastKnownDoorStatus(boolean status){
        putProperty(LAST_KNOWN_DOOR_STATUS, status);
    }

    public static boolean getLastKnownDoorStatus(){
        return getBooleanValue(LAST_KNOWN_DOOR_STATUS, false);
    }

    public static void setLastKnownDoorLockStatus(boolean status){
        putProperty(LAST_KNOWN_DOOR_LOCK_STATUS, status);
    }

    public static boolean getLastKnownDoorLockStatus(){
        return getBooleanValue(LAST_KNOWN_DOOR_LOCK_STATUS, false);
    }

    public static void setLastKnownDcPowerStatus(boolean status){
        putProperty(LAST_KNOWN_DC_POWER_STATUS, status);
    }

    public static boolean getLastKnownDcPowerStatus(){
        return getBooleanValue(LAST_KNOWN_DC_POWER_STATUS, false);
    }

    public static void setLastKnownAlarmStatus(boolean status){
        putProperty(LAST_KNOWN_ALARM_STATUS, status);
    }

    public static boolean getLastKnownAlarmStatus(){
        return getBooleanValue(LAST_KNOWN_ALARM_STATUS, false);
    }

    public static void setLastKnownLockPadStatus(boolean status){
        putProperty(LAST_KNOWN_LOCK_PAD_STATUS, status);
    }

    public static boolean getLastKnownDcLocKPadStatus(){
        return getBooleanValue(LAST_KNOWN_LOCK_PAD_STATUS, false);
    }

    public static void setLastKnownAlarmArmStatus(boolean status){
        putProperty(LAST_KNOWN_ALARM_ARM_STATUS, status);
    }

    public static boolean getLastKnownAlarmArmStatus(){
        return getBooleanValue(LAST_KNOWN_ALARM_ARM_STATUS, true);
    }

    public static void setLastKnownDoorLightStatus(boolean doorLightStatus){
        putProperty(LAST_KNOWN_DOOR_LIGHT_STATUS, doorLightStatus);
    }


    public static boolean getLastKnownDoorLightStatus(){
        return getBooleanValue(LAST_KNOWN_DOOR_LIGHT_STATUS, false);
    }

    public static void setLastUpdate(Date date){
        SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = sm.format(date);

        putProperty(LAST_UPDATE, formattedDate);
    }

    public static Date getLastUpdate(){

        String lastUpdate = getStringValue(LAST_UPDATE, "2021-01-01 00:00:00");
        SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sm.parse(lastUpdate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void setLastKnownBatteryLevel(int level){
        putProperty(LAST_KNOWN_BATTERY_LEVEL, level);
    }

    public static int getLastKnownBatteryLevel(){
        return getIntValue(LAST_KNOWN_BATTERY_LEVEL, -1);
    }


    public static void putProperty(String key, int value){
        putProperty(key, String.valueOf(value), INT_VALUE);
    }

    public static void putProperty(String key, long value){
        putProperty(key, String.valueOf(value), LONG_VALUE);
    }

    public static void putProperty(String key, double value){
        putProperty(key, String.valueOf(value), DOUBLE_VALUE);
    }
    public static void putProperty(String key, String value){
        putProperty(key, value, STRING_VALUE);
    }
    public static void putProperty(String key, boolean value){
        putProperty(key, String.valueOf(value), BOOLEAN_VALUE);
    }
    public static void putProperty(String key, JSONObject value){
        putProperty(key, value.toString(), STRING_VALUE);
    }

    public static int getIntValue(String key, int defaultValue){
        try{
            return getIntValue(key);
        }catch (LocalPropertyNotFound ex){
            putProperty(key, defaultValue);
        }

        return defaultValue;
    }

    public static int getIntValue(String key){
        LocalProperty localProperty = getProperty(key);

        if(localProperty.getValueType() == INT_VALUE){
            return Integer.parseInt(localProperty.getValue());
        }

        throw new WrongValueTypeException(String.format("The property '%s' is not of type 'int'", key));
    }

    public static long getLongValue(String key){
        LocalProperty localProperty = getProperty(key);

        if(localProperty.getValueType() == LONG_VALUE || localProperty.getValueType() == INT_VALUE){
            return Long.parseLong(localProperty.getValue());
        }

        throw new WrongValueTypeException(String.format("The property '%s' is not of type 'long'", key));
    }

    public static long getLongValue(String key, long defaultValue){
        try{
            return getLongValue(key);
        }catch (LocalPropertyNotFound e){
            putProperty(key, defaultValue);
            return defaultValue;
        }
    }

    public static double getDoubleValue(String key){
        LocalProperty localProperty = getProperty(key);

        if(localProperty.getValueType() == DOUBLE_VALUE){
            return Double.parseDouble(localProperty.getValue());
        }

        throw new WrongValueTypeException(String.format("The property '%s' is not of type 'double'", key));
    }
    public static String getStringValue(String key){
        return getProperty(key).getValue();
    }

    public static String getStringValue(String key, String defaultValue){
        try {
            return getStringValue(key);
        }catch (LocalPropertyNotFound e){
            return defaultValue;
        }
    }
    public static boolean getBooleanValue(String key){
        LocalProperty localProperty;

        try {
            localProperty = getProperty(key);
        }catch (LocalPropertyNotFound e){
            putProperty(key, false);

            return false;
        }

        if(localProperty.getValueType() == BOOLEAN_VALUE){
            return Boolean.parseBoolean(localProperty.getValue());
        }

        throw new WrongValueTypeException(String.format("The property '%s' is not of type 'boolean'", key));
    }

    public static boolean getBooleanValue(String key, boolean defaultValue){

        LocalProperty localProperty;

        try {
            localProperty = getProperty(key);
        }catch (LocalPropertyNotFound e){
            putProperty(key, defaultValue);

            return defaultValue;
        }

        if(localProperty.getValueType() == BOOLEAN_VALUE){
            return Boolean.parseBoolean(localProperty.getValue());
        }

        throw new WrongValueTypeException(String.format("The property '%s' is not of type 'boolean'", key));
    }

    public static JSONObject getJsonValue(String key){
        LocalProperty localProperty = getProperty(key);

        if(localProperty.getValueType() == JSON_VALUE){
            try {
                return new JSONObject(localProperty.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        throw new WrongValueTypeException(String.format("The property '%s' is not of type 'JSON'", key));
    }

    private static LocalProperty getProperty(String key){
        Box<LocalProperty> localPropertyBox = ObjectBox.get().boxFor(LocalProperty.class);

        LocalProperty localProperty = localPropertyBox
                .query()
                .equal(LocalProperty_.key, key, QueryBuilder.StringOrder.CASE_SENSITIVE)
                .build().findFirst();

        if(localProperty != null)
            return localProperty;

        throw new LocalPropertyNotFound(String.format("The property '%s' does not exist", key));
    }

    private static void putProperty(String key, String value, int valueType){
        Box<LocalProperty> localPropertyBox = ObjectBox.get().boxFor(LocalProperty.class);
        try{
            LocalProperty localProperty = getProperty(key);
            localProperty.setValue(value);
            localProperty.setValueType(valueType);

            localPropertyBox.put(localProperty);
        }catch (LocalPropertyNotFound e){
            LocalProperty localProperty = new LocalProperty(key, value, valueType);
            localPropertyBox.put(localProperty);
        }
    }
}
