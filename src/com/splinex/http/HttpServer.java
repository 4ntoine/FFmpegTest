package com.splinex.http;

import android.content.Context;
import fi.iki.elonen.NanoHTTPD;

import java.util.HashMap;

public class HttpServer extends NanoHTTPD {
    private HashMap<String, RequestHandler> handlers = new HashMap<String, RequestHandler>();
    private FileHandler fileHandler;

    public HttpServer(Context context, int port) {
        super(port);
        fileHandler = new FileHandler(context);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if ("/".equals(uri))
            return redirect("index.html");

        if (handlers.containsKey(uri))
            return handlers.get(uri).handle(session);

        Response response = fileHandler.handle(session);
        return response != null ? response : super.serve(session);
    }

    public void registerHandler(RequestHandler handler) {
        for (String url : handler.getUrls())
            handlers.put(url, handler);
    }

    private Response redirect(String target) {
        Response response = new Response(Response.Status.REDIRECT, "", "");
        response.addHeader("Location", target);
        return response;
    }

}
