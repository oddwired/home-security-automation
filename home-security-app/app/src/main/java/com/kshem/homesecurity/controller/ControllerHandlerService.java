package com.kshem.homesecurity.controller;

import com.kshem.homesecurity.server.UdpSender;
import com.kshem.homesecurity.utils.LocalPropertyHelper;

import java.util.LinkedList;

public class ControllerHandlerService {
    private static volatile ControllerHandlerService instance;

    private LinkedList<ControllerCommand> controllerCommands;
    private String controllerIp;

    private ControllerHandlerService(){
        controllerCommands = new LinkedList<>();
    }

    public static ControllerHandlerService getInstance(){
        if(instance == null){
            synchronized (ControllerHandlerService.class){
                if(instance == null){
                    instance = new ControllerHandlerService();
                }
            }
        }

        return instance;
    }

    public void sendToQueue(ControllerCommand controllerCommand){
        if(!this.controllerCommands.contains(controllerCommand)){
            controllerCommands.add(controllerCommand);
        }
    }

    public String getFormattedCommands(){
        StringBuilder commandString = new StringBuilder();
        for (ControllerCommand controllerCommand: controllerCommands) {
            commandString.append(controllerCommand.getCommand());
            commandString.append(",");
        }
        // Remove the last comma
        //commandString.deleteCharAt(commandString.lastIndexOf(","));
        return commandString.toString();
    }

    public String getControllerIp() {
        return controllerIp;
    }

    public void setControllerIp(String controllerIp) {
        this.controllerIp = controllerIp;
    }

    public boolean sendCommand(ControllerCommand command){
        if(this.controllerIp == null){
            return false;
        }

        if(command.equals(ControllerCommand.TURN_OFF_DC)){
            LocalPropertyHelper.setPowerState(false);
        }else if(command.equals(ControllerCommand.TURN_ON_DC)){
            LocalPropertyHelper.setPowerState(true);
        }

        if(command.equals(ControllerCommand.ENABLE_ALARM)){
            LocalPropertyHelper.setAlarmArmedState(true);
        }else if(command.equals(ControllerCommand.DISABLE_ALARM)){
            LocalPropertyHelper.setAlarmArmedState(false);
        }

        UdpSender.send(this.controllerIp, 8085, command.getCommand());

        return true;
    }
}
