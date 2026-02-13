package com.kshem.homesecurity.mqtt;

import android.util.Log;

import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttToken;

public class DataTopic extends MqttTopic{
    private static final String TAG = DataTopic.class.getSimpleName();

    @Override
    public SubscriptionStatus getDefaultSubscription() {
        return SubscriptionStatus.PUBLISH_ONLY;
    }

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttControllerDataTopic();
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
        Log.i(TAG, "Message published: "+ message);
    }

    @Override
    public void onPublishMessageFailed(String message, IMqttToken token, Throwable exception) {
        Log.i(TAG, "Failed to publish data message: "+ message);
    }
}
