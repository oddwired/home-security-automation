package com.kshem.homesecurity.mqtt;

import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;

public class ConfigTopic extends MqttTopic{
    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttControllerConfigTopic();
    }

    @Override
    public void onMessage(String message) {
        try{
            JSONObject jsonObject = new JSONObject(message);
            LocalPropertyHelper.setLockTimeout(jsonObject.getInt("lockTimeout"));
            LocalPropertyHelper.setDataSendInterval(jsonObject.getInt("dataSendInterval"));
            LocalPropertyHelper.setResetInterval(jsonObject.getInt("resetInterval"));
        }catch (JSONException e){
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
