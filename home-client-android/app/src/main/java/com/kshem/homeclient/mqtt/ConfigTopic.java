package com.kshem.homeclient.mqtt;

import com.kshem.homeclient.utils.LocalPropertyHelper;

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
