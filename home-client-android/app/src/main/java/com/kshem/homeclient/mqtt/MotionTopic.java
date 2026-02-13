package com.kshem.homeclient.mqtt;

import android.content.Context;
import android.content.Intent;

import com.kshem.homeclient.ui.MainActivity;
import com.kshem.homeclient.utils.LocalPropertyHelper;
import com.kshem.homeclient.utils.Utils;

import org.eclipse.paho.client.mqttv3.IMqttToken;

public class MotionTopic extends MqttTopic{

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttMotionSensorTopic();
    }

    @Override
    public void onMessage(String message) {
        Utils.createNotification(getContext(), new Intent(context, MainActivity.class),
                "Motion detected", "Motion", false);
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
