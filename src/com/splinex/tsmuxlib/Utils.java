package com.splinex.tsmuxlib;

public class Utils {

    public static final boolean DEBUG = false;

    static public final long uint32ToLong(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xff) << 24)
                | ((buffer[offset + 1] & 0xff) << 16)
                | ((buffer[offset + 2] & 0xff) << 8)
                | (buffer[offset + 3] & 0xff);
    }

    static final void longToPcr(long pcr, byte[] buffer, int offset) {
        assert (pcr >= 0);
        long pcrLow = pcr & 0x1ff;
        assert (pcrLow >= 0);
        pcr >>= 9;
        assert (pcr >= 0);
        pcr = pcr & 0x1ffffffffL;
        buffer[offset] = (byte) (pcr >> 25);
        buffer[offset + 1] = (byte) (pcr >> 17);
        buffer[offset + 2] = (byte) (pcr >> 9);
        buffer[offset + 3] = (byte) (pcr >> 1);
        buffer[offset + 4] = (byte) (((pcr & 1) << 7) | 0x7e | (pcrLow >>> 8));
        buffer[offset + 5] = (byte) pcrLow;
    }

}
