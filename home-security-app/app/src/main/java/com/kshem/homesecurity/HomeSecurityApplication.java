package com.kshem.homesecurity;

import android.app.Application;

import com.kshem.homesecurity.beans.ObjectBox;

public class HomeSecurityApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        ObjectBox.init(this);
    }
}
