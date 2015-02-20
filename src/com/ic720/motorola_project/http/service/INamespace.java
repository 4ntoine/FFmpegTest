package com.ic720.motorola_project.http.service;

import com.ic720.motorola_project.http.IRequestHandler;

import java.util.List;

/**
 * Handlres namespace interface
 */
public interface INamespace {
    String getUri();
    List<IRequestHandler> getHandlers();
}
