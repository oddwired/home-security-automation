package com.kshem.homesecurity.exceptions;

public class MqttServiceNotInitialized extends RuntimeException{
    public MqttServiceNotInitialized(String message) {
        super(message);
    }
}
