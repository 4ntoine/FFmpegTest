package com.ic720.motorola_project.http.service;

import android.util.Log;
import com.ic720.motorola_project.db.EventEntity;
import com.ic720.motorola_project.db.IDao;
import com.ic720.motorola_project.db.SegmentEntity;
import com.ic720.motorola_project.http.exceptions.InvalidStateException;
import com.ic720.motorola_project.http.exceptions.NotFoundException;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class StreamService implements IStreamsService {

    private IDao dao;
    private ManifestBuilder manifestBuilder;

    public StreamService(IDao dao, ManifestBuilder manifestBuilder) {
        this.dao = dao;
        this.manifestBuilder = manifestBuilder;
    }

    protected EventEntity findAndCheckEvent(String event_id) throws NotFoundException {
        EventEntity event = dao.findEvent(event_id);
        if (event == null)
            throw new NotFoundException(MessageFormat.format("Event \"{0}\" not found", event_id));

        return event;
    }

    @Override
    public String getEventManifest(String event_id) throws Exception {
        EventEntity event = findAndCheckEvent(event_id);
        List<SegmentEntity> segments = dao.findSegments(event.started, event.finished);

        // TODO: segments should be sorted according to the time (in dao or here using object field)
        Date now = new Date();
        boolean isLive = now.before(event.finished); // if now is after event.finished full chunks set is recorder and no need to reload it
        int sequence = (isLive ? (int)(now.getTime() - event.started.getTime()) : 1);
        String manifestBody = manifestBuilder.build(segments, isLive, sequence);
        return manifestBody;
    }

    @Override
    public String getSegmentPath(String segment_id) throws Exception {
        SegmentEntity segment = dao.findSegment(segment_id);
        if (segment == null)
            throw new NotFoundException(MessageFormat.format("Segment \"{0}\" not found", segment_id));

        return segment.path;
    }

    private int liveManifestCounter = 0;

    @Override
    public String getLiveManifest() throws Exception {
        // live manifest is build as 1 last segment manifest

        SegmentEntity segment = dao.findLastSegment();
        if (segment == null)
            throw new InvalidStateException("No segments found");

        String liveManifestContent = manifestBuilder.build(Arrays.asList(segment), true, ++liveManifestCounter);

//        Log.i(CameraHttpServer.TAG, "Manifest segments:\n" + liveManifestContent);

        return liveManifestContent;
    }
}
