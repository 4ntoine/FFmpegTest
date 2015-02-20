package com.ic720.motorola_project.db;

import java.util.Date;
import java.util.List;

/**
 *
 */
public interface IDao {

    EventEntity createEvent(int type, String payload, Date event_time, Date started, Date finished);
    EventEntity findEvent(String event_id);
    List<EventEntity> findEventsBetween(Date started, Date finished);
    List<EventEntity> findEventsHavingSegment(Date segment_finished);
    List<EventEntity> getEvents();
    EventEntity findLastEvent();
    void deleteEvent(EventEntity event);

    SegmentEntity createSegment(Date started_time, Date finished_time, String path);
    SegmentEntity findSegment(String segment_id);
    List<SegmentEntity> findSegments(Date started, Date finished);
    SegmentEntity findLastSegment();
    void deleteSegment(SegmentEntity segment);
    List<SegmentEntity> getSegments();
}
