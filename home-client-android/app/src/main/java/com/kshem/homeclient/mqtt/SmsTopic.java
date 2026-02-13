package com.kshem.homeclient.mqtt;


import com.kshem.homeclient.beans.ObjectBox;
import com.kshem.homeclient.models.Sms;
import com.kshem.homeclient.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;

import io.objectbox.Box;

public class SmsTopic extends MqttTopic{

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttSmsTopic();
    }

    @Override
    public void onMessage(String message) {
        try {
            Sms sms = Sms.parseJson(message);

            Box<Sms> smsBox = ObjectBox.get().boxFor(Sms.class);
            smsBox.put(sms);

        } catch (JSONException e) {
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
