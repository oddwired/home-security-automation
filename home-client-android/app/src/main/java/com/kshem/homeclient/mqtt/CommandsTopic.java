package com.kshem.homeclient.mqtt;

import android.util.Log;

import com.kshem.homeclient.controller.ControllerCommand;
import com.kshem.homeclient.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;

public class CommandsTopic extends MqttTopic{
    private static final String TAG = CommandsTopic.class.getSimpleName();

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttCommandTopic();
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

    private ControllerCommand decodeMessage(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);

        String command = jsonObject.getString("command");

        return ControllerCommand.findByValue(command);
    }
}
