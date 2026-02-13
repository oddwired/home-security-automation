package com.kshem.homesecurity.mqtt;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.kshem.homesecurity.exceptions.MqttServiceNotInitialized;
import com.kshem.homesecurity.services.AsyncService;
import com.kshem.homesecurity.services.DeviceHandlerService;
import com.kshem.homesecurity.services.SmsHandlerService;
import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.Map;

public class MqttService {
    private static final String TAG = MqttService.class.getSimpleName();

    private static volatile MqttService instance;
    private final MqttAndroidClient mqttClient;
    private Map<String, MqttTopic> registeredMqttTopics;
    private Handler backgroundHandler;

    private final Runnable subscriptionTask = ()-> {
        if(instance.isConnected()){
            for ( String topicKey : registeredMqttTopics.keySet()) {
                MqttTopic topic = registeredMqttTopics.get(topicKey);
                if(topic.getSubscriptionStatus() == SubscriptionStatus.UNSUBSCRIBED){
                    topic.setSubscriptionStatus(SubscriptionStatus.SUBSCRIBING);
                    try {
                        registerTopics(topic.getTopic(), new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.i(TAG, "Subscribed to topic: "+ topic.getTopic());
                                topic.setSubscriptionStatus(SubscriptionStatus.SUBSCRIBED);
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.i(TAG, "Failed to subscribe to topic: "+ topic.getTopic());
                                topic.setSubscriptionStatus(SubscriptionStatus.UNSUBSCRIBED);
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                        topic.setSubscriptionStatus(SubscriptionStatus.UNSUBSCRIBED);
                    }
                }

                if(topic.getSubscriptionStatus() == SubscriptionStatus.AWAITING_UNSUBSCRIPTION){
                    topic.setSubscriptionStatus(SubscriptionStatus.UNSUBSCRIBING);
                    try {
                        unsubscribe(topic.getTopic(), new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.i(TAG, "Unsubscribed from "+ topic.getTopic());
                                topic.setSubscriptionStatus(SubscriptionStatus.UNSUBSCRIBED);
                                //mqttTopics.remove(topicKey);
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.i(TAG, "Failed to unsubscribe to topic: "+ topic.getTopic());
                                topic.setSubscriptionStatus(SubscriptionStatus.UNSUBSCRIBED);
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                        topic.setSubscriptionStatus(SubscriptionStatus.AWAITING_UNSUBSCRIPTION);
                    }
                }
            }
        }
    };

    private Runnable backgroundRunnable = () -> {
        try{
            // Send to Background
            AsyncService.execute(subscriptionTask);
        }finally {
            backgroundHandler.postDelayed(this.backgroundRunnable, 15000);
        }
    };

    private MqttService(Context context, String serverUri, String clientId){
        this.mqttClient = new MqttAndroidClient(context, serverUri, clientId, new MemoryPersistence());
        this.registeredMqttTopics = new HashMap<>();
        backgroundHandler = new Handler();
        this.startMqtt();
        backgroundHandler.post(backgroundRunnable);
    }

    public static MqttService init(Context context, MqttTopic... mqttTopics){
        if(instance !=null){
            instance.disconnect(null);
        }

        Log.i(TAG, "Initializing MQTT Service");

        String serverIp = LocalPropertyHelper.getMqttServerIp();
        Log.i(TAG, "Server Ip: "+ serverIp);
        int port = LocalPropertyHelper.getMqttServerPort();
        Log.i(TAG, "Server Port: "+ port);
        String clientId = LocalPropertyHelper.getMqttClientId();
        Log.i(TAG, "Client ID: "+ clientId);
        String serverUri = String.format("tcp://%s:%d", serverIp, port);
        Log.i(TAG, "Server Uri: "+ serverUri);


        instance = new MqttService(context, serverUri, clientId);

        instance.registerTopics(mqttTopics);
        return instance;
    }
    public static MqttService getInstance(){
        if(instance == null){
            throw new MqttServiceNotInitialized("Service not initialized");
        }

        return instance;
    }

    public boolean isConnected(){
        try{
            if(mqttClient != null){
                return mqttClient.isConnected();
            }else{
                return false;
            }
        }catch (Exception ex){
            return false;
        }
    }

    private void startMqtt(){
        mqttClient.setTraceEnabled(true);
        Log.i(TAG, "Connecting to MQTT Server");
        connect(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.i(TAG, "MQTT connection success");

                // Reset all subscriptions
                for(MqttTopic topic: registeredMqttTopics.values()){
                    if(topic.getSubscriptionStatus() != SubscriptionStatus.PUBLISH_ONLY){
                        topic.setSubscriptionStatus(SubscriptionStatus.UNSUBSCRIBED);
                    }
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.i(TAG, "Failed to connect to MQTT Server");
                //SmsHandlerService.getInstance().sendSms("Failed to connect to MQTT Server");
                exception.printStackTrace();

                try {
                    registeredMqttTopics.values().forEach((mqttTopic)->{
                        mqttTopic.setSubscriptionStatus(SubscriptionStatus.UNSUBSCRIBED);
                        unsubscribe(mqttTopic.getTopic());
                    });

                    //mqttClient.close();
                    //mqttClient.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                backgroundHandler.postDelayed((Runnable) () -> {
                    startMqtt();
                }, 10000);
            }
        }, new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "MQTT Server Connection lost");
                //SmsHandlerService.getInstance().sendSms("MQTT Server connection lost!");

                backgroundHandler.postDelayed((Runnable) () -> {
                    startMqtt();
                }, 10000);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG, String.format("Received message %s from topic: %s", topic, new String(message.getPayload())));
                if(registeredMqttTopics.containsKey(topic) || registeredMqttTopics.get(topic) != null){
                    Log.i(TAG, "Topic is registered");

                    MqttTopic mqttTopic = registeredMqttTopics.get(topic);
                    mqttTopic.onMessage(new String(message.getPayload()));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void connect(IMqttActionListener actionListener,
                        MqttCallback mqttCallback){
        this.mqttClient.setCallback(mqttCallback);

        String username = LocalPropertyHelper.getMqttUsername();
        String password = LocalPropertyHelper.getMqttPassword();

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setUserName(username);
        connectOptions.setPassword(password.toCharArray());
        connectOptions.setCleanSession(true);

        try{
            this.mqttClient.connect(connectOptions, null, actionListener);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void registerTopics(MqttTopic... mqttTopics){

        for(MqttTopic topic: mqttTopics){
            if(this.registeredMqttTopics.containsKey(topic.getTopic())){
                continue;
            }
            this.registeredMqttTopics.put(topic.getTopic(), topic);
        }
    }

    public MqttTopic getRegisteredTopic(String topic){
        if(registeredMqttTopics.containsKey(topic)){
            return registeredMqttTopics.get(topic);
        }

        return null;
    }

    public void registerTopics(String topic, IMqttActionListener actionListener) throws MqttException{
        if(mqttClient.isConnected()){
            this.mqttClient.subscribe(topic, 1, null, actionListener);
        }
    }

    public void unsubscribe(String topic){
        if(!this.registeredMqttTopics.containsKey(topic)){
            return;
        }

        MqttTopic mqttTopic = registeredMqttTopics.get(topic);
        unsubscribe(mqttTopic);
    }

    public void unsubscribe(MqttTopic topic){
        if(!this.registeredMqttTopics.containsKey(topic.getTopic())){
            return;
        }

        topic.setSubscriptionStatus(SubscriptionStatus.AWAITING_UNSUBSCRIPTION);
    }

    public void unsubscribe(String topic, IMqttActionListener actionListener) throws MqttException{
        this.mqttClient.unsubscribe(topic, null, actionListener);
    }

    public <T extends MqttTopic> void publish(String message, Class<T> tClass){
        AsyncService.execute(()->{
            T tInstance = null;
            for(MqttTopic mqttTopic: registeredMqttTopics.values()){
                if(tClass.isInstance(mqttTopic)){
                    tInstance = (T) mqttTopic;
                    break;
                }
            }

            if(tInstance == null){
                try {
                    tInstance = tClass.newInstance();
                    registerTopics(tInstance);
                } catch (IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
            }

            if(tInstance != null){
                tInstance.publish(message);
            }
        });
    }

    public void publish(String topic, String message, int qos, boolean retained,
                        IMqttActionListener actionListener){
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setPayload(message.getBytes());
        mqttMessage.setQos(qos);
        mqttMessage.setRetained(retained);

        try {
            this.mqttClient.publish(topic, mqttMessage, null, actionListener);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect(IMqttActionListener actionListener){
        try {
            if(actionListener == null){
                mqttClient.disconnect();
            }else{
                mqttClient.disconnect(null, actionListener);
            }

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public MqttAndroidClient getMqttClient() {
        return mqttClient;
    }
}
