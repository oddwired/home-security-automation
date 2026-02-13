package com.kshem.homeclient.models;

import android.support.annotation.Nullable;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class TriggeredAlarm {
    @Id
    private long id;
    private String date;

    public TriggeredAlarm() {
    }

    public TriggeredAlarm(long id, String date) {
        this.id = id;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof TriggeredAlarm){
            TriggeredAlarm triggeredAlarm = (TriggeredAlarm) obj;
            return triggeredAlarm.getId() == this.id;
        }

        return false;
    }
}
