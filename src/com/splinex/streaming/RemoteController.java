package com.splinex.streaming;

import java.net.ServerSocket;
import java.net.Socket;

public class RemoteController {
    public static final int PORT = 2010;
    private static int idx = 0;
    private final OnConnectedListener listener;

    private ServerSocket serverSocket;
    private boolean closed = false;
    private final Runnable serverLoop = new Runnable() {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                while (!closed) {
                    Socket client = serverSocket.accept();
                    listener.OnConnected(client);
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                Log.e("Exception: ", e);
            }

        }
    };
    private final int port;

    public RemoteController(OnConnectedListener listener) {
        this.port = PORT + idx;
        idx++;
        this.listener = listener;
        new Thread(serverLoop).start();
    }

    public void close() {
        closed = true;
    }
}
