package com.ic720.motorola_project.http.service;

import com.ic720.motorola_project.http.IRequestHandler;

import java.util.LinkedList;
import java.util.List;

/**
 * Basic IService implementation
 */
public class Namespace implements INamespace {

    private String uri;

    private List<IRequestHandler> handlers = new LinkedList<IRequestHandler>();

    @Override
    public String getUri() {
        return uri;
    }

    public Namespace(String uri) {
        this.uri = uri;
    }

    @Override
    public List<IRequestHandler> getHandlers() {
        return handlers;
    }

    public void addHandler(IRequestHandler handler) {
        handlers.add(handler);
    }

}
