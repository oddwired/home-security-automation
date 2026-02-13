package com.kshem.homeclient.mqtt;

import com.kshem.homeclient.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONObject;

public class DeviceInfoTopic extends MqttTopic{

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttDeviceStatusTopic();
    }

    @Override
    public void onMessage(String message) {
        try{
            JSONObject jsonObject = new JSONObject(message);
            int batteryLevel = jsonObject.getInt("batteryLevel");

            LocalPropertyHelper.setLastKnownBatteryLevel(batteryLevel);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSubscribe() {

    }

    @Override
    public void onUnsubscribe() {

    }

    @Override
    public void onPublishMessageSuccess(String message, IMqttToken token) {

    }

    @Override
    public void onPublishMessageFailed(String message, IMqttToken token, Throwable exception) {

    }
}
