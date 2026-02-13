package com.kshem.homesecurity.camera;

import android.util.Log;

import com.kshem.homesecurity.mqtt.MqttService;
import com.kshem.homesecurity.server.RequestHandler;
import com.kshem.homesecurity.services.AsyncService;

import fi.iki.elonen.NanoHTTPD;

public class CameraRequestHandler extends RequestHandler {
    private static final String TAG = CameraRequestHandler.class.getSimpleName();

    @Override
    public String getEndpoint() {
        return "/cam";
    }

    @Override
    public NanoHTTPD.Response post(NanoHTTPD.IHTTPSession ihttpSession, String body) {
        Log.i(TAG,"Received request from camera: "+ body);

        CameraHandlerService cameraHandlerService = CameraHandlerService.getInstance();
        cameraHandlerService.setCameraIp(ihttpSession.getRemoteIpAddress());
        switch (ihttpSession.getUri()){
            case "/cam/motion":
                //TODO: Trigger capture;
                Log.i(TAG, "Motion detected");
                cameraHandlerService.onMotionDetected();
                break;
            case "/cam/ping":
                //cameraHandlerService.onMotionDetected();
                return NanoHTTPD.newFixedLengthResponse(cameraHandlerService.onPing(body));
            case "/cam/boot":
                AsyncService.execute(cameraHandlerService::sendInitialConfig);
                break;
        }
        return NanoHTTPD.newFixedLengthResponse("OK");
    }

    @Override
    public NanoHTTPD.Response get(NanoHTTPD.IHTTPSession ihttpSession) {
        return null;
    }
}
