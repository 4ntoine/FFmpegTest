package com.splinex.streaming;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

class Client {
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
    private OutputStream tos;
    private long lastPacketRealTime = 0;
    private int lastPacketTimeStamp = 0;

    private boolean send(Packet p) {
        try {
            if (!p.isSPS_PPS()) {
                long now = System.nanoTime();
                if (lastPacketRealTime > 0)
                    try {
                        long time = (p.timeStamp - lastPacketTimeStamp) * 1000l - (now - lastPacketRealTime);
                        if (time > 0) {
//                            Log.d("Sleep "+(time/1000000));
                            Thread.sleep(time / 1000000, (int) (time % 1000000));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                if (tos != null) {
                    int ts = p.timeStamp;
                    byte[] data = new byte[]{(byte) (ts & 0xFF), (byte) (ts >> 8), (byte) (ts >> 16), (byte) (ts >> 24)};
                    tos.write(data);
                }

                lastPacketRealTime = now;
                lastPacketTimeStamp = p.timeStamp;
            }
            os.write(p.data);
        } catch (IOException e) {
            Log.e("IOException ", e);
            return false;
        }
        return true;
    }

    private final Socket socket;
    private PacketQueue packets;

    public Client(final Socket socket, PacketQueue packets) {
        this.socket = socket;
        try {
            this.os = socket.getOutputStream();
            this.packets = packets;
            new Thread(mainLoop).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean put(Packet p) {
        packets.add(p);
        return connected;
    }

    public void setTimestampSocket(Socket timestampSocket) throws IOException {
        this.tos = timestampSocket.getOutputStream();
    }

    public boolean hasTimestampSocket() {
        return this.tos != null;
    }
}
