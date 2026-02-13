package com.kshem.homeclient.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.kshem.homeclient.R;
import com.kshem.homeclient.mqtt.CommandsTopic;
import com.kshem.homeclient.mqtt.ConfigTopic;
import com.kshem.homeclient.mqtt.DataTopic;
import com.kshem.homeclient.mqtt.DeviceInfoTopic;
import com.kshem.homeclient.mqtt.MotionTopic;
import com.kshem.homeclient.mqtt.MqttService;
import com.kshem.homeclient.mqtt.SmsTopic;
import com.kshem.homeclient.mqtt.SubscriptionStatus;
import com.kshem.homeclient.ui.MainActivity;

import java.io.IOException;

public class MainControlService extends Service {

    private static final String TAG = MainControlService.class.getSimpleName();

    public static final String ACTION_START_CONTROL_SERVICE =
            "com.kshem.homeclient.services.action.START_CONTROL_SERVICE";
    public static final String ACTION_STOP_CONTROL_SERVICE =
            "com.kshem.homeclient.services.action.STOP_CONTROL_SERVICE";

    private static final int NOTIFICATION_ID = 100;

    private boolean serviceRunning = false;
    private PowerManager.WakeLock wakeLock;
    private Handler handler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startActionMainControlService(Context context) {
        Intent intent = new Intent(context, MainControlService.class);
        intent.setAction(ACTION_START_CONTROL_SERVICE);
        try {
            context.startService(intent);
        }catch (IllegalStateException e){

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        handler = new Handler();
        if(intent != null){
            switch (intent.getAction()){
                case ACTION_START_CONTROL_SERVICE:
                    startService();
                    break;
                case ACTION_STOP_CONTROL_SERVICE:
                    stopService();
                    break;
            }
        }else{
            startService();
        }

        return START_STICKY;
    }

    private void startService(){
        if(serviceRunning){
            return;
        }

        serviceRunning = true;

        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MainControlService::WakelockTag");
        wakeLock.acquire();

        CommandsTopic commandsTopic = new CommandsTopic();
        commandsTopic.setSubscriptionStatus(SubscriptionStatus.PUBLISH_ONLY);
        ConfigTopic configTopic = new ConfigTopic();
        configTopic.setSubscriptionStatus(SubscriptionStatus.PUBLISH_ONLY);

        MqttService.init(this, commandsTopic, new DataTopic(), configTopic,
                new MotionTopic(), new DeviceInfoTopic(), new SmsTopic());

    }

    private void stopService(){}

    @Override
    public void onCreate() {
        super.onCreate();

        createNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        MqttService.getInstance().disconnect(null);
    }

    private void createNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("my_service", "Home Security client");
        } else {

            // Create notification default intent.
            Intent intent = new Intent();
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            // Create notification builder.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            // Make notification show big text.
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle("SmartHealth is monitoring your Blood Pressure");
            //bigTextStyle.bigText("Android foreground service is a android service which can run in foreground always, it can be controlled by user via notification.");
            // Set big text style.
            builder.setStyle(bigTextStyle);

            builder.setWhen(System.currentTimeMillis());
            builder.setSmallIcon(R.mipmap.ic_launcher);
            Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            builder.setLargeIcon(largeIconBitmap);
            // Make the notification max priority.
            builder.setPriority(Notification.PRIORITY_HIGH);
            // Make head-up notification.
            builder.setFullScreenIntent(pendingIntent, true);

            // Build the notification.
            Notification notification = builder.build();

            // Start foreground service.
            startForeground(1, notification);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName) {
        Intent resultIntent = new Intent(this, MainActivity.class);
// Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Monitoring Home Security")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(resultPendingIntent) //intent
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, notificationBuilder.build());
        startForeground(1, notification);
    }
}
