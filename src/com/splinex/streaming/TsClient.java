package com.splinex.streaming;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

class TsClient {
    private OutputStream os;
    private boolean connected = false;
    private final Runnable mainLoop = new Runnable() {
        @Override
        public void run() {
            try {
                connected = true;
                while (connected) {
                    Packet p = packets.pop();
                    if (p != null)
                        connected &= send(p);
                    else
                        Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                connected = false;
                Log.e("Interrupted", e);
            }

            try {
                socket.close();
            } catch (IOException e) {
                Log.e("IOException while closing client socket");
            }
        }
    };

    private boolean send(Packet p) {
        try {
            os.write(p.data);
        } catch (IOException e) {
            Log.e("IOException ", e);
            return false;
        }
        return true;
    }

    private final Socket socket;
    private PacketQueue packets;

    public TsClient(Socket socket, Packet packet, int cleanThreshold) {
        this.socket = socket;
        try {
            this.os = socket.getOutputStream();
            this.packets = new PacketQueue(cleanThreshold, false);
            if (packet != null)
                packets.add(packet);
            new Thread(mainLoop).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean put(Packet p) {
        packets.add(p);
        return connected;
    }

    public void close() {
        connected = false;
    }
}
