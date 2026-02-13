package com.kshem.homesecurity.server;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

public class UdpServer extends Thread{
    public interface UdpServerCallback{
        void onEvent(String data);
    }

    private static final String TAG = UdpServer.class.getSimpleName();

    private final int serverPort;
    private DatagramSocket socket;
    private final UdpServerCallback callback;

    boolean running;

    public UdpServer(UdpServerCallback callback) {
        super();
        this.serverPort = 8081;
        this.callback = callback;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    @Override
    public void run() {

        running = true;

        try {
            callback.onEvent("Starting UDP Server");
            socket = new DatagramSocket(serverPort);

            callback.onEvent("UDP Server is running");
            Log.e(TAG, "UDP Server is running");

            while(running){
                byte[] buf = new byte[256];

                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);     //this code block the program flow

                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                callback.onEvent("Request from: " + address + ":" + port + "\n");

                String dString = new Date().toString() + "\n"
                        + "Your address " + address.toString() + ":" + port;
                buf = dString.getBytes();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);

            }

            Log.e(TAG, "UDP Server ended");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                Log.e(TAG, "socket.close()");
            }
        }
    }
}
