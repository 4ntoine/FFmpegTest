package com.ic720.motorola_project.db;

import java.util.*;

/**
 *
 */
public class InMemoryDao implements IDao {

    private List<EventEntity> events = new ArrayList<EventEntity>();
    private List<SegmentEntity> segments = new ArrayList<SegmentEntity>();

    @Override
    public EventEntity createEvent(int type, String payload, Date event_time, Date started, Date finished) {
        EventEntity event = new EventEntity();
        event.event_id = UUID.randomUUID().toString();
        event.type = type;
        event.payload = payload;
        event.time = event_time;
        event.started = started;
        event.finished = finished;

        // simulate save
        events.add(event);

        return event;
    }

    @Override
    public List<SegmentEntity> findSegments(Date started, Date finished) {
        List<SegmentEntity> foundSegments = new LinkedList<SegmentEntity>();

        for (SegmentEntity eachSegment : this.segments) {
            if (
                    (started == null || eachSegment.started.after(started))
                    &&
                    (finished == null || trimMilliseconds(eachSegment.finished).before(finished))
                )
                foundSegments.add(eachSegment);
        }

        return foundSegments;
    }

    @Override
    public SegmentEntity findLastSegment() {
        if (segments.size() == 0)
            return null;

        return segments.get(segments.size() - 1);
    }

    @Override
    public EventEntity findEvent(String event_id) {
        for (EventEntity eachEvent : events)
            if (eachEvent.event_id.equals(event_id))
                return eachEvent;

        return null;
    }

    @Override
    public SegmentEntity createSegment(Date started_time, Date finished_time, String path) {
        SegmentEntity segment = new SegmentEntity();

        segment.segment_id = UUID.randomUUID().toString();
        segment.started = started_time;
        segment.finished = finished_time;
        segment.path = path;

        // simulate create segment
        segments.add(segment);

        return segment;
    }

    @Override
    public SegmentEntity findSegment(String segment_id) {
        for (SegmentEntity eachSegment : segments)
            if (eachSegment.segment_id.equals(segment_id))
                return eachSegment;

        return null;
    }

    private Date trimMilliseconds(Date date) {
        return new Date(date.getYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
    }

    @Override
    public List<EventEntity> findEventsBetween(Date started, Date finished) {
        List<EventEntity> foundEvents = new LinkedList<EventEntity>();

        for (EventEntity eachEvent : events) {
            // in order to compare as Unix Era we should trim milliseconds
            if (
                    (started == null || (eachEvent.started).after(started))
                    &&
                    (finished == null || !trimMilliseconds(eachEvent.finished).after(finished))
               )
               foundEvents.add(eachEvent);
        }

        return foundEvents;
    }

    @Override
    public List<EventEntity> findEventsHavingSegment(Date segment_finished) {
        List<EventEntity> foundEvents = new LinkedList<EventEntity>();

        for (EventEntity eachEvent : this.events) {
            if (eachEvent.finished.after(segment_finished))
                foundEvents.add(eachEvent);
        }

        return foundEvents;
    }

    @Override
    public void deleteEvent(EventEntity event) {
        events.remove(event);
    }

    @Override
    public List<EventEntity> getEvents() {
        return events;
    }

    @Override
    public EventEntity findLastEvent() {
        if (events.size() == 0)
            return null;

        return events.get(events.size() - 1);
    }

    @Override
    public void deleteSegment(SegmentEntity segment) {
        segments.remove(segment);
    }

    @Override
    public List<SegmentEntity> getSegments() {
        return segments;
    }
}
