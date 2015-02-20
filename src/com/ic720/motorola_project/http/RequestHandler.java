package com.ic720.motorola_project.http;

import android.webkit.MimeTypeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.ic720.motorola_project.http.exceptions.NoArgumentException;
import com.ic720.motorola_project.http.exceptions.InvalidArgumentException;
import com.ic720.motorola_project.http.exceptions.ValidationException;
import fi.iki.elonen.NanoHTTPD;

import java.io.*;
import java.util.Date;

/**
 * Abstract IRequestHandler implementation
 */
public abstract class RequestHandler implements IRequestHandler {

    private String uri;

    @Override
    public String getUri() {
        return uri;
    }

    public RequestHandler(String uri) {
        this.uri = uri;
    }

    public int getRequiredInteger(NanoHTTPD.IHTTPSession session, String name) throws ValidationException {
        String strValue = session.getParms().get(name);
        if (strValue == null)
            throw new NoArgumentException(name);

        try {
            return Integer.parseInt(strValue);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException(e, name, strValue);
        }
    }

    public Date getDate(NanoHTTPD.IHTTPSession session, String name) throws ValidationException {
        String strValue = session.getParms().get(name);
        if (strValue == null)
            return null;

        try {
            return new Date(Long.parseLong(strValue) * 1000); // unix era
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException(e, name, strValue);
        }
    }

    public String getRequiredString(NanoHTTPD.IHTTPSession session, String name) throws ValidationException {
        String strValue = session.getParms().get(name);
        if (strValue == null)
            throw new NoArgumentException(name);

        return strValue;
    }

    public Integer getOptionalInteger(NanoHTTPD.IHTTPSession session, String name) throws ValidationException {
        String strValue = session.getParms().get(name);
        if (strValue == null)
            return null;

        try {
            return Integer.parseInt(strValue);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException(e, name, strValue);
        }
    }

    public NanoHTTPD.Response okResponse(String mimeType, String response) {
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, mimeType, response);
    }

    public NanoHTTPD.Response okResponse() {
        return okResponse(HttpServer.MIME_TEXT_PLAIN, null);
    }

    private static Gson gson;


    /**
     * Adapter to read/write Date as Unix Era
     */
    public static class DateAdapter extends TypeAdapter<Date> {

        @Override
        public void write(JsonWriter jsonWriter, Date date) throws IOException {
            long seconds = date.getTime() / 1000;
            jsonWriter.value(seconds);
        }

        @Override
        public Date read(JsonReader jsonReader) throws IOException {
            long milliseconds = jsonReader.nextLong() * 1000;
            return new Date(milliseconds);
        }
    }

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.registerTypeAdapter(Date.class, new DateAdapter()); // write Date (started/finished) as long
        gson = builder.create();
    }

    public NanoHTTPD.Response jsonResponse(Object entity) {
        String json = gson.toJson(entity);
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "application/json", json);
    }

    public NanoHTTPD.Response stringResponse(Object value) {
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, HttpServer.MIME_TEXT_PLAIN, String.valueOf(value));
    }

    private String getMime(String filename) {
        int idx = filename.lastIndexOf(".");
        String ext = filename.substring(idx + 1);
        return "js".equals(ext) ? "text/javascript" :
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    public NanoHTTPD.Response fileResponse(String path, String mime) throws FileNotFoundException {
        if (mime == null)
            mime = getMime(path);
        InputStream is = new BufferedInputStream(new FileInputStream(path));
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, mime, is);
    }

}
