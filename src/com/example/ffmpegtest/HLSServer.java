package com.example.ffmpegtest;

import android.util.Log;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Created by asmirnov on 12.12.14.
 */
public class HLSServer extends NanoHTTPD {

    private static final String TAG = "HLSServer";

    private File wwwroot;

    public HLSServer(int port, File wwwroot) throws IOException {
        super(port, wwwroot);
        this.wwwroot = wwwroot;
    }

    public void setWwwroot(File wwwroot) {
        this.wwwroot = wwwroot;
    }

    public File getWwwroot() {
        return wwwroot;
    }

    private static final String MIME_TYPE = "application/octet-stream";

    /**
     * Connect event listener
     */
    public static interface Listener {
        // need to return filename for FileInputStream
        File onManifestRequested();

        void onSegmentDownloadStart(String url);
        void onSegmentDownloadFinished(String url);
    }

    private Listener listener;

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {

        Log.w(TAG, "serve: uri = " + uri);

        File file;
        boolean isManifest;
        if (uri.endsWith(".m3u8")) {
            isManifest = true;
            file = listener.onManifestRequested(); // listener should generate manifest file and return it's filename
            Log.i(TAG, "manifest file requested: " + uri);
        } else {
            isManifest = false;
            file = new File(wwwroot, uri);
            listener.onSegmentDownloadStart(uri);
        }

        Log.i(TAG, "File requested: " + uri + " -> " + file.getAbsolutePath());

        InputStream is = null;
        try {
            if (isManifest) {
                String fileString = FileUtils.getStringFromFile(file.getAbsolutePath());
                Log.i(TAG, "Manifest content:\n" + fileString);
//                is = new FileInputStream(file);
            } else {
//                return serveFile(uri, header, file.getParentFile().getAbsoluteFile(), false);
            }

//            is = new FileInputStream(file);
        } catch (Exception e) {
            Log.e(TAG, "failed to find " + file);
            return new Response("404", MIME_TYPE, "error");
        }

        Log.d(TAG, "serveFile() started");
        serveStarted = new Date().getTime();
        return serveFile(uri, header, wwwroot, false);

//        Response response = new Response(HTTP_OK, MIME_TYPE, is);
//        response.isStreaming = true;
//        return response;
    }

    private long serveStarted;

    @Override
    public void serveDone(Response r) {
        long ms = new Date().getTime() - serveStarted;
        Log.d(TAG, "serveFile() finished in " + ms + " ms");

//        try {
//            if (r.isStreaming) {
//                r.data.close(); // close the stream after segment downloaded
//            }

            listener.onSegmentDownloadFinished(r.uri);


//        } catch(IOException e) {
//            Log.e(TAG, "Failed to close input stream for " + r.uri, e);
//        }
    }
}
