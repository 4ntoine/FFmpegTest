package com.splinex.http;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by asmirnov on 12.01.15.
 */
public class ControlRequestHandler extends RequestHandler {

    /**
     * Listener
     */
    public interface Listener {
        boolean onStartRequested();
        boolean onStopRequested();
    }

    private ControlRequestHandler.Listener listener;

    private static final String BASE_URL = "/control";
    private static final String START_URL = BASE_URL + "/start";
    private static final String STOP_URL  = BASE_URL + "/stop";

    public ControlRequestHandler(ControlRequestHandler.Listener listener) {
        super(new String[] { START_URL, STOP_URL });
        this.listener = listener;
    }

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {

        boolean done;

        if (session.getUri().equals(START_URL))
            done = listener.onStartRequested();
        else
        if (session.getUri().equals(STOP_URL))
            done = listener.onStopRequested();
        else
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.BAD_REQUEST, HttpServer.MIME_PLAINTEXT, "unknown action");

        String message = (done ? "action done" : "action ignored (probably already in that state)");
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, HttpServer.MIME_PLAINTEXT, message);
    }
}
