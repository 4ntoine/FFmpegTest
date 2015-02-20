package com.ic720.motorola_project.http.service;

import com.ic720.motorola_project.http.dto.Mode;
import com.ic720.motorola_project.http.dto.Resolution;
import com.splinex.streaming.settings.Settings;

import java.util.List;

/**
 *
 */
public interface ISettingsService {

    List<Mode> getSupportedModes();
    void setMode(Mode mode) throws Exception;
    Mode getMode() throws Exception;

    void setBitrate(int bitrate) throws Exception;
    int getBitrate() throws Exception;

    void setTimeBeforeOffset(int time_before) throws Exception;
    int getTimeBeforeOffset() throws Exception;

    void setTimeAfterOffset(int time_after) throws Exception;
    int getTimeAfterOffset() throws Exception;

    // to get all settings in our structure
    Settings getSettings() throws Exception;
}
