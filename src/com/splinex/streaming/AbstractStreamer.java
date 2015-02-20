package com.splinex.streaming;

public abstract class AbstractStreamer {


    public abstract void put(Packet packet, int idx);

    public abstract void close();

}
