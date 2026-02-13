package com.kshem.homeclient.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.kshem.homeclient.R;
import com.kshem.homeclient.models.Sms;

public class SmsAdapter extends BaseListAdapter<Sms>{
    public SmsAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected int getLayout() {
        return R.layout.sms_layout;
    }

    @Override
    protected void buildItemView(View convertView, Sms sms) {
        TextView vPhone = convertView.findViewById(R.id.vPhone);
        TextView vMessage = convertView.findViewById(R.id.vMessage);

        vPhone.setText(sms.getPhone());
        vMessage.setText(sms.getMessage());
    }
}
