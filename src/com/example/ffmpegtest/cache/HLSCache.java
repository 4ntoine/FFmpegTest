package com.example.ffmpegtest.cache;

import android.util.Log;
import com.example.ffmpegtest.HLSFileObserver;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caches segment files
 * (recreates manifest file with specified segments only)
 */
public abstract class HLSCache implements HLSFileObserver.HLSCallback {

    protected String TAG = getClass().getSimpleName();

    // common header parameters
    public static final String TOKEN_SEPARATOR_START = "#";
    public static final String TOKEN_SEPARATOR = ":";
    public static final String TOKEN_HEADER_VERSION = "EXT-X-VERSION";
    public static final String TOKEN_HEADER_DURATION = "EXT-X-TARGETDURATION";
    public static final String TOKEN_HEADER_SEQUENCE = "EXT-X-MEDIA-SEQUENCE";
    public static final String TOKEN_HEADER_SEQUENCE_FULL = TOKEN_SEPARATOR_START + TOKEN_HEADER_SEQUENCE;
    public static final String TOKEN_SEGMENT_START = "EXTINF";
    public static final String TOKEN_SEGMENT_START_FULL = TOKEN_SEPARATOR_START + TOKEN_SEGMENT_START;
    public static final String TOKEN_END = "EXT-X-ENDLIST";

    protected HLSFileObserver.HLSCallback callback;
    protected List<String> manifestHeader;
    protected Map<String, String> headerParams;

    protected String originalManifestFilename; // original manifest
    protected String manifestFilename; // cached manifest
    protected File manifestFile;
    protected File tmpManifestFile;

    private void parseHeader(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        while ((line = br.readLine()) != null &&
                line.startsWith(TOKEN_SEPARATOR_START) &&
                !line.startsWith(TOKEN_SEGMENT_START_FULL)) {

            if (!line.startsWith(TOKEN_HEADER_SEQUENCE_FULL)) // ignore non-constant field ("EXT-X-MEDIA-SEQUENCE")
                manifestHeader.add(line);
            String[] parts = line.split(TOKEN_SEPARATOR); // some header params does not have value
            if (parts.length > 1)
                headerParams.put(parts[0].substring(1), parts[1]); // trim # at the beginning
        }
        br.close();
    }

    private int mediaSequence = 0;

    protected Writer writeHeader() throws IOException {
        // remove existing file
        if (tmpManifestFile.exists()) {
            Log.d(TAG, "Deleting tmp manifest file " + tmpManifestFile.getAbsolutePath());
            tmpManifestFile.delete();
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpManifestFile));
        for (String eachHeaderLine : manifestHeader) {
            writer.write(eachHeaderLine);
            writer.write("\n");
        }

        // write media sequence counter value
        writer.write(TOKEN_SEPARATOR_START);
        writer.write(TOKEN_HEADER_SEQUENCE);
        writer.write(TOKEN_SEPARATOR);
        writer.write(String.valueOf(mediaSequence++));
        writer.write("\n");

        return writer;
    }

    protected void writeEnd(Writer writer) throws IOException {
        writer.write(TOKEN_SEPARATOR_START);
        writer.write(TOKEN_END);
        writer.write("\n");
    }

    protected void writeSegment(Writer writer, String duration, String shortFilename) throws IOException {
        writer.write(TOKEN_SEGMENT_START_FULL);
        writer.write(TOKEN_SEPARATOR);
        writer.write(duration);
        writer.write(",\n");

        writer.write(shortFilename);

        writer.write("\n");
    }

    public HLSCache(String manifestFilename) {
        this.manifestFilename = manifestFilename;

        manifestFile = new File(manifestFilename);
        tmpManifestFile = new File(manifestFilename + ".tmp");
    }

    public void reset() {
        manifestHeader = new ArrayList<String>();
        headerParams = new HashMap<String, String>();
        mediaSequence = 0;
    }

    public void setCallback(HLSFileObserver.HLSCallback callback) {
        this.callback = callback;
    }

    @Override
    public final void onManifestUpdated(String path) {
        originalManifestFilename = path;

        if (manifestHeader.size() == 0)
            try {
                parseHeader(path);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to parse HLS header for " + path, e);
            }

        onManifestUpdated();

        // callback
        if (callback != null)
            callback.onManifestUpdated(manifestFilename);
    }

    // to be overriden

    protected abstract void onManifestUpdated();
}
