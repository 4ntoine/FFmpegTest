package com.splinex.http;

import android.content.Context;
import android.content.res.AssetManager;
import android.webkit.MimeTypeMap;
import com.splinex.streaming.Log;
import fi.iki.elonen.NanoHTTPD;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileHandler extends RequestHandler {
    public static final String HTTP_ROOT = "http_root";
    private Context context;

    public FileHandler(Context context) {
        super("");
        this.context = context;
    }

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        String filename = session.getUri();
        AssetManager assets = context.getAssets();
        try {
            InputStream is = assets.open(HTTP_ROOT + filename);
            String mime = getMime(filename);
            Log.d(filename + " => " + mime);
            return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, mime, is);
        } catch (FileNotFoundException e) {
            Log.w("File not found: " + filename);
        } catch (IOException e) {
            Log.e("IOException: ", e);
        }

        return null;
    }

    private String getMime(String filename) {
        int idx = filename.lastIndexOf(".");
        String ext = filename.substring(idx + 1);
        return "js".equals(ext) ? "text/javascript" :
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }
}
