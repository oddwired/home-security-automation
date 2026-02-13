package com.kshem.homesecurity.models;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class CameraCapture {
    @Id
    public long id;

    private Date date;
    private String filePath;

    public CameraCapture(){}

    public CameraCapture(Date date, String filePath){
        this.date = date;
        this.filePath = filePath;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
