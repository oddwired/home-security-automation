package com.kshem.homeclient.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.kshem.homeclient.R;
import com.kshem.homeclient.beans.ObjectBox;
import com.kshem.homeclient.models.TriggeredAlarm;
import com.kshem.homeclient.services.AsyncService;

import io.objectbox.Box;

public class AlarmsAdapter extends BaseListAdapter<TriggeredAlarm>{
    public AlarmsAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected int getLayout() {
        return R.layout.triggered_alarm_layout;
    }

    @Override
    protected void buildItemView(View convertView, TriggeredAlarm triggeredAlarm) {
        TextView textView = convertView.findViewById(R.id.triggeredAlarm);
        ImageButton btnDelete = convertView.findViewById(R.id.deleteEntry);

        textView.setText(triggeredAlarm.getDate());

        btnDelete.setOnClickListener(view-> {
            remove(triggeredAlarm);
            AsyncService.execute(()->{
                Box<TriggeredAlarm> triggeredAlarmBox = ObjectBox.get().boxFor(TriggeredAlarm.class);
                triggeredAlarmBox.remove(triggeredAlarm.getId());
            });


        });
    }
}
