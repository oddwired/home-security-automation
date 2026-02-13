package com.kshem.homesecurity.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UdpSender {
    public static void send(String ip, int port, String message) {
        final byte[] buf = message.getBytes(StandardCharsets.UTF_8);
        new Thread(() -> {
            try {
                InetAddress serverAddress = InetAddress.getByName(ip);

                DatagramSocket socket = new DatagramSocket();
                if (!socket.getBroadcast()) socket.setBroadcast(true);
                DatagramPacket packet = new DatagramPacket(buf, buf.length,
                        serverAddress, port);
                socket.send(packet);
                byte[] responseBuf = new byte[2];
                DatagramPacket responsePacket = new DatagramPacket(responseBuf,
                        responseBuf.length);
                socket.receive(responsePacket);
                socket.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}