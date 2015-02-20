package com.splinex.streaming.settings;

import android.content.SharedPreferences;

/**
 * Created by asmirnov on 12.01.15.
 */
public class SharedPreferencesStorage implements IStorage {

    private final String WIDTH = "width";
    private final String HEIGHT = "height";
    private final String FPS_MIN = "fps_min";
    private final String FPS_MAX = "fps_max";
    private final String I_FRAME_INTERVAL = "i_frame_interval";
    private final String VIDEO_BITRATE = "video_bitrate";
    private final String HLS_SEGMENT_LENGTH = "hls_segment_length";
    private final String HLS_SEGMENT_COUNT = "hls_segment_count";

    private SharedPreferences preferences;

    public SharedPreferencesStorage(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public boolean hasSettings() {
        return preferences.contains(WIDTH);
    }

    public Settings load() {
        Settings settings = new Settings();

        settings.setWidth(preferences.getInt(WIDTH, Settings.WIDTH_DEFAULT));
        settings.setHeight(preferences.getInt(HEIGHT, Settings.HEIGHT_DEFAULT));
        settings.setFps_min(preferences.getInt(FPS_MIN, Settings.FPS_DEFAULT));
        settings.setFps_max(preferences.getInt(FPS_MAX, Settings.FPS_DEFAULT));
        settings.setI_frame_interval(preferences.getInt(I_FRAME_INTERVAL, Settings.I_FRAME_INTERVAL_DEFAULT));
        settings.setVideoBitrate(preferences.getInt(VIDEO_BITRATE, Settings.VIDEO_BITRATE_DEFAULT));
        settings.setHlsSegmentLength(preferences.getInt(HLS_SEGMENT_LENGTH, Settings.HLS_SEGMENT_LENGTH_DEFAULT));
        settings.setHlsSegmentCount(preferences.getInt(HLS_SEGMENT_COUNT, Settings.HLS_SEGMENT_COUNT_DEFAULT));

        return settings;
    }

    public void save(Settings settings) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(WIDTH, settings.getWidth());
        editor.putInt(HEIGHT, settings.getHeight());
        editor.putInt(FPS_MIN, settings.getFps_min());
        editor.putInt(FPS_MAX, settings.getFps_max());
        editor.putInt(I_FRAME_INTERVAL, settings.getI_frame_interval());
        editor.putInt(VIDEO_BITRATE, settings.getVideoBitrate());
        editor.putInt(HLS_SEGMENT_LENGTH, settings.getHlsSegmentLength());
        editor.putInt(HLS_SEGMENT_COUNT, settings.getHlsSegmentCount());
        editor.commit();
    }
}
