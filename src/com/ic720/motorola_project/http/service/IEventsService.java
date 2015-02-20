package com.ic720.motorola_project.http.service;

import com.ic720.motorola_project.http.dto.Event;

import java.util.Date;
import java.util.List;

/**
 * Events data service
 */
public interface IEventsService {
    String notify(int type, String payload, Integer time_before_offset, Integer time_after_offset) throws Exception;

    List<Event> find(Date started, Date finished) throws Exception; // event_id's list
    List<Event> list() throws Exception; // = get all the events

    void clear();
}
