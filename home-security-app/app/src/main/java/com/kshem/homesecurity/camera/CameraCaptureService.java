package com.kshem.homesecurity.camera;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.kshem.homesecurity.beans.ObjectBox;
import com.kshem.homesecurity.models.CameraCapture;
import com.kshem.homesecurity.services.AsyncService;
import com.kshem.homesecurity.utils.NetworkManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.objectbox.Box;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class CameraCaptureService {
    private static final String TAG = CameraCaptureService.class.getSimpleName();

    private static volatile CameraCaptureService instance;

    private boolean capturing = false;
    private int captureCount = 3;
    private Handler handler;
    private final String storagePath;

    private CameraCaptureService(){
        this.handler = new Handler();
        File file = new File(Environment.getExternalStorageDirectory(), "cctv_capture");

        this.storagePath = file.getPath();

        Log.i(TAG, "Files directory "+this.storagePath);
    }

    public static CameraCaptureService getInstance(){
        if(instance == null){
            synchronized (CameraCaptureService.class){
                if(instance == null){
                    instance = new CameraCaptureService();
                }
            }
        }

        return instance;
    }

    public synchronized void captureSingle(){
        if(!capturing){
            captureCount = 1;
            capture();
        }
    }

    public synchronized void capture(){
        if(capturing && captureCount < 10){
            captureCount += captureCount; // Increase the number if captures
        }else{
            capturing = true;
            doCapture();
        }
    }

    private void doCapture(){
        AsyncService.execute(()->{
            int count = 0;
            while (count < captureCount){
                try {
                    NetworkManager networkManager = NetworkManager.getInstance();
                    String url = CameraHandlerService.getInstance().getCaptureUrl();
                    Response response = networkManager.get(url, null, null, false);

                    if(response.isSuccessful()){
                        saveImage(response);
                    }else{
                        Log.i(TAG, "Error capturing from CCTV");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                count++;
            }
            capturing = false;
            captureCount = 3;
        });
    }

    private void saveImage(Response response) throws IOException {
        File storageDirectory = new File(this.storagePath);
        if(!storageDirectory.exists()){
            storageDirectory.mkdir();
        }
        Log.i(TAG, "Save image is reached");
        // check response/body for null

        Date date = new Date();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                .format(date);

        String filename = timestamp + ".jpg";

        File downloadedFile = new File(this.storagePath, filename);
        BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
        sink.writeAll(response.body().source());
        sink.close();

        if (downloadedFile.exists()){
            Log.i(TAG, "File created: "+ downloadedFile.getPath());
            uploadToServer(downloadedFile);

            /*Box<CameraCapture> cameraCaptureBox = ObjectBox.get().boxFor(CameraCapture.class);
            CameraCapture cameraCapture = new CameraCapture(date, downloadedFile.getPath());

            cameraCaptureBox.put(cameraCapture);*/
        }
    }

    private void uploadToServer(File file){
        AsyncService.execute(()-> {
            String url = "http://<Server IP>:9001/api/files"; // TODO: Make this configurable
            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);
            builder.addFormDataPart("file" , file.getName() , RequestBody.create(MediaType.parse("image/*"), file));
            RequestBody requestBody = builder.build();

            try{
                Response response = NetworkManager.getInstance().post(url, requestBody);

                if(response.isSuccessful()){
                    Log.i(TAG, "Capture uploaded successfully");
                }else{
                    Log.i(TAG, "Upload failed");
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(file.delete()){
                    Log.i(TAG, "Capture deletion success");
                }else{
                    Log.i(TAG, "Capture deletion failed");
                }
            }
        });
    }

}
