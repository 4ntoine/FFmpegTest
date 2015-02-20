package com.ic720.motorola_project.http;

import com.ic720.motorola_project.http.exceptions.InvalidStateException;
import com.ic720.motorola_project.http.exceptions.NotFoundException;
import com.ic720.motorola_project.http.exceptions.ValidationException;
import com.ic720.motorola_project.http.service.INamespace;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP server base on NanoHTTPD
 */
public class HttpServer extends NanoHTTPD {

    private Map<String, IRequestHandler> handlers = new HashMap<String, IRequestHandler>();

    public void addHandler(String uri, IRequestHandler handler) {
        handlers.put(uri, handler);
    }

    private static final String SLASH = "/";

    public void addNamespace(INamespace namespace) {
        for (IRequestHandler eachHandler : namespace.getHandlers()) {
            StringBuilder uriBulder = new StringBuilder();
            if (uri != null) {
                uriBulder.append(SLASH);
                uriBulder.append(uri);
            }

            uriBulder.append(SLASH);
            uriBulder.append(namespace.getUri());
            uriBulder.append(SLASH);
            uriBulder.append(eachHandler.getUri());

            addHandler(uriBulder.toString(), eachHandler);
        }
    }

    private final String TAG = getClass().getSimpleName();

    private String uri;

    public HttpServer(int port, String uri) throws IOException {
        super(port);
        this.uri = uri;
    }

    public static final String MIME_TEXT_PLAIN = "text/plain";

    @Override
    public Response serve(IHTTPSession session) {
        IRequestHandler handler = handlers.get(session.getUri());

        try {
            if (handler != null)
                return handler.handle(session);
        } catch (ValidationException e) {
            // arguments validation
            return new Response(Response.Status.BAD_REQUEST, MIME_TEXT_PLAIN, e.getMessage());
        } catch (NotFoundException e) {
            // logic - not found
            return new Response(Response.Status.NO_CONTENT, MIME_TEXT_PLAIN, e.getMessage());
        } catch (InvalidStateException e) {
            // logic - invalid state
            return new Response(Response.Status.BAD_REQUEST, MIME_TEXT_PLAIN, e.getMessage());
        } catch (Exception e) {
            // all other errors
            return new Response(Response.Status.INTERNAL_ERROR, MIME_TEXT_PLAIN, e.getMessage());
        }

        return new Response(Response.Status.NOT_FOUND, MIME_TEXT_PLAIN, "404: Not found");
    }


}
