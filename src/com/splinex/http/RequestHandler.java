package com.splinex.http;

import static fi.iki.elonen.NanoHTTPD.IHTTPSession;
import static fi.iki.elonen.NanoHTTPD.Response;

public abstract class RequestHandler {

    private String urls[];

    public RequestHandler(String[] urls) {
        this.urls = urls;
    }

    public RequestHandler(String url) {
        this(new String[] { url });
    }

    public abstract Response handle(IHTTPSession session);

    public String[] getUrls() {
        return urls;
    }
}
