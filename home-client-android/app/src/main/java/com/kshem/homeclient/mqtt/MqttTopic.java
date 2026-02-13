package com.kshem.homeclient.mqtt;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

public abstract class MqttTopic {

    private static final String TAG = MqttTopic.class.getSimpleName();
    // Bind to UI
    public interface OnDataCallback{
        void onMessageReceived(String message);
    }

    protected Context context;
    protected SubscriptionStatus subscriptionStatus;
    protected OnDataCallback callback;
    public MqttTopic(){
        this.subscriptionStatus = getDefaultSubscription();
    }

    abstract public String getTopic();
    public void onMessage(String message){
        if(callback != null){
            Log.i(TAG, "Callback has been called");
            callback.onMessageReceived(message);
        }else{
            Log.i(TAG, "Callback is null");
        }
    }
    abstract public void onSubscribe();
    abstract public void onUnsubscribe();
    abstract public void onPublishMessageSuccess(String message, IMqttToken token);
    abstract public void onPublishMessageFailed(String message, IMqttToken token, Throwable exception);

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

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
        MqttService.getInstance().publish(this.getTopic(), message, 1, false, new IMqttActionListener() {
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

    public void setCallback(OnDataCallback callback) {
        this.callback = callback;
    }
}
