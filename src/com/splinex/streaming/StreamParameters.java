package com.splinex.streaming;

import static android.media.MediaRecorder.VideoEncoder.*;

public class StreamParameters {

    private static final byte[] SPS_PPS_2048_2048_BASE = {
            0, 0, 0, 1, 103, 68, -128, 50, -28, 64, 16, 0, 32, 52, 3, 104, 80, -102, -128,
            0, 0, 0, 1, 104, -50, 56, -128
    };

    private static final byte[] SPS_PPS_2048_2048_MAIN = {
            0, 0, 0, 1, 103, 77, 0, 50, -28, 96, 16, 0, 32, 52, 3, 104, 80, -108, -32,
            0, 0, 0, 1, 104, -18, 56, -128,
    };

    private static final byte[] SPS_PPS_2048_2048_HIGH = {
            0, 0, 0, 1, 103, 100, 0, 50, -84, -56, -64, 32, 0, 64, 104, 6, -48, -95, 41, -64,
            0, 0, 0, 1, 104, -18, 56, -80,
    };

    private static final byte[] SPS_PPS_1280_1280_BASE = {
            0, 0, 0, 1, 103, 66, -128, 41, -28, 64, 40, 1, 67, 64, 54, -123, 9, -88,
            0, 0, 0, 1, 104, -50, 56, -128
    };

    private static final byte[] SPS_PPS_1280_1280_MAIN = {
            0, 0, 0, 1, 103, 77, 0, 42, -28, 96, 40, 1, 67, 64, 54, -123, 9, 78,
            0, 0, 0, 1, 104, -18, 56, -128,
    };

    private static final byte[] SPS_PPS_1280_1280_HIGH = {
            0, 0, 0, 1, 103, 100, 0, 42, -84, -56, -64, 80, 2, -122, -128, 109, 10, 18, -100,
            0, 0, 0, 1, 104, -18, 56, -80,
    };

//    private static final byte[] SPS_PPS_1920_1920 = { 0, 0, 0, 1, 103, 68, -128, 50, -28,
//    		64, 60, 1, -29, 64, 54, -123, 9, -88, 0, 0, 0, 1, 104, -50, 56,
//    		-128 };

    public final int idx;
    public final byte[] SPS_PPS;
    public final int height;
    public final int width;
    public final int profile;


    private StreamParameters(int idx, byte[] SPS_PPS, int width, int height, int profile) {
        this.idx = idx;
        this.SPS_PPS = SPS_PPS;
        this.profile = profile;
        this.height = height;
        this.width = width;
    }


    public static final StreamParameters[] params = new StreamParameters[]
            {
                    new StreamParameters(0, SPS_PPS_2048_2048_BASE, 2048, 2048, H264ProfileBaseline),
                    new StreamParameters(1, SPS_PPS_1280_1280_BASE, 1280, 1280, H264ProfileBaseline),
                    new StreamParameters(2, SPS_PPS_2048_2048_MAIN, 2048, 2048, H264ProfileMain),
                    new StreamParameters(3, SPS_PPS_1280_1280_MAIN, 1280, 1280, H264ProfileMain),
                    new StreamParameters(4, SPS_PPS_2048_2048_HIGH, 2048, 2048, H264ProfileHigh),
                    new StreamParameters(5, SPS_PPS_1280_1280_HIGH, 1280, 1280, H264ProfileHigh),
            };

    @Override
    public String toString() {
        String p = profile == H264ProfileBaseline ? "baseline" : profile == H264ProfileMain ? "main" : "high";
        return String.format("%dx%d (%s)", width, height, p);
    }
}
