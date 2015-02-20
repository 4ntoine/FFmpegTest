package com.splinex.streaming;

public class Packet {
    private final byte type;
    final byte[] data;
    final int timeStamp;

    public Packet(byte[] data, int timeStamp) {

        this.data = data;
        this.timeStamp = timeStamp;
        type = (byte) (data[4] & 0x1F);
    }

    public boolean isKey() {
        return type == 0x5;
    }

    public boolean isSPS_PPS() {
        return type == 0x7;
    }
}
