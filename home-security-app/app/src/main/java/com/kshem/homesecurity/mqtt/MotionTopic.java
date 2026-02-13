package com.kshem.homesecurity.mqtt;

import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttToken;

public class MotionTopic extends MqttTopic{

    @Override
    public SubscriptionStatus getDefaultSubscription() {
        return SubscriptionStatus.PUBLISH_ONLY;
    }

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttMotionSensorTopic();
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
