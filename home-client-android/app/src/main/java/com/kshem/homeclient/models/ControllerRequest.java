package com.kshem.homeclient.models;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class ControllerRequest {
    @Id
    public long id;

    private Date date;
    private String data;
    private Date synchronizedAt;

    public ControllerRequest(){}

    public ControllerRequest(Date date, String data, Date synchronizedAt){
        this.date = date;
        this.data = data;
        this.synchronizedAt = synchronizedAt;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Date getSynchronizedAt() {
        return synchronizedAt;
    }

    public void setSynchronizedAt(Date synchronizedAt) {
        this.synchronizedAt = synchronizedAt;
    }
}
