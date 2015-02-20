package com.ic720.motorola_project.http.service;

import android.util.Log;
import android.webkit.MimeTypeMap;
import com.ic720.motorola_project.http.HttpServer;
import com.ic720.motorola_project.http.RequestHandler;
import com.ic720.motorola_project.http.dto.Event;
import com.ic720.motorola_project.http.dto.Mode;
import com.ic720.motorola_project.http.exceptions.InvalidStateException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Camera HTTP server
 */
public class CameraHttpServer extends HttpServer {

    public static final String TAG = CameraHttpServer.class.getSimpleName();

    private static final String M3U8_MIME = "application/x-mpegURL";
    private static final String TS_MIME = "video/MP2T";

    private ISettingsService settingsService;

    public ISettingsService getSettingsService() {
        return settingsService;
    }

    public void setSettingsService(ISettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private IControlService controlService;

    public IControlService getControlService() {
        return controlService;
    }

    public void setControlService(IControlService controlService) {
        this.controlService = controlService;
    }

    private IEventsService recordsService;

    public IEventsService getRecordsService() {
        return recordsService;
    }

    public void setEventsService(IEventsService recordsService) {
        this.recordsService = recordsService;
    }

    private IStreamsService streamsService;

    public IStreamsService getStreamsService() {
        return streamsService;
    }

    public void setStreamsService(IStreamsService streamsService) {
        this.streamsService = streamsService;
    }

    public CameraHttpServer(int port, String uri) throws IOException {
        super(port, uri);
        init();
    }

    private void init() {
        initSettingsService();
        initControlService();
        initEventsService();
        initStreamsService();
    }

    private void initStreamsService() {
        final Namespace streamsNamespace = new Namespace("streams");

        streamsNamespace.addHandler(new RequestHandler("record") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                String event_id = getRequiredString(session, "event_id");
                String manifestContent = streamsService.getEventManifest(event_id);
                return new Response(Response.Status.OK, M3U8_MIME, manifestContent);
            }
        });

        streamsNamespace.addHandler(new RequestHandler("live") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                if (!controlService.isStarted())
                    throw new InvalidStateException("Recording is not started");

                String manifestContent = streamsService.getLiveManifest();
                return new Response(Response.Status.OK, M3U8_MIME, manifestContent);
            }
        });

        streamsNamespace.addHandler(new RequestHandler("getSegment") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                String segment_id = getRequiredString(session, "segment_id");
                String path = streamsService.getSegmentPath(segment_id);

//                Log.d(TAG, "segment requested " + segment_id);

                return fileResponse(path, TS_MIME);
            }
        });

        addNamespace(streamsNamespace);
    }

    private void initEventsService() {
        Namespace eventsNamespace = new Namespace("events");

        eventsNamespace.addHandler(new RequestHandler("notify") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                if (!controlService.isStarted())
                    throw new InvalidStateException("Recording is not started");

                int type = getRequiredInteger(session, "type");
                String payload = session.getParms().get("payload");
                Integer time_before_offset = getOptionalInteger(session, "time_before_offset");
                Integer time_after_offset = getOptionalInteger(session, "time_after_offset");

                String event_id = recordsService.notify(type, payload, time_before_offset, time_after_offset);

                return jsonResponse(event_id);
            }
        });

        eventsNamespace.addHandler(new RequestHandler("list") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                List<Event> events = recordsService.list();
                return jsonResponse(events);
            }
        });

        eventsNamespace.addHandler(new RequestHandler("find") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                Date started = getDate(session, "started");
                Date finished = getDate(session, "finished");

                List<Event> events = recordsService.find(started, finished);
                return jsonResponse(events);
            }
        });

        eventsNamespace.addHandler(new RequestHandler("clear") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                recordsService.clear();
                return okResponse();
            }
        });

        addNamespace(eventsNamespace);
    }

    private void initControlService() {
        final Namespace controlNamespace = new Namespace("control");

        controlNamespace.addHandler(new RequestHandler("start") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                if (controlService.isStarted())
                    throw new InvalidStateException("Already started");

                controlService.start();

                return okResponse();
            }
        });

        controlNamespace.addHandler(new RequestHandler("stop") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                if (!controlService.isStarted())
                    throw new InvalidStateException("Already stopped");

                controlService.stop();

                return okResponse();
            }
        });

        controlNamespace.addHandler(new RequestHandler("isStarted") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                boolean isStarted = controlService.isStarted();
                return stringResponse(isStarted);
            }
        });

        addNamespace(controlNamespace);
    }

    private void initSettingsService() {
        final Namespace settingsNamespace = new Namespace("settings");

        settingsNamespace.addHandler(new RequestHandler("setMode") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                int width = getRequiredInteger(session, "width");
                int height = getRequiredInteger(session, "height");
                int fps = getRequiredInteger(session, "fps");

                settingsService.setMode(new Mode(width, height, fps));

                return okResponse();
            }
        });

        settingsNamespace.addHandler(new RequestHandler("getMode") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                Mode mode = settingsService.getMode();
                return jsonResponse(mode);
            }
        });

        settingsNamespace.addHandler(new RequestHandler("getSupportedModes") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                List<Mode> supportedModes = settingsService.getSupportedModes();
                return jsonResponse(supportedModes);
            }
        });

        settingsNamespace.addHandler(new RequestHandler("setBitrate") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                int bitrate = getRequiredInteger(session, "bitrate");
                // TODO : add login control for bitrate
                settingsService.setBitrate(bitrate);
                return okResponse();
            }
        });

        settingsNamespace.addHandler(new RequestHandler("getBitrate") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                int bitrate = settingsService.getBitrate();
                return stringResponse(bitrate);
            }
        });

        settingsNamespace.addHandler(new RequestHandler("setTimeBeforeOffset") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                int time_before = getRequiredInteger(session, "time_before");
                // TODO : validate positive and reasonable
                settingsService.setTimeBeforeOffset(time_before);
                return okResponse();
            }
        });

        settingsNamespace.addHandler(new RequestHandler("getTimeBeforeOffset") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                int time_before = settingsService.getTimeBeforeOffset();
                return stringResponse(time_before);
            }
        });

        settingsNamespace.addHandler(new RequestHandler("setTimeAfterOffset") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                int time_after = getRequiredInteger(session, "time_after");
                // TODO : validate positive and reasonable
                settingsService.setTimeAfterOffset(time_after);
                return okResponse();
            }
        });

        settingsNamespace.addHandler(new RequestHandler("getTimeAfterOffset") {
            @Override
            public Response handle(IHTTPSession session) throws Exception {
                int time_after = getRequiredInteger(session, "time_after");
                return stringResponse(time_after);
            }
        });

        addNamespace(settingsNamespace);
    }
}
