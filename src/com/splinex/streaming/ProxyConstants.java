package com.splinex.streaming;

public class ProxyConstants {
    public static final String PROXY_SERVER = "192.168.3.123";
    public static final int PROXY_PORT = 8000;
    // for proxy: first 4 bytes for frame size, then 1 byte as keyframe flag
    static boolean useProxy = false;
}
