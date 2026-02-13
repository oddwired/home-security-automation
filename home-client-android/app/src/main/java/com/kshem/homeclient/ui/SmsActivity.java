package com.kshem.homeclient.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;

import com.kshem.homeclient.R;
import com.kshem.homeclient.beans.ObjectBox;
import com.kshem.homeclient.models.Sms;
import com.kshem.homeclient.models.Sms_;
import com.kshem.homeclient.models.TriggeredAlarm;
import com.kshem.homeclient.services.AsyncService;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.QueryBuilder;

public class SmsActivity extends AppCompatActivity {

    private ListView listView;
    private SmsAdapter smsAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        listView = findViewById(R.id.list);

        smsAdapter = new SmsAdapter(this);

        listView.setAdapter(smsAdapter);

        AsyncService.execute(()->{
            Box<Sms> triggeredAlarmBox = ObjectBox.get().boxFor(Sms.class);

            List<Sms> triggeredAlarms = triggeredAlarmBox.query()
                    .order(Sms_.id, QueryBuilder.DESCENDING)
                    .build().find();

            smsAdapter.addItems(triggeredAlarms);
        });
    }
}