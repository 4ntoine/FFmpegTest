package com.ic720.motorola_project.http.service;

import java.util.Date;

/**
 * Recorder/streamer control service
 */
public interface IControlService {
    void start() throws Exception;
    void stop() throws Exception;
    boolean isStarted() throws Exception;

    void notifySegment(Date started_time, Date finished_time, String path);
}
