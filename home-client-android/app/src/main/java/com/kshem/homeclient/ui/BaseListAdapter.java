package com.kshem.homeclient.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseListAdapter<T> extends BaseAdapter {

    protected Activity activity;
    protected List<T> tList;
    public BaseListAdapter(Activity activity){
        this.activity = activity;
        this.tList = new ArrayList<>();
    }

    public void addItem(T t){
        this.tList.add(t);

        this.activity.runOnUiThread(this::notifyDataSetChanged);
    }

    public void addItems(List<T> ts){
        this.tList.addAll(ts);

        this.activity.runOnUiThread(this::notifyDataSetChanged);
    }

    public List<T> getItems(){
        return this.tList;
    }

    public void remove(T t){
        tList.remove(t);
        this.activity.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public int getCount() {
        return this.tList.size();
    }

    @Override
    public T getItem(int position) {
        return this.tList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(this.activity).inflate(getLayout(), parent, false);
        }

        buildItemView(convertView, getItem(position));

        return convertView;
    }

    protected abstract int getLayout();
    protected abstract void buildItemView(View convertView, T t);
}
