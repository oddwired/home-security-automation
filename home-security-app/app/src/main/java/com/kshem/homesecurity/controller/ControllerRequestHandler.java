package com.kshem.homesecurity.controller;

import android.util.Log;

import com.kshem.homesecurity.beans.ObjectBox;
import com.kshem.homesecurity.models.ControllerRequest;
import com.kshem.homesecurity.mqtt.DataTopic;
import com.kshem.homesecurity.mqtt.MqttService;
import com.kshem.homesecurity.server.RequestHandler;
import com.kshem.homesecurity.services.AsyncService;
import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.json.JSONObject;

import java.util.Date;

import fi.iki.elonen.NanoHTTPD;
import io.objectbox.Box;

public class ControllerRequestHandler extends RequestHandler {

    private static final String TAG = ControllerRequestHandler.class.getSimpleName();

    @Override
    public String getEndpoint() {
        return "/data";
    }

    @Override
    public NanoHTTPD.Response post(NanoHTTPD.IHTTPSession ihttpSession, String body) {
        switch (ihttpSession.getUri()){
            case "/data":
                Log.i(TAG, "Received data request");

                ControllerHandlerService controllerHandlerService =
                        ControllerHandlerService.getInstance();
                controllerHandlerService.setControllerIp(ihttpSession.getRemoteIpAddress());

                //saveRequest(body);
                publishData(body);

                return NanoHTTPD.newFixedLengthResponse(buildResponse());
            case "/logs":
                Log.i(TAG, "Received log request");
            default:
                Log.i(TAG, "Default response");
                return NanoHTTPD.newFixedLengthResponse("OK");
        }
    }

    @Override
    public NanoHTTPD.Response get(NanoHTTPD.IHTTPSession ihttpSession) {
        switch (ihttpSession.getUri()){
            case "/commands":
                Log.i(TAG, "Received commands request");
                return NanoHTTPD.newFixedLengthResponse(buildResponse());
            default:
                return NanoHTTPD.newFixedLengthResponse("OK");
        }
    }

    private String buildResponse(){
        String response = ControllerHandlerService.getInstance().getFormattedCommands() + ":"
                + ControllerConfig.getFormattedConfig();

        Log.i(TAG,"Response: "+ response);
        return response;
    }

    private void saveRequest(String data){
        new Thread(() -> {
            Box<ControllerRequest> controllerRequestBox = ObjectBox.get()
                    .boxFor(ControllerRequest.class);

            ControllerRequest controllerRequest = new ControllerRequest(new Date(), data,
                    null);
            controllerRequestBox.put(controllerRequest);
        }).start();
    }

    private void publishData(String data){
        AsyncService.execute(() -> {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("data", data);

                MqttService.getInstance().publish(jsonObject.toString(), DataTopic.class);

            }catch (Exception e){
                e.printStackTrace();
            }

        });
    }
}
