package com.kshem.homeclient.models;

import org.json.JSONException;
import org.json.JSONObject;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class Sms {
    @Id
    private long id;

    private String phone;
    private String message;

    public Sms() {
    }

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

    public static Sms parseJson(String json) throws JSONException{
        JSONObject jsonObject = new JSONObject(json);
        String phone = jsonObject.getString("phone");
        String message = jsonObject.getString("message");

        return new Sms(phone, message);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
