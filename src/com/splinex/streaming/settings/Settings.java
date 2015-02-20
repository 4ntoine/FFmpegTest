package com.splinex.streaming.settings;

// Streaming settings
public class Settings {

    public static final int WIDTH_DEFAULT = 2048;
    public static final int HEIGHT_DEFAULT = 2048;
    public static final int FPS_DEFAULT = 15;
    public static final int I_FRAME_INTERVAL_DEFAULT = 2;
    public static final int VIDEO_BITRATE_DEFAULT = 2 * 1000 * 1000;
    public static final int HLS_SEGMENT_LENGTH_DEFAULT = 2; // seconds
    public static final int HLS_SEGMENT_COUNT_DEFAULT = 10;

    private int width = WIDTH_DEFAULT;
    private int height = HEIGHT_DEFAULT;
    private int fps_min = FPS_DEFAULT;
    private int fps_max = FPS_DEFAULT;
    private int i_frame_interval = I_FRAME_INTERVAL_DEFAULT;
    private int videoBitrate = VIDEO_BITRATE_DEFAULT;
    private int hlsSegmentLength = HLS_SEGMENT_LENGTH_DEFAULT; // seconds
    private int hlsSegmentCount = HLS_SEGMENT_COUNT_DEFAULT;   // segments count limit


    public int getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public int getI_frame_interval() {
        return i_frame_interval;
    }

    public void setI_frame_interval(int i_frame_interval) {
        this.i_frame_interval = i_frame_interval;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFps_min() {
        return fps_min;
    }

    public void setFps_min(int fps_min) {
        this.fps_min = fps_min;
    }

    public int getFps_max() {
        return fps_max;
    }

    public void setFps_max(int fps_max) {
        this.fps_max = fps_max;
    }

    public int getHlsSegmentLength() {
        return hlsSegmentLength;
    }

    public void setHlsSegmentLength(int hlsSegmentLength) {
        this.hlsSegmentLength = hlsSegmentLength;
    }

    public int getHlsSegmentCount() {
        return hlsSegmentCount;
    }

    public void setHlsSegmentCount(int hlsSegmentCount) {
        this.hlsSegmentCount = hlsSegmentCount;
    }

    @Override
    public boolean equals(Object o) {
        Settings o2;

        if (o == null || ( (o2 =(Settings)o) == null))
            return false;

        return width == o2.getWidth() &&
               height == o2.getHeight() &&
               fps_min == o2.getFps_min() &&
               fps_max == o2.getFps_max() &&
               i_frame_interval == o2.getI_frame_interval() &&
               videoBitrate == o2.getVideoBitrate() &&
               hlsSegmentLength == o2.getHlsSegmentLength() &&
               hlsSegmentCount == o2.getHlsSegmentCount();

    }
}
