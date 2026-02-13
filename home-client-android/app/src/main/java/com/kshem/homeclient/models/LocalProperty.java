package com.kshem.homeclient.models;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class LocalProperty {
    @Id
    public long id;

    private String key;
    private String value;
    private int valueType;

    public LocalProperty() {
    }

    public LocalProperty(String key, String value, int valueType) {
        this.key = key;
        this.value = value;
        this.valueType = valueType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getValueType() {
        return valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }
}
