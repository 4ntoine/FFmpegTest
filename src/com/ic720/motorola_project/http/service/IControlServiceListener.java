package com.ic720.motorola_project.http.service;

import com.splinex.streaming.settings.Settings;

/**
 *
 */
public interface IControlServiceListener {
    void onApiStartRequested(Settings settings);
    void onApiStopRequested();
}
