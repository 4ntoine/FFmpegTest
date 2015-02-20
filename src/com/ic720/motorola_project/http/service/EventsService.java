package com.ic720.motorola_project.http.service;

import com.ic720.motorola_project.db.EventEntity;
import com.ic720.motorola_project.db.IDao;
import com.ic720.motorola_project.db.SegmentEntity;
import com.ic720.motorola_project.http.dto.Event;

import java.io.File;
import java.util.*;

/**
 *
 */
public class EventsService implements IEventsService {

    private IDao dao;
    private ISettingsService settingsService;

    public EventsService(ISettingsService settingService, IDao dao) {
        this.dao = dao;
        this.settingsService = settingService;
    }

    @Override
    public String notify(int type, String payload, Integer time_before_offset, Integer time_after_offset) throws Exception {
        Calendar event_time = GregorianCalendar.getInstance();

        // from time
        Calendar started = (Calendar)event_time.clone();
        if (time_before_offset == null)
            time_before_offset = settingsService.getTimeBeforeOffset();
        started.add(Calendar.SECOND, -time_before_offset);

        // to time
        Calendar finished = (Calendar)event_time.clone();
        if (time_after_offset == null)
            time_after_offset = settingsService.getTimeAfterOffset();
        finished.add(Calendar.SECOND, time_after_offset);

        // add event
        EventEntity event = dao.createEvent(type, payload, event_time.getTime(), started.getTime(), finished.getTime());

        return event.event_id;
    }

    @Override
    public List<Event> find(Date started, Date finished) throws Exception {
        List<Event> response = new LinkedList<Event>();

        List<EventEntity> events = dao.findEventsBetween(started, finished);
        for (EventEntity eachEntity : events)
            response.add(mapEvent(eachEntity));

        return response;
    }

    @Override
    public List<Event> list() throws Exception {
        List<Event> response = new LinkedList<Event>();

        List<EventEntity> events = dao.getEvents();
        for (EventEntity eachEntity : events)
            response.add(mapEvent(eachEntity));

        return response;
    }

    private Event mapEvent(EventEntity dbObject) {
        Event dtoObject = new Event();

        dtoObject.event_id = dbObject.event_id;
        dtoObject.type = dbObject.type;
        dtoObject.payload = dbObject.payload;
        dtoObject.time = dbObject.time;
        dtoObject.started = dbObject.started;
        dtoObject.finished = dbObject.finished;

        return dtoObject;
    }

    @Override
    public void clear() {
        // events
        List<EventEntity> events = dao.getEvents();
        for (EventEntity eachEvent : events)
            dao.deleteEvent(eachEvent);

        // segments
        List<SegmentEntity> segments = dao.getSegments();
        for (SegmentEntity eachSegment : segments) {
            dao.deleteSegment(eachSegment);
            new File(eachSegment.path).delete();
        }
    }
}
