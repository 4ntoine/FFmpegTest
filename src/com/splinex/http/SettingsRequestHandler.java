package com.splinex.http;

import com.example.ffmpegtest.HLSServer;
import com.example.ffmpegtest.HWRecorderActivity;
import com.splinex.streaming.Log;
import com.splinex.streaming.settings.Settings;
import com.splinex.streaming.Utils;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Map;

public class SettingsRequestHandler extends RequestHandler {

    private static final String DATA = "data";
    private static final String STARTED = "started";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String FPS_MIN = "fps_min";
    private static final String FPS_MAX = "fps_max";
    private static final String I_FRAME_INTERVAL = "i_frame_interval";
    private static final String VIDEO_BITRATE = "video_bitrate";
    private static final String HLS_SEGMENT_LENGTH = "hls_segment_length";
    private static final String HLS_SEGMENT_COUNT = "hls_segment_count";

    private Settings settings;

    public SettingsRequestHandler(Settings settings, SettingsRequestHandler.Listener listener) {
        super("/settings");
        this.settings = settings;
        this.listener = listener;
    }

    /**
     * Listener
     */
    public interface Listener {
        boolean isStarted();
        void onSettingsChanged(Settings settings);
    }

    private Listener listener;

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        Map<String, String> params = session.getParms();
        if (params.containsKey(DATA)) {
            try {
                JSONObject json = (JSONObject) new JSONTokener(params.get(DATA)).nextValue();

                Settings newSettings = new Settings();
                newSettings.setWidth(json.getInt(WIDTH));
                newSettings.setHeight(json.getInt(HEIGHT));
                newSettings.setFps_min(json.getInt(FPS_MIN));
                newSettings.setFps_max(json.getInt(FPS_MAX));
                newSettings.setI_frame_interval(json.getInt(I_FRAME_INTERVAL));
                newSettings.setVideoBitrate(json.getInt(VIDEO_BITRATE));
                newSettings.setHlsSegmentLength(json.getInt(HLS_SEGMENT_LENGTH));
                newSettings.setHlsSegmentCount(json.getInt(HLS_SEGMENT_COUNT));

                if (!newSettings.equals(settings)) {
                    settings = newSettings;
                    listener.onSettingsChanged(newSettings);
                }
            } catch (JSONException e) {
                Log.e("JSONException: ", e);
            }
        }

        JSONObject res = new JSONObject();
        try {
            res.put(STARTED, listener.isStarted());
            res.put(WIDTH, settings.getWidth());
            res.put(HEIGHT, settings.getHeight());
            res.put(FPS_MIN, settings.getFps_min());
            res.put(FPS_MAX, settings.getFps_max());
            res.put(I_FRAME_INTERVAL, settings.getI_frame_interval());
            res.put(VIDEO_BITRATE, settings.getVideoBitrate());
            res.put(HLS_SEGMENT_LENGTH, settings.getHlsSegmentLength());
            res.put(HLS_SEGMENT_COUNT, settings.getHlsSegmentCount());
        } catch (JSONException e) {
            Log.e("JSONException: ", e);
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.INTERNAL_ERROR, HttpServer.MIME_PLAINTEXT, e.getMessage());
        }

        String json = res.toString();
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, HttpServer.MIME_PLAINTEXT, json);
    }
}
