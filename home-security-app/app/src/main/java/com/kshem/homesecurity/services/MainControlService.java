package com.kshem.homesecurity.services;

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

import com.kshem.homesecurity.R;
import com.kshem.homesecurity.camera.CameraRequestHandler;
import com.kshem.homesecurity.controller.ControllerRequestHandler;
import com.kshem.homesecurity.mqtt.CommandsTopic;
import com.kshem.homesecurity.mqtt.ConfigTopic;
import com.kshem.homesecurity.mqtt.DataTopic;
import com.kshem.homesecurity.mqtt.DeviceInfoTopic;
import com.kshem.homesecurity.mqtt.MotionTopic;
import com.kshem.homesecurity.mqtt.MqttService;
import com.kshem.homesecurity.mqtt.SubscriptionStatus;
import com.kshem.homesecurity.server.NanoServer;
import com.kshem.homesecurity.ui.MainActivity;
import com.kshem.homesecurity.utils.LocalPropertyHelper;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;

public class MainControlService extends Service {

    private static final String TAG = MainControlService.class.getSimpleName();

    public static final String ACTION_START_CONTROL_SERVICE =
            "com.kshem.homesecurity.services.action.START_CONTROL_SERVICE";
    public static final String ACTION_STOP_CONTROL_SERVICE =
            "com.kshem.homesecurity.services.action.STOP_CONTROL_SERVICE";

    private static final int NOTIFICATION_ID = 100;

    private boolean serviceRunning = false;
    private PowerManager.WakeLock wakeLock;
    private Handler handler;

    private NanoServer nanoServer;

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

        this.nanoServer = new NanoServer(event -> Log.i(TAG, "Nano Server Event: "+ event),
                new ControllerRequestHandler(), new CameraRequestHandler());

        try{
            this.nanoServer.start();
            Log.i(TAG, "Server started");
        }catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "The server could not start.");
        }

        MqttService.init(this,
                new CommandsTopic(),
                new DataTopic(),
                new ConfigTopic(),
                new DeviceInfoTopic(),
                new MotionTopic());


        new DeviceHandlerService().runStatusUpdate(getApplicationContext());
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

        if(nanoServer != null){
            nanoServer.stop();
        }

        MqttService.getInstance().disconnect(null);
        MqttService.getInstance().getMqttClient().close();
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
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

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
