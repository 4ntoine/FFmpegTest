package com.ic720.motorola_project.http.service;

import com.ic720.motorola_project.db.SegmentEntity;

import java.util.List;

/**
 *
 */
public class ManifestBuilder {

    private int segmentDuration;

    public int getSegmentDuration() {
        return segmentDuration;
    }

    public void setSegmentDuration(int segmentDuration) {
        this.segmentDuration = segmentDuration;
    }

    private String uriMethod;

    public String getUriMethod() {
        return uriMethod;
    }

    public void setUriMethod(String uriMethod) {
        this.uriMethod = uriMethod;
    }

    private StringBuilder sb;

    public String build(List<SegmentEntity> segments, boolean liveManifest, int mediaSequence) {
        sb = new StringBuilder();

        writeHeader(mediaSequence);
        for (SegmentEntity eachSegment : segments) {
            writeSegment(eachSegment);
        }
        if (!liveManifest)
            writeFooter();

        return sb.toString();
    }

    private void writeFooter() {
        // to let the client know manifest is finished and client should not reload it
        sb.append("#EXT-X-ENDLIST\n");
    }

    private void writeSegment(SegmentEntity segment) {
        sb.append("#EXTINF:"); sb.append(segmentDuration); sb.append(",\n");
        sb.append(uriMethod); sb.append("?segment_id="); sb.append(segment.segment_id); sb.append("\n");
    }

    private void writeHeader(int mediaSequence) {
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:1\n");
        sb.append("#EXT-X-TARGETDURATION:"); sb.append(segmentDuration); sb.append("\n");
        sb.append("#EXT-X-MEDIA-SEQUENCE:"); sb.append(mediaSequence); sb.append("\n");
    }
}
