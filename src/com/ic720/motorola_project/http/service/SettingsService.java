package com.ic720.motorola_project.http.service;

import com.ic720.motorola_project.http.dto.Mode;
import com.ic720.motorola_project.http.exceptions.InvalidArgumentException;
import com.ic720.motorola_project.http.exceptions.InvalidStateException;
import com.splinex.streaming.settings.Settings;

import java.util.List;

/**
 * ISettingsService implementation
 */
public class SettingsService implements ISettingsService {

    public SettingsService(List<Mode> supportedModes) {
        this.supportedModes = supportedModes;
    }

    public Settings getSettings() throws Exception {
        // validate state
        if (mode == null)
            throw new InvalidStateException("Not initialized: mode not set");

        if (bitrate == null)
            throw new InvalidStateException("Not initialized: bitrate not set");

        // get settings
        Settings settings = new Settings();
        settings.setWidth(mode.width);
        settings.setHeight(mode.height);
        settings.setFps_min(mode.fps);
        settings.setFps_max(mode.fps);
        settings.setVideoBitrate(bitrate);

        return settings;
    }

    private List<Mode> supportedModes;

    @Override
    public List<Mode> getSupportedModes() {
        return supportedModes;
    }

    private Mode mode = new Mode(2780, 2780, 10);

    @Override
    public void setMode(Mode mode) throws Exception {
        // check mode is in supported
        boolean found = false;
        for (Mode eachMode : supportedModes)
            if (eachMode.width == mode.width &&
                eachMode.height == mode.height &&
                eachMode.fps == mode.fps) {
                found = true;
                break;
            }

        if (!found)
            throw new InvalidArgumentException("Mode is not supported", "mode", mode.toString());

        // should be applied later in getSettings()
        this.mode = mode;
    }

    @Override
    public Mode getMode() throws Exception {
        if (mode == null)
            throw new InvalidStateException("Mode not set");

        return mode;
    }

    private Integer bitrate = 8192000;

    @Override
    public void setBitrate(int bitrate) throws Exception {
        // should be applied later in getSettings()
        this.bitrate = bitrate;
    }

    @Override
    public int getBitrate() throws Exception {
        if (bitrate == null)
            throw new InvalidStateException("Bitrate not set");

        return bitrate;
    }

    private Integer timeBeforeOffset = 30; // seconds
    private Integer timeAfterOffset = 30;  // seconds

    @Override
    public void setTimeBeforeOffset(int time_before) throws Exception {
        timeBeforeOffset = time_before;
    }

    @Override
    public int getTimeBeforeOffset() throws Exception {
        if (timeBeforeOffset == null)
            throw new InvalidStateException("Value not set");

        return timeBeforeOffset;
    }

    @Override
    public void setTimeAfterOffset(int time_after) throws Exception {
        timeAfterOffset = time_after;
    }

    @Override
    public int getTimeAfterOffset() throws Exception {
        if (timeAfterOffset == null)
            throw new InvalidStateException("Value not set");

        return timeAfterOffset;
    }
}
