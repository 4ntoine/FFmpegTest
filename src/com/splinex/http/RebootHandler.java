package com.splinex.http;

import android.content.Context;
import android.os.PowerManager;
import fi.iki.elonen.NanoHTTPD;

public class RebootHandler extends RequestHandler {
    private Context context;

    public RebootHandler(Context context) {
        super("/reboot");
        this.context = context;
    }

    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        if ("1".equals(session.getQueryParameterString())) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            pm.reboot(null);
        }
        return new NanoHTTPD.Response("OK");
    }
}
