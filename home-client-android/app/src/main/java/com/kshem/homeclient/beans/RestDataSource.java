package com.kshem.homeclient.beans;

import static com.kshem.homeclient.utils.Constants.BASE_SERVER_URL;

import android.util.Log;

import com.kshem.homeclient.CctvCapture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RestDataSource {
    private static final String TAG = RestDataSource.class.getSimpleName();

    public interface RestCallback<T>{
        void onData(T data);
        void onError(String error);
    }

    public static void getCctvCaptures(int page, int limit, RestCallback<List<CctvCapture>> callback) throws IOException {
        String urlParams = String.format("limit=%s&offset=%s", limit, page);
        String url = BASE_SERVER_URL + "?" + urlParams;

        NetworkManager.getInstance().get(url, null, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "Error fetching captures");
                callback.onError("Error fetching captures");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    Log.i(TAG, "Fetching patients success");
                    try{
                        JSONObject data = new JSONObject(response.body().string());

                        JSONArray captures = data.getJSONArray("content");

                        List<CctvCapture> cctvCaptures = new ArrayList<>();

                        for(int i = 0; i < captures.length(); i++){
                            JSONObject cctvObject = captures.getJSONObject(i);

                            if(cctvObject!=null){
                                cctvCaptures.add(CctvCapture.parseJson(cctvObject));
                            }

                        }

                        callback.onData(cctvCaptures);
                    }catch (JSONException e){
                        callback.onError("JSON Parse error");
                        e.printStackTrace();
                    }

                }
            }
        }, false);
    }
}
