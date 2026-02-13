package com.kshem.homeclient;

import android.app.Application;

import com.kshem.homeclient.beans.ObjectBox;

public class HomeClientApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        ObjectBox.init(this);
    }
}
