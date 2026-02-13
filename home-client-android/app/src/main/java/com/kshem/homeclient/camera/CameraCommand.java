package com.kshem.homeclient.camera;

import android.support.annotation.NonNull;

public enum CameraCommand {
    CAPTURE("cap");

    private final String command;

    private CameraCommand(String command){
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @NonNull
    @Override
    public String toString() {
        return this.command;
    }

    public boolean equals(CameraCommand command){
        return this.command.equals(command.getCommand());
    }

    public static CameraCommand findByValue(String value){
        for(CameraCommand cameraCommand: values()){
            if(cameraCommand.getCommand().equals(value)){
                return cameraCommand;
            }
        }

        return null;
    }
}
