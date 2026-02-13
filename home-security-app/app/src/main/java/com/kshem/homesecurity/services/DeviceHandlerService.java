package com.kshem.homesecurity.services;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Handler;

import com.kshem.homesecurity.mqtt.DeviceInfoTopic;
import com.kshem.homesecurity.mqtt.MqttService;
import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceHandlerService {

    private Handler handler;
    public DeviceHandlerService(){
        this.handler = new Handler();
    }

    public void runStatusUpdate(Context context){
        AsyncService.execute(()-> {
            try {
                BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

                if(batteryManager != null){
                    int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    boolean isCharging = batteryManager.isCharging();
                    int isPluggedAC = batteryManager.getIntProperty(BatteryManager.BATTERY_PLUGGED_AC);

                    JSONObject jsonObject = new JSONObject();

                    jsonObject.put("batteryLevel", batteryLevel);
                    jsonObject.put("isCharging", isCharging);

                    publishData(jsonObject.toString());
                }
            }catch (JSONException e){
                e.printStackTrace();
            }finally {
                handler.postDelayed(()->runStatusUpdate(context), 60 * 10 * 1000);
            }

        });
    }

    private void publishData(String data){
        MqttService mqttService = MqttService.getInstance();
        // Wait for service to get ready
        while(!mqttService.isConnected()){}

        mqttService.publish(data, DeviceInfoTopic.class);

    }
}
