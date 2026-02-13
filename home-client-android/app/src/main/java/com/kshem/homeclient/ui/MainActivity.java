package com.kshem.homeclient.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kshem.homeclient.R;
import com.kshem.homeclient.camera.CameraCommand;
import com.kshem.homeclient.controller.ControllerCommand;
import com.kshem.homeclient.mqtt.CommandsTopic;
import com.kshem.homeclient.mqtt.DataTopic;
import com.kshem.homeclient.mqtt.DeviceInfoTopic;
import com.kshem.homeclient.mqtt.MqttService;
import com.kshem.homeclient.services.AsyncService;
import com.kshem.homeclient.services.MainControlService;
import com.kshem.homeclient.utils.LocalPropertyHelper;

import org.json.JSONObject;

import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Button btnUnlockDoor, btnLockDoor, btnPowerDC, btnPowerOffDC, btnAlarmOn,
            btnAlarmOff, btnReset, btnCctvView, btnEnableAlarm, btnDisableAlarm, btnResetCamera,
            btnTurnOffCamera, btnCapture, btnViewMessages;

    private TextView vDoorStatus, vDoorLockStatus, vDcPowerStatus, vAlarmStatus, vLockPadStatus,
            vLastUpdate, vAlarmEnabled, vBatteryLevel;

    private LinearLayout alarmStatusView;

    @Override
    protected void onStart() {
        super.onStart();

        MainControlService.startActionMainControlService(this);

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUnlockDoor = findViewById(R.id.testCommand);
        btnLockDoor = findViewById(R.id.lockDoor);
        btnPowerDC = findViewById(R.id.powerDC);
        btnPowerOffDC = findViewById(R.id.powerOffDC);
        btnAlarmOn = findViewById(R.id.alarmOn);
        btnAlarmOff = findViewById(R.id.alarmOff);
        btnReset = findViewById(R.id.reset);
        btnCctvView = findViewById(R.id.btnStartCctvView);
        btnEnableAlarm = findViewById(R.id.enableAlarm);
        btnDisableAlarm = findViewById(R.id.disableAlarm);
        btnResetCamera = findViewById(R.id.btnResetCamera);
        btnCapture = findViewById(R.id.btnCapture);
        btnTurnOffCamera = findViewById(R.id.btnTurnOffCamera);
        btnViewMessages = findViewById(R.id.btnViewMessages);
        alarmStatusView = findViewById(R.id.alarmStatusView);

        vLastUpdate = findViewById(R.id.lastUpdate);
        vDoorStatus = findViewById(R.id.doorStatus);
        vDoorLockStatus = findViewById(R.id.doorLockStatus);
        vDcPowerStatus = findViewById(R.id.dcPowerStatus);
        vAlarmStatus = findViewById(R.id.alarmStatus);
        vLockPadStatus = findViewById(R.id.lockPadStatus);
        vAlarmEnabled = findViewById(R.id.alarmEnabled);
        vBatteryLevel = findViewById(R.id.batteryLevel);

        btnUnlockDoor.setOnClickListener(view -> sendCommand(ControllerCommand.UNLOCK_DOOR));
        btnLockDoor.setOnClickListener(view -> sendCommand(ControllerCommand.LOCK_DOOR));
        btnPowerDC.setOnClickListener(view-> sendCommand(ControllerCommand.TURN_ON_DC));
        btnPowerOffDC.setOnClickListener(view-> sendCommand(ControllerCommand.TURN_OFF_DC));
        btnAlarmOn.setOnClickListener(view-> sendCommand(ControllerCommand.TURN_ON_ALARM));
        btnAlarmOff.setOnClickListener(view-> sendCommand(ControllerCommand.TURN_OFF_ALARM));
        btnReset.setOnClickListener(view-> sendCommand(ControllerCommand.RESET_CONTROLLER));
        btnEnableAlarm.setOnClickListener(view -> sendCommand(ControllerCommand.ENABLE_ALARM));
        btnDisableAlarm.setOnClickListener(view -> sendCommand(ControllerCommand.DISABLE_ALARM));
        btnResetCamera.setOnClickListener(view -> sendCommand(ControllerCommand.RESET_CAMERA));
        btnTurnOffCamera.setOnClickListener(view -> sendCommand(ControllerCommand.TURN_OFF_CAMERA));
        btnCapture.setOnClickListener(view -> sendCommand(CameraCommand.CAPTURE));
        btnCctvView.setOnClickListener(view -> startActivity(new Intent(MainActivity.this,
                CameraCaptureView.class)));

        btnViewMessages.setOnClickListener(view -> startActivity(new Intent(MainActivity.this,
                SmsActivity.class)));

        alarmStatusView.setOnClickListener(view -> startActivity(new Intent(MainActivity.this,
                AlarmsView.class)));

        refreshStatus();

        AsyncService.execute(()->{
            while (!MqttService.isInitialized()){
                // Wait for it to be init
            }

            DataTopic dataTopic = null;
            DeviceInfoTopic deviceInfoTopic = null;

            MqttService mqttService = MqttService.getInstance();

            // Wait for datatopic to be init
            while(dataTopic == null || deviceInfoTopic == null){
                dataTopic = (DataTopic) mqttService
                        .getRegisteredTopic(LocalPropertyHelper.getMqttControllerDataTopic());

                deviceInfoTopic = (DeviceInfoTopic) mqttService.getRegisteredTopic(LocalPropertyHelper.getMqttDeviceStatusTopic());
            }

            dataTopic.setCallback(message -> refreshStatus());
            deviceInfoTopic.setCallback(message -> refreshStatus());
        });
    }

    private void refreshStatus(){

        runOnUiThread(() -> {
            String lastUpdate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                    .format(LocalPropertyHelper.getLastUpdate());
            vLastUpdate.setText(lastUpdate);
            vDoorLockStatus.setText(LocalPropertyHelper.getLastKnownDoorLockStatus() ? "LOCKED": "UNLOCKED");
            vDcPowerStatus.setText(LocalPropertyHelper.getLastKnownDcPowerStatus()? "ON" : "OFF");
            vAlarmStatus.setText(LocalPropertyHelper.getLastKnownAlarmStatus() ? "ON" : "OFF");
            vLockPadStatus.setText(LocalPropertyHelper.getLastKnownDcLocKPadStatus() ? "CLOSED" : "OPEN");
            vAlarmEnabled.setText(LocalPropertyHelper.getLastKnownAlarmArmStatus() ? "True": "False");
            vBatteryLevel.setText(String.valueOf(LocalPropertyHelper.getLastKnownBatteryLevel()));
        });

    }

    private void sendCommand(CameraCommand cameraCommand){
        try {
            MqttService mqttService = MqttService.getInstance();
            CommandsTopic commandsTopic = (CommandsTopic) mqttService
                    .getRegisteredTopic(LocalPropertyHelper.getMqttCommandTopic());

            if(commandsTopic != null){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "cam");
                jsonObject.put("command", cameraCommand.getCommand());

                commandsTopic.publish(jsonObject.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendCommand(ControllerCommand command){
        try {
            MqttService mqttService = MqttService.getInstance();
            CommandsTopic commandsTopic = (CommandsTopic) mqttService
                    .getRegisteredTopic(LocalPropertyHelper.getMqttCommandTopic());

            if(commandsTopic != null){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "con");
                jsonObject.put("command", command.getCommand());

                commandsTopic.publish(jsonObject.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}