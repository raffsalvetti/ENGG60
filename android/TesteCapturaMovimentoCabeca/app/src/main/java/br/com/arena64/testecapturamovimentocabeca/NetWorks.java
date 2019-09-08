package br.com.arena64.testecapturamovimentocabeca;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetWorks {

    public interface Listener {
        void onConnect(final String ipAddress);
        void onDisconnect();
        void onBeacon();
    }

    private InetAddress address, broadcastAddress;
    private static final int port = 3200;
    private Socket socket;
    private DatagramSocket socketBeacon;
    private boolean beaconActive = false;
    private static final byte[] beaconMessage = "DISCOVER_CONTROLLER_REQUEST".getBytes();
    private static final int beaconTime = 10 * 1000; //15 segundos
    private Listener listener;
    private boolean autoconnect;

    public NetWorks(boolean autoconnect, Listener listener) {
        this.listener = listener;
        this.autoconnect = autoconnect;
        try {
            broadcastAddress = InetAddress.getByName("255.255.255.255");
            socketBeacon = new DatagramSocket();
            socketBeacon.setBroadcast(true);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void startBeacon() {
        beaconActive = true;
        waitResponse();
        new Thread(new Runnable() {
            DatagramPacket sendPacket = new DatagramPacket(beaconMessage, beaconMessage.length, broadcastAddress, port);
            @Override
            public void run() {
                while(beaconActive) {
                    try {
                        if(listener != null)
                            listener.onBeacon();
                        socketBeacon.send(sendPacket);
                        Thread.sleep(beaconTime);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        beaconActive = false;
                    }
                }
            }
        }).start();
    }

    public void stopBeacon() {
        beaconActive = false;
    }

    private void waitResponse() {
        new Thread(new Runnable() {
            DatagramPacket receivePacket = null;
            byte[] recvBuf = new byte[8 * 1024];
            @Override
            public void run() {
                while(beaconActive){
                    receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        socketBeacon.receive(receivePacket);
                        String message = new String(receivePacket.getData()).trim();
                        if (message.equals("DISCOVER_CONTROLLER_RESPONSE")) {
                            if(autoconnect) {
                                address = receivePacket.getAddress();
                                connect();
                            }
                            beaconActive = false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        beaconActive = false;
                    }
                }
            }
        }).start();
    }

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(socket == null || !socket.isConnected()) {
                    try {
                        socket = new Socket(address, port);
                        if(listener != null) {
                            listener.onConnect(address.getHostAddress());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void disconnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(listener != null)
                    listener.onDisconnect();
                if(socket != null && socket.isConnected()) {
                    try {
                        socket.close();
                        socket = null;
                        if(autoconnect) {
                            startBeacon();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }).start();
    }

    void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null != socket && socket.isConnected()) {
                        Log.w("SenWorks", "Enviando " + message);
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                        out.println(message);
                        if(out.checkError()) {
                            Log.w("SenWorks", "Erro enviando mensagem! O servidor está em operação?!");
                            disconnect();
                        }
                    } else {
                        Log.w("SenWorks", "Sem conexao com o controlador!");
                        disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
