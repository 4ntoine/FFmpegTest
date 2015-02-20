package com.ic720.motorola_project.http.service;

/**
 *
 */
public interface IStreamsService {
    String getEventManifest(String event_id) throws Exception;
    String getSegmentPath(String segment_id) throws Exception;
    String getLiveManifest() throws Exception;
}
