package com.ic720.motorola_project.http.dto;

import java.text.MessageFormat;

/**
 *
 */
public class Mode {
    public int width;
    public int height;
    public int fps;

    public Mode() {
    }

    public Mode(int width, int height, int fps) {
        this();

        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} x {1} @ {2}", width, height, fps);
    }
}
