package com.kshem.homeclient.controller;

import android.support.annotation.NonNull;

public enum ControllerCommand {
    TURN_OFF_DC("dc0"),
    TURN_ON_DC("dc1"),
    UNLOCK_DOOR("d0"),
    LOCK_DOOR("d1"),
    TURN_ON_ALARM("al1"),
    TURN_OFF_ALARM("al0"),
    TURN_ON_DOOR_LIGHT("dl1"),
    TURN_OFF_DOOR_LIGHT("dl0"),
    RESET_CONTROLLER("rst"),
    TURN_ON_CAMERA("c1"),
    TURN_OFF_CAMERA("c0"),
    RESET_CAMERA("crst"),
    DISABLE_ALARM("al2"),
    ENABLE_ALARM("al3");

    private final String command;

    private ControllerCommand(String command){
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

    public boolean equals(ControllerCommand command){
        return this.command.equals(command.getCommand());
    }

    public static ControllerCommand findByValue(String value){
        for(ControllerCommand controllerCommand: values()){
            if(controllerCommand.getCommand().equals(value)){
                return controllerCommand;
            }
        }

        return null;
    }
}
