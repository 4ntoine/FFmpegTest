package com.ic720.motorola_project.http;

import fi.iki.elonen.NanoHTTPD;

/**
 * Incoming http request handler
 */
public interface IRequestHandler {
    String getUri();
    NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) throws Exception;
}
