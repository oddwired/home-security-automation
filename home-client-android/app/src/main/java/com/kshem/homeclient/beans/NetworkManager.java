package com.kshem.homeclient.beans;

import android.support.annotation.Nullable;

import com.kshem.homeclient.utils.Constants;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkManager {

    private static final String TAG = NetworkManager.class.getSimpleName();

    public interface NetworkChangeListener{
        void onChange(boolean isConnected);
    }

    private volatile static NetworkManager instance;
    private Boolean isConnected = true;

    private static final MediaType JSONMedia = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/*");

    private OkHttpClient client;

    private ArrayList<NetworkChangeListener> listeners;

    public static NetworkManager getInstance() {

        if(instance == null){
            synchronized (NetworkManager.class){
                if(instance == null){
                    instance = new NetworkManager();
                }
            }
        }

        return instance;
    }

    private NetworkManager() {
        this.client = new OkHttpClient();
        this.listeners = new ArrayList<>();
    }

    public void setListener(NetworkChangeListener listener){
        listeners.add(listener);
    }

    public Boolean isConnected() {
        return isConnected;
    }

    public void setConnected(Boolean connected) {
        isConnected = connected;

        for(NetworkChangeListener listener : listeners){
            try{
                listener.onChange(this.isConnected);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public Response login(String username, String password) throws IOException {
        System.out.println(TAG + " Login called");

        RequestBody requestBody = new MultipartBody.Builder()
                //.type(MultipartBuilder.FORM)
                .addFormDataPart("grant_type", "password")
                .addFormDataPart("username", username)
                .addFormDataPart("password", password)
                .build();

        Request request = new Request.Builder()
                .url(Constants.BASE_SERVER_URL + "/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic Y2xpZW50OnNlY3JldA==")
                .post(requestBody)
                .build();

        Call call = this.client.newCall(request);

        return  call.execute();
    }

    public Response post(String url, @Nullable Headers.Builder headers, JSONObject data,
                         @Nullable Callback callback, Boolean requireAuth) throws IOException {
        RequestBody requestBody = RequestBody.create(JSONMedia, data.toString());
        Request.Builder requestBuilder = new Request.Builder();

        if(headers == null)
            headers = new Headers.Builder();


        requestBuilder.url(url)
                .headers(headers.build())
                .post(requestBody);

        Request request = requestBuilder.build();

        Call call = this.client.newCall(request);

        if(callback == null){
            return call.execute();
        }

        call.enqueue(callback);

        return null;
    }

    public Response put(String url, @Nullable Headers.Builder headers, JSONObject data,
                        @Nullable Callback callback, Boolean requireAuth) throws IOException {
        RequestBody requestBody = RequestBody.create(JSONMedia, data.toString());
        Request.Builder requestBuilder = new Request.Builder();

        if(headers == null)
            headers = new Headers.Builder();


        headers.add("Content-Type", "application/json");
        requestBuilder.url(url)
                .headers(headers.build())
                .put(requestBody);

        Request request = requestBuilder.build();

        Call call = this.client.newCall(request);

        if(callback == null){
            return call.execute();
        }

        call.enqueue(callback);

        return null;
    }

    public Response get(String url, @Nullable Headers.Builder headers, @Nullable Callback callback,
                        Boolean requireAuth)
            throws IOException {

        if(headers == null)
            headers = new Headers.Builder();


        Request request = new Request.Builder()
                .url(url)
                .headers(headers.build())
                .get()
                .build();

        Call call = this.client.newCall(request);

        if(callback == null){
            return call.execute();
        }else{
            call.enqueue(callback);
        }

        return null;
    }
}
