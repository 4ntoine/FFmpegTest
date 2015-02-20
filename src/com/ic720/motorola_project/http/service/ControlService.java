package com.ic720.motorola_project.http.service;

import android.util.Log;
import com.ic720.motorola_project.db.EventEntity;
import com.ic720.motorola_project.db.IDao;
import com.ic720.motorola_project.db.SegmentEntity;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 *
 */
public class ControlService implements IControlService {

    private ISettingsService settingsService;
    private IDao dao;
    private IControlServiceListener listener;

    public ControlService(ISettingsService settingService, IDao dao, IControlServiceListener listener) {
        this.settingsService = settingService;
        this.dao = dao;
        this.listener = listener;
    }

    private boolean isStarted;

    @Override
    public void start() throws Exception {
        listener.onApiStartRequested(settingsService.getSettings());  // delegate actual starting to listener

        isStarted = true; // Warning: this should be in the end as getSettings() may throw exception
    }

    @Override
    public void notifySegment(Date started_time, Date finished_time, String path) {
        // add new segment
        dao.createSegment(started_time, finished_time, path);

        // check and remove unneeded segments

        /*
            timeline:
                             time1                        time2
            ----- * ---------- ] ---###################--- [ --------- * ------> t
              last_event   +offset   unneeded_segments  -offset       now
         */

        EventEntity event = dao.findLastEvent();
        // the last event 'to' time. we don't need segments after it
        Date time1 = (event != null ? event.finished : null);

        // the buffer start time. we do need segments after it as if we get new event we need segment after time2
        Calendar time2 = GregorianCalendar.getInstance();
        time2.setTime(new Date());

        try {
            time2.add(Calendar.SECOND, -settingsService.getTimeBeforeOffset());
        } catch (Exception e) {
            // should not happen as time before has default value
        }

        List<SegmentEntity> segments = dao.findSegments(time1, time2.getTime());
        for (SegmentEntity eachSegment : segments) {
            Log.i(CameraHttpServer.TAG, "deleting segment " + eachSegment.path);
            dao.deleteSegment(eachSegment); // from db
            new File(eachSegment.path).delete(); // from storage
        }
    }

    @Override
    public void stop() throws Exception {
        isStarted = false;
        listener.onApiStopRequested(); // delegate actual stopping to listener
    }

    @Override
    public boolean isStarted() throws Exception {
        return isStarted;
    }
}
