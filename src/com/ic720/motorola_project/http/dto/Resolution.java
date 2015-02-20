package com.ic720.motorola_project.http.dto;

/**
 * Video resolution
 */
public class Resolution {
    public int width;
    public int height;

    public Resolution() {
    }

    public Resolution(int width, int height) {
        this();
        this.width = width;
        this.height = height;
    }
}
