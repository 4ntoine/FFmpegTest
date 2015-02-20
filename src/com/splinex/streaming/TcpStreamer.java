package com.splinex.streaming;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class TcpStreamer extends AbstractStreamer {
    private static final int PORT = 2000;
    private static final int TS_PORT_DELTA = 1000;
    private static final int LOG_RATE = 50;
    private final PacketQueue packets = new PacketQueue(0,true);
    private boolean closed = false;
    private ServerSocket serverSocket, tsServerSocket;
    private final ArrayList<Client> clients = new ArrayList<Client>();
    private final LinkedBlockingQueue<Socket> waiting = new LinkedBlockingQueue<Socket>();
    private final Runnable serverLoop = new Runnable() {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                while (!closed) {
                    clients.add(new Client(serverSocket.accept(), packets.copy()));
                    checkWaiting();
                    Log.e("Client to " + port);
                }
            } catch (IOException e) {
                Log.e("IOException: ", e);
            }
        }
    };

    private void checkWaiting() throws IOException {
        synchronized (this) {
            if (waiting.isEmpty() || clients.isEmpty()) return;
            Client lastClient = clients.get(clients.size() - 1);
            if (!lastClient.hasTimestampSocket())
                lastClient.setTimestampSocket(waiting.poll());
        }
    }

    private final Runnable tsServerLoop = new Runnable() {
        @Override
        public void run() {
            try {
                tsServerSocket = new ServerSocket(port + TS_PORT_DELTA);
                while (!closed) {
                    waiting.add(tsServerSocket.accept());
                    checkWaiting();
                    Log.e("TS Client to " + (port + TS_PORT_DELTA));
                }
            } catch (IOException e) {
                Log.e("IOException: ", e);
            }
        }
    };
    private final int port;
    private int frame = 0;
    private long start = 0, time = 0;

    public TcpStreamer(int port) {
        this.port = port;
        new Thread(serverLoop).start();
        new Thread(tsServerLoop).start();
    }

    @Override
    public void put(Packet p, int idx) {
        if (start == 0)
            start = time = System.nanoTime();
        if (++frame % LOG_RATE == 0) {
            long now = System.nanoTime();
            Log.d((port - PORT) + ". frames=" + frame + " fps=" + ((1e9 * LOG_RATE) / (now - time)) + "/" + ((1e9 * frame) / (now - start)));
            time = now;
        }
        packets.add(p);
        Client remove = null;
        for (Client client : clients) {
            boolean res = client.put(p);
            if (!res)
                remove = client;
        }
        if (remove != null)
            clients.remove(remove);
    }


    @Override
    public void close() {
        if (closed)
            return;
        closed = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e("IOException while closing server socket");
        }
        clients.clear();
    }
}
