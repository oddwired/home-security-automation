package com.kshem.homeclient.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.kshem.homeclient.R;
import com.kshem.homeclient.beans.ObjectBox;
import com.kshem.homeclient.models.TriggeredAlarm;
import com.kshem.homeclient.services.AsyncService;

import java.util.List;

import io.objectbox.Box;

public class AlarmsView extends AppCompatActivity {

    private ListView listView;
    private AlarmsAdapter alarmsAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms_view);

        listView = findViewById(R.id.alarmsList);

        alarmsAdapter = new AlarmsAdapter(this);

        listView.setAdapter(alarmsAdapter);

        AsyncService.execute(()->{
            Box<TriggeredAlarm> triggeredAlarmBox = ObjectBox.get().boxFor(TriggeredAlarm.class);

            List<TriggeredAlarm> triggeredAlarms = triggeredAlarmBox.getAll();

            alarmsAdapter.addItems(triggeredAlarms);
        });
    }
}