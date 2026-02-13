package com.kshem.homeclient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CctvCapture {
    private String name;
    private String fileId;

    public CctvCapture() {
    }

    public CctvCapture(String name, String fileId) {
        this.name = name;
        this.fileId = fileId;
    }

    public static CctvCapture parseJson(JSONObject cctvCaptureObject) {
        try{
            String name = cctvCaptureObject.getString("name");
            String fileId = cctvCaptureObject.getString("fileId");

            return new CctvCapture(name, fileId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
