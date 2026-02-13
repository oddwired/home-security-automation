package com.kshem.homeclient.ui;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.kshem.homeclient.R;

public class NotificationActivity extends AppCompatActivity {

    private Uri notification;
    private Ringtone r;
    private ImageButton btnCancel;

    private AudioManager am;
    private int originalVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window win= getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_notification);

        btnCancel = findViewById(R.id.cancelAlarm);

        btnCancel.setOnClickListener(view -> {
            stopSound();

            finish();
        });

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        originalVolume = am.getStreamVolume(AudioManager.STREAM_RING);

        notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        r = RingtoneManager.getRingtone(NotificationActivity.this, notification);
        playNotifyingSound();


    }

    private void playNotifyingSound(){
        try {

            am.setStreamVolume(
                    AudioManager.STREAM_RING,
                    am.getStreamMaxVolume(AudioManager.STREAM_RING),
                    0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                r.setLooping(false);
            }
            r.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopSound(){
        try {
            r.stop();

            am.setStreamVolume(
                    AudioManager.STREAM_RING,
                    originalVolume,
                    0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}