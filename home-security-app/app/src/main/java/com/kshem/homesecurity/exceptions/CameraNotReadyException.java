package com.kshem.homesecurity.exceptions;

public class CameraNotReadyException extends RuntimeException{
    public CameraNotReadyException(String message) {
        super(message);
    }
}
