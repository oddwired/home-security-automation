package com.kshem.homeclient.mqtt;

import android.content.Intent;
import android.util.Log;

import com.kshem.homeclient.beans.ObjectBox;
import com.kshem.homeclient.models.TriggeredAlarm;
import com.kshem.homeclient.ui.MainActivity;
import com.kshem.homeclient.utils.LocalPropertyHelper;
import com.kshem.homeclient.utils.Utils;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.objectbox.Box;

public class DataTopic extends MqttTopic{

    private static final String TAG = DataTopic.class.getSimpleName();

    @Override
    public String getTopic() {
        return LocalPropertyHelper.getMqttControllerDataTopic();
    }

    @Override
    public void onMessage(String message) {
        Log.i(TAG, "Received data message: "+ message);

        try {
            JSONObject jsonData = new JSONObject(message);
            String data = jsonData.getString("data");
            String[] items = data.split(",");
            if(items.length >= 6){
                LocalPropertyHelper.setLastKnownDoorLockStatus(items[0].equals("0"));
                LocalPropertyHelper.setLastKnownLockPadStatus(items[1].equals("0"));
                LocalPropertyHelper.setLastKnownDcPowerStatus(items[2].equals("1"));
                LocalPropertyHelper.setLastKnownAlarmStatus(items[3].equals("1"));
                LocalPropertyHelper.setLastKnownDoorLightStatus(items[4].equals("1"));
                LocalPropertyHelper.setLastKnownAlarmArmStatus(items[5].equals("1"));
                LocalPropertyHelper.setLastUpdate(new Date());

                if(LocalPropertyHelper.getLastKnownAlarmStatus()){
                    Utils.createNotification(getContext(),
                            new Intent(getContext(), MainActivity.class),
                            "Alarm triggered", "Alarm",true);

                    Box<TriggeredAlarm> triggeredAlarmBox = ObjectBox.get()
                            .boxFor(TriggeredAlarm.class);

                    TriggeredAlarm triggeredAlarm = new TriggeredAlarm();

                    SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String formattedDate = sm.format(new Date());

                    triggeredAlarm.setDate(formattedDate);

                    triggeredAlarmBox.put(triggeredAlarm);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(TAG, "Error decoding data");
        }

        super.onMessage(message);
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
