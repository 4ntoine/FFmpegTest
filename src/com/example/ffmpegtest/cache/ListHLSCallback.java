package com.example.ffmpegtest.cache;

import com.example.ffmpegtest.HLSFileObserver;

import java.util.List;

/**
 * Wrapper for the list of callbacks
 */
public class ListHLSCallback implements HLSFileObserver.HLSCallback {

    private List<HLSFileObserver.HLSCallback> list;

    public ListHLSCallback(List<HLSFileObserver.HLSCallback> list) {
        this.list = list;
    }


    @Override
    public void onSegmentComplete(String path) {
        for (HLSFileObserver.HLSCallback eachCallback : list)
            eachCallback.onSegmentComplete(path);
    }

    @Override
    public void onManifestUpdated(String path) {
        for (HLSFileObserver.HLSCallback eachCallback : list)
            eachCallback.onManifestUpdated(path);
    }
}
