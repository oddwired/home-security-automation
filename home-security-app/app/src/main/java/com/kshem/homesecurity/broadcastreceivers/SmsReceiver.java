package com.kshem.homesecurity.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.kshem.homesecurity.models.Sms;
import com.kshem.homesecurity.services.SmsHandlerService;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = SmsReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received SMS");

        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String strMessage = "";
        String format = bundle.getString("format");

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus != null) {
            // Fill the msgs array.
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                // Check Android version and use appropriate createFromPdu.
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                // Build the message to show.
                String phone = msgs[i].getOriginatingAddress();
                String message = msgs[i].getMessageBody();
                // Log and display the SMS message.
                Log.i(TAG, "onReceive: " + message);


                SmsHandlerService.getInstance().onReceiveMessage(new Sms(phone, message));
                if(phone.contains("716383258")){ // The only number that can send commands

                }
            }
        }
    }
}
