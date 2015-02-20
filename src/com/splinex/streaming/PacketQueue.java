package com.splinex.streaming;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketQueue {
    public static final int CLIENT_PACKET_QUEUE_SIZE = 60;
    private final Queue<Packet> queue = new LinkedBlockingQueue<Packet>();
    private Packet spsPacket;
    private boolean spsSent = false;
    private int cleanThreshold = 0;
    private boolean checkFrame;

    public PacketQueue(int cleanThreshold, boolean checkFrame) {
        this.cleanThreshold = cleanThreshold;
        this.checkFrame = checkFrame;
    }

    public void add(Packet p) {
        synchronized (this) {
            if (checkFrame && p.isSPS_PPS())
                spsPacket = p;
            if ((!checkFrame || p.isKey()) && queue.size() > cleanThreshold) {
                if (cleanThreshold > 0)
                    Log.d("CLEAR!!");
                queue.clear();
            }
            queue.add(p);
        }
    }

    public PacketQueue copy() {
        synchronized (this) {
            PacketQueue p = new PacketQueue(CLIENT_PACKET_QUEUE_SIZE, checkFrame);
            p.spsPacket = spsPacket;
            p.queue.addAll(queue);
            return p;
        }
    }

    public Packet pop() {
        if (spsSent)
            return queue.poll();
        else {
            spsSent = true;
            return spsPacket;
        }
    }
}
