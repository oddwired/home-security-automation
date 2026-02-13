package com.kshem.homesecurity.camera;

import android.util.Log;

import com.kshem.homesecurity.exceptions.CameraNotReadyException;
import com.kshem.homesecurity.mqtt.MotionTopic;
import com.kshem.homesecurity.mqtt.MqttService;
import com.kshem.homesecurity.utils.NetworkManager;
import com.kshem.homesecurity.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraHandlerService {
    private static final String TAG = CameraHandlerService.class.getSimpleName();

    private final String CONFIG_URL = "http://%s/control?var=%s&val=%d";
    public interface HandlerCallback{
        void onImageCaptured();
    }

    private static CameraHandlerService instance;

    private String cameraIp;
    private int port = 80;
    private int streamPort = 81;

    private CameraHandlerService(){

    }

    public static CameraHandlerService getInstance(){
        if(instance == null){
            synchronized (CameraHandlerService.class){
                if(instance == null){
                    instance = new CameraHandlerService();
                }
            }
        }

        return instance;
    }

    private void updateFrameSize(){
        String configUrl = String.format(CONFIG_URL, cameraIp, "framesize", 7);
        try {
            NetworkManager.getInstance().get(configUrl, null, null, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateGain(){
        String configUrl = String.format(CONFIG_URL, cameraIp, "gainceiling", 3);
        try {
            NetworkManager.getInstance().get(configUrl, null, null, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendInitialConfig(){
        if(cameraIp == null){
            throw new CameraNotReadyException("Camera IP not found");
        }

        updateFrameSize();
        updateGain();
    }

    public String onPing(String data){
        boolean secEnabled = CameraConfig.isSecLightEnabled();

        try {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("secLight",
                    CameraConfig.isSecLightEnabled()
                            && Utils.isTimeBetweenTwoTimes(
                                    CameraConfig.getSecLightStartTime(),
                            CameraConfig.getSecLightEndTime(),
                            new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())
                    )
            );

            jsonObject.put("secLOn", CameraConfig.isSecLightTurnedOn());

            return jsonObject.toString();
        } catch (ParseException | JSONException e) {
            e.printStackTrace();
        }

        return "{}";
    }

    public void onMotionDetected(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message", "Motion detected");

            MqttService.getInstance().publish(jsonObject.toString(), MotionTopic.class);

            //CameraCaptureService.getInstance().capture();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void executeCommand(CameraCommand cameraCommand){
        Log.i(TAG, "Executing camera command");
        switch (cameraCommand){
            case CAPTURE:
                CameraCaptureService.getInstance().captureSingle();
                break;
        }
    }

    public String getCameraIp() {
        return cameraIp;
    }

    public String getCaptureUrl() throws CameraNotReadyException{
        if(cameraIp == null){
            throw new CameraNotReadyException("Camera IP not found");
        }

        return String.format("http://%s/capture", this.cameraIp);
    }

    public void setCameraIp(String cameraIp) {
        this.cameraIp = cameraIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getStreamPort() {
        return streamPort;
    }

    public void setStreamPort(int streamPort) {
        this.streamPort = streamPort;
    }
}
