package com.kshem.homeclient.exceptions;

public class MqttServiceNotInitialized extends RuntimeException{
    public MqttServiceNotInitialized(String message) {
        super(message);
    }
}
