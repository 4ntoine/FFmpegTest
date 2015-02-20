package com.example.ffmpegtest.cache;

import android.util.Log;
import com.example.ffmpegtest.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Caches specified HLS segment count
 */
public class SegmentCountCache extends HLSCache {

    private List<String> segmentFilenames;
    private int count;
    private boolean deleteSegments;
    private boolean enabled = true;

    public SegmentCountCache(int count, boolean deleteSegments, String manifestFilename) {
        super(manifestFilename);
        this.count = count;
        this.deleteSegments = deleteSegments;

        reset();
    }

    @Override
    public void reset() {
        super.reset();

        segmentFilenames = new ArrayList<String>(count);
    }

    private void createManifest() throws IOException {
        Writer writer = writeHeader();

        try {
            // segments
            for (String eachSegmentFilename : segmentFilenames) {
                // assume all segments have exactly declared length
                writeSegment(writer, headerParams.get(TOKEN_HEADER_DURATION), new File(eachSegmentFilename).getName());
            }

            // end (write to let client stop)
//            writeEnd(writer);
        } finally {
            if (writer == null)
                return;

            writer.flush();
            writer.close();

            // delete existing ready manifest file
            if (manifestFile.exists()) {
                Log.i(TAG, "Delete manifest file " + manifestFilename);
                manifestFile.delete();
            }

            // rename temporary manifest file to ready manifest file
            Log.i(TAG, "Renaming tmp manifest file to manifest file:" + manifestFile);
            tmpManifestFile.renameTo(manifestFile);

            boolean manifestExists = manifestFile.exists();
            Log.i(TAG, "Check manifest file exists: " + manifestFilename + ": " + manifestExists);

            if (manifestExists) {
                String manifestContent = null;
                try {
                    manifestContent = FileUtils.getStringFromFile(manifestFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Manifest file content:\n" + manifestContent);
            }
        }
    }

    @Override
    public void onSegmentComplete(String path) {
        // keep segment count < limit
        if (enabled && segmentFilenames.size() == count) {
            String deleteSegmentFilename = segmentFilenames.get(0);
            segmentFilenames.remove(0);

            if (deleteSegments) {
                Log.i(TAG, "Deleting segment file " + new File(deleteSegmentFilename).getName());
                new File(deleteSegmentFilename).delete();
            }
        }

        segmentFilenames.add(path);

        // re/-create manifest if original manifest is already created
        if (manifestHeader.size() > 0)
            try {
                createManifest();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to create manifest " + manifestFilename, e);
            }

        // callback
        if (callback != null)
            callback.onSegmentComplete(path);
    }

    @Override
    protected void onManifestUpdated() {
        if (segmentFilenames.size() == 1) {

            // the first segment is created before manifest written but we need manifest header information
            // that's why we skip first segment created event and write manifest only when original manifest is created
            try {
                createManifest();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to create manifest " + manifestFilename, e);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
