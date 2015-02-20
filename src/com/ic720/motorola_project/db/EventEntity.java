package com.ic720.motorola_project.db;

import java.util.Date;

/**
 *
 */
public class EventEntity {
    public String event_id;
    public int type;
    public String payload;
    public Date time;
    public Date started;  // time - offset
    public Date finished; // time + offset
}
