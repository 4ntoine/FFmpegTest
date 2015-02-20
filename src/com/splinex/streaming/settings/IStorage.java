package com.splinex.streaming.settings;

/**
 * Created by asmirnov on 12.01.15.
 */
public interface IStorage {
    boolean hasSettings();
    Settings load();
    void save(Settings settings);
}
