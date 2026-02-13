package com.kshem.homesecurity.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.kshem.homesecurity.R;
import com.kshem.homesecurity.camera.CameraConfig;
import com.kshem.homesecurity.camera.CameraHandlerService;
import com.kshem.homesecurity.controller.ControllerCommand;
import com.kshem.homesecurity.controller.ControllerHandlerService;
import com.kshem.homesecurity.controller.ControllerRequestHandler;
import com.kshem.homesecurity.exceptions.CameraNotReadyException;
import com.kshem.homesecurity.server.NanoServer;
import com.kshem.homesecurity.services.MainControlService;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Button btnUnlockDoor, btnLockDoor, btnPowerDC, btnPowerOffDC, btnAlarmOn,
            btnAlarmOff, btnReset, btnCapture, btnEnableAlarm, btnDisableAlarm, btnResetCamera,
            btnTurnOffCamera, btnToggleSecLight, btnToggleSecLightState;

    private ImageView captureView;

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
        btnCapture = findViewById(R.id.capture);
        btnEnableAlarm = findViewById(R.id.enableAlarm);
        btnDisableAlarm = findViewById(R.id.disableAlarm);
        btnResetCamera = findViewById(R.id.btnResetCamera);
        btnTurnOffCamera = findViewById(R.id.btnTurnOffCamera);
        btnToggleSecLight = findViewById(R.id.toggleSecLight);
        btnToggleSecLightState = findViewById(R.id.toggleSecLightState);

        captureView = findViewById(R.id.imgCapture);

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
        btnCapture.setOnClickListener(view-> captureImage());
        btnToggleSecLight.setOnClickListener(view -> {
            CameraConfig.setSecLightEnabled(!CameraConfig.isSecLightEnabled());

            updateUI();
        });

        btnToggleSecLightState.setOnClickListener(view -> {
            CameraConfig.setSecLightTurnedOn(!CameraConfig.isSecLightTurnedOn());
            updateUI();
        });

    }



    private void sendCommand(ControllerCommand command){
        if(ControllerHandlerService.getInstance().sendCommand(command)){
            Toast.makeText(MainActivity.this, "Sent command", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_LONG).show();
        }
    }

    private void updateUI(){
        if(CameraConfig.isSecLightEnabled()){
            btnToggleSecLight.setText("Disable SEC Light");
        }else{
            btnToggleSecLight.setText("Enable SEC Light");
        }

        if(CameraConfig.isSecLightTurnedOn()){
            btnToggleSecLightState.setText("Turn Off Security Light");
        }else{
            btnToggleSecLightState.setText("Turn On Security Light");
        }
    }

    private void captureImage(){
        try{
            Picasso.get()
                    .load(CameraHandlerService.getInstance().getCaptureUrl())
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .into(captureView);
        }catch (CameraNotReadyException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}