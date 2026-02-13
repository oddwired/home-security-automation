package com.kshem.homesecurity.server;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

public class NanoServer extends NanoHTTPD {

    public interface NanoServerCallback{
        void onEvent(String event);
    }

    private static final String TAG = NanoServer.class.getSimpleName();

    private NanoServerCallback callback;

    private Map<String, RequestHandler> requestHandlers;

    public NanoServer(NanoServerCallback callback, RequestHandler... requestHandlers) {
        super(8080);
        this.requestHandlers = new HashMap<>();
        this.callback = callback;

        for(RequestHandler requestHandler: requestHandlers){
            this.requestHandlers.put(requestHandler.getEndpoint(), requestHandler);
        }
    }

    private String extractInitialUri(String uri){
        Pattern pattern = Pattern.compile("^/[^/]+");
        Matcher matcher = pattern.matcher(uri);

        if(matcher.find()){
            return matcher.group();
        }

        return "/";
    }

    @Override
    public Response serve(IHTTPSession ihttpSession) {
        Log.i(TAG, "Request URI: "+ ihttpSession.getUri());
        String initialEndpoint = extractInitialUri(ihttpSession.getUri());
        Log.i(TAG, "initial endpoint: "+ initialEndpoint);

        RequestHandler requestHandler;
        if(this.requestHandlers.containsKey(initialEndpoint)){
            requestHandler = this.requestHandlers.get(initialEndpoint);
        }else{
            return newFixedLengthResponse("OK");
        }

        if(ihttpSession.getMethod().name().equals("POST")){
            Map<String, String> data = new HashMap<>();
            try {
                ihttpSession.parseBody(data);
            } catch (IOException | ResponseException e) {
                e.printStackTrace();
            }

            callback.onEvent("Body: "+ data.get("postData"));

            return requestHandler.post(ihttpSession, data.get("postData"));
        }else{
            return requestHandler.get(ihttpSession);
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        final int bufLen = 4 * 0x400; // 4KB
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((readLen = inputStream.read(buf, 0, bufLen)) != -1)
                    outputStream.write(buf, 0, readLen);

                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inputStream.close();
            else try {
                inputStream.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
        }
    }
}
