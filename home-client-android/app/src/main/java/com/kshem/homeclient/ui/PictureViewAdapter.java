package com.kshem.homeclient.ui;

import static com.kshem.homeclient.utils.Constants.BASE_SERVER_URL;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kshem.homeclient.CctvCapture;
import com.kshem.homeclient.R;
import com.squareup.picasso.Picasso;

public class PictureViewAdapter extends BaseListAdapter<CctvCapture>{
    public PictureViewAdapter(Activity activity) {
        super(activity);
    }

    @Override
    protected int getLayout() {
        return R.layout.capture_item;
    }

    @Override
    protected void buildItemView(View convertView, CctvCapture cctvCapture) {
        TextView textView = convertView.findViewById(R.id.captureDate);
        ImageView imageView = convertView.findViewById(R.id.captureView);

        textView.setText(cctvCapture.getName());
        Picasso.get()
                .load(BASE_SERVER_URL + "/" + cctvCapture.getFileId())
                .into(imageView);

    }
}
