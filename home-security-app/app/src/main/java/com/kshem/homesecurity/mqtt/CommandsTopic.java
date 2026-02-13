package com.kshem.homesecurity.mqtt;

import android.util.Log;

import com.kshem.homesecurity.camera.CameraCommand;
import com.kshem.homesecurity.camera.CameraHandlerService;
import com.kshem.homesecurity.controller.ControllerCommand;
import com.kshem.homesecurity.controller.ControllerHandlerService;
import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;

public class CommandsTopic extends MqttTopic{
    private static final String TAG = CommandsTopic.class.getSimpleName();

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttCommandTopic();
    }

    @Override
    public void onMessage(String message) {
        Log.i(TAG, "Received controller command");
        try{

            JSONObject jsonObject = new JSONObject(message);
            String command = jsonObject.getString("command");
            if(jsonObject.has("type") && jsonObject.getString("type").equals("cam")){
                CameraCommand cameraCommand = CameraCommand
                        .findByValue(command);

                if(cameraCommand != null){
                    CameraHandlerService.getInstance().executeCommand(cameraCommand);
                }

            }else{
                ControllerCommand controllerCommand = ControllerCommand.findByValue(command);
                if(controllerCommand != null){
                    ControllerHandlerService.getInstance().sendCommand(controllerCommand);
                }
            }

        }catch (JSONException e){
            Log.i(TAG, "Error decoding command message");
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
