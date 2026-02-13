package com.kshem.homesecurity.services;

import android.app.PendingIntent;
import android.telephony.SmsManager;

import com.kshem.homesecurity.controller.ControllerCommand;
import com.kshem.homesecurity.controller.ControllerHandlerService;
import com.kshem.homesecurity.models.Sms;
import com.kshem.homesecurity.mqtt.MqttService;
import com.kshem.homesecurity.mqtt.MqttTopic;
import com.kshem.homesecurity.mqtt.SmsTopic;

import org.json.JSONException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsHandlerService {
    private static final String TAG = SmsHandlerService.class.getSimpleName();
    private static volatile SmsHandlerService instance;

    private SmsHandlerService(){}

    public static SmsHandlerService getInstance(){
        if(instance == null){
            synchronized (SmsHandlerService.class){
                if(instance == null){
                    instance = new SmsHandlerService();
                }
            }
        }

        return instance;
    }

    private String extractCommand(String commandString){
        Pattern pattern = Pattern.compile("[^:]*$");
        Matcher matcher = pattern.matcher(commandString);

        if(matcher.find()){
            return matcher.group();
        }

        return null;
    }

    public void onReceiveMessage(Sms sms){
        if(!sms.getMessage().contains("cmd:")){
            try {
                MqttService.getInstance().publish(sms.toJson(), SmsTopic.class);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

        returnOKResponse();
        if(!sms.getPhone().contains("<Your number here>") && !sms.getMessage().toLowerCase().contains("emergency")){
            return;
        }

        String command = extractCommand(sms.getMessage());
        if(command != null){
            ControllerHandlerService.getInstance()
                    .sendCommand(ControllerCommand.findByValue(command));
        }
    }

    public void returnOKResponse(){
        AsyncService.execute(()->{
            sendSms("OK");
        });
    }

    public void sendSms(String message){
        String scAddress = null;
        // Set pending intents to broadcast
        // when message sent and when delivered, or set to null.
        String destinationAddress = "<Your number here or be smarter than be and make it configurable>";
        PendingIntent sentIntent = null, deliveryIntent = null;
        // Use SmsManager.
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage
                (destinationAddress, scAddress, message,
                        sentIntent, deliveryIntent);
    }
}
