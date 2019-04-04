package br.com.arena64.testecapturamovimentocabeca;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetWorks implements Runnable {
    private DatagramSocket udpSocket;
    private InetAddress address;
    private int port;
    private ConcurrentLinkedQueue buffer;
    final Pattern pattern = Pattern.compile("([0-9]{1,3}).([0-9]{1,3}).([0-9]{1,3}).([0-9]{1,3})", Pattern.CASE_INSENSITIVE);
    private boolean running = false;

    private final String TAG = "NetWorks";

    public NetWorks(String host, int port, ConcurrentLinkedQueue buffer) throws SocketException, UnknownHostException {
        byte[] ipaddr = new byte[4];
        udpSocket = new DatagramSocket(port);
        Matcher matcher = pattern.matcher(host);
        if(matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                ipaddr[i-1] = (byte)Integer.parseInt(matcher.group(i));
            }
            address = InetAddress.getByAddress(ipaddr);
        } else {
            address = InetAddress.getByName(host);
        }
        this.buffer = buffer;
        this.port = port;
    }

    public void connect() {
        Log.i(TAG, "connect: " + address.getCanonicalHostName() + ":" + port);
        udpSocket.connect(address, port);
    }

    @Override
    public void run() {
        String json = "";
        DatagramPacket packet;
        byte[] stream;
        connect();
        running = true;
        while (running) {
            if((json = (String)buffer.poll())!= null) {
//                Log.i(TAG, "run: envinado " + json);
                stream = json.getBytes();
                packet = new DatagramPacket(stream, stream.length, address, port);
                try {
                    if(!udpSocket.isClosed())
                        udpSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        running = false;
        udpSocket.close();
    }

    public boolean isActive() {
        return running;
    }

}
