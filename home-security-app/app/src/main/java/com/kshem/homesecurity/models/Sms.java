package com.kshem.homesecurity.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Sms {
    private String phone;
    private String message;

    public Sms(String phone, String message) {
        this.phone = phone;
        this.message = message;
    }

    public String toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("phone", this.phone);
        jsonObject.put("message", this.message);

        return jsonObject.toString();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
