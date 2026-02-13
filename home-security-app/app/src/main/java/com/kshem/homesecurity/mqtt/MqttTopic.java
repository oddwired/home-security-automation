package com.kshem.homesecurity.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

public abstract class MqttTopic {

    protected SubscriptionStatus subscriptionStatus;

    public MqttTopic(){
        this.subscriptionStatus = getDefaultSubscription();
    }

    abstract public String getTopic();
    abstract public void onMessage(String message);
    abstract public void onSubscribe();
    abstract public void onUnsubscribe();
    abstract public void onPublishMessageSuccess(String message, IMqttToken token);
    abstract public void onPublishMessageFailed(String message, IMqttToken token, Throwable exception);

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public SubscriptionStatus getDefaultSubscription(){
        return SubscriptionStatus.UNSUBSCRIBED;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        switch (subscriptionStatus){
            case SUBSCRIBED:
                onSubscribe();
                break;
            case UNSUBSCRIBED:
                if(this.subscriptionStatus == SubscriptionStatus.SUBSCRIBED){
                    onUnsubscribe();
                }
                break;
        }

        this.subscriptionStatus = subscriptionStatus;
    }

    public void publish(String message){
        MqttService mqttService = MqttService.getInstance();
        if(mqttService.isConnected()){
            mqttService.publish(this.getTopic(), message, 1, false, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    onPublishMessageSuccess(message, asyncActionToken);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    onPublishMessageFailed(message, asyncActionToken, exception);
                }
            });
        }
    }
}
