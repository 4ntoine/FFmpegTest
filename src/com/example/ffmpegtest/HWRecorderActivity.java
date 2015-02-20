/*
 * Copyright (c) 2013, David Brodsky. All rights reserved.
 *
 *	This program is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.ffmpegtest;

import android.os.Environment;
import android.widget.Toast;
import com.example.ffmpegtest.recorder.LiveHLSRecorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import com.ic720.motorola_project.db.IDao;
import com.ic720.motorola_project.db.InMemoryDao;
import com.ic720.motorola_project.http.dto.Mode;
import com.ic720.motorola_project.http.service.*;
import com.splinex.http.ControlRequestHandler;
import com.splinex.http.HttpServer;
import com.splinex.http.SettingsRequestHandler;
import com.splinex.streaming.TcpStreamer;
import com.splinex.streaming.settings.IStorage;
import com.splinex.streaming.settings.Settings;
import com.splinex.streaming.TsStreamer;
import com.splinex.streaming.settings.SharedPreferencesStorage;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HWRecorderActivity extends Activity
        implements
            HLSServer.Listener,
            HLSFileObserver.HLSCallback,
            SettingsRequestHandler.Listener,
            ControlRequestHandler.Listener,
            IControlServiceListener {

    private static final String TAG = "HWRecorderActivity";
    private boolean recording = false;
    private LiveHLSRecorder liveRecorder;

    private TextView liveIndicator;
    private String broadcastUrl;
    private Button button;

    private IStorage settingsStorage;

    @Override
    public boolean isStarted() {
        return recording;
    }

    // "start" requested by http
    @Override
    public boolean onStartRequested() {
        if (recording)
            return false;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HWRecorderActivity.this, "Start() requested by HTTP", Toast.LENGTH_LONG).show();
                startRecording();
            }
        });

        return true;
    }

    @Override
    // motorola API requested start
    public void onApiStartRequested(Settings settings) {
        // paths
        File baseRoot = getBaseOutputFolder();
        File outputRoot = new File(baseRoot, UUID.randomUUID().toString());
        initPaths(outputRoot);
        hlsServer.setWwwroot(outputRoot);

        // missing settings
        settings.setI_frame_interval(1);
        settings.setHlsSegmentLength(1); // should be bigger than i-frame interval
        settings.setHlsSegmentCount(10);
        this.settings = settings;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HWRecorderActivity.this, "Start() requested using API", Toast.LENGTH_LONG).show();
                startRecording();
            }
        });
    }

    @Override
    // motorola API requested stop
    public void onApiStopRequested() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HWRecorderActivity.this, "Stop() requested using API", Toast.LENGTH_LONG).show();
                stopRecording();
            }
        });
    }

    // "stop" requested by http
    @Override
    public boolean onStopRequested() {
        if (!recording)
            return false;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HWRecorderActivity.this, "Stop() requested by HTTP", Toast.LENGTH_LONG).show();
                stopRecording();
            }
        });

        return true;
    }

    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_hwrecorder);
        liveIndicator = (TextView) findViewById(R.id.liveLabel);
        button = (Button) findViewById(R.id.button1);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordButtonClicked(v);
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
        	      new IntentFilter(LiveHLSRecorder.INTENT_ACTION));

        settingsStorage = new SharedPreferencesStorage(getSharedPreferences("SETTINGS", MODE_PRIVATE));

        cpuMonitor = new CpuMonitor();
        cpuMonitor.start();

//        if (VIDEO_STREAMS_COUNT == 1) {
            // note3 version
            settings = new Settings();
            settings.setWidth(1280);
            settings.setHeight(720);
            settings.setHlsSegmentLength(2);  // seconds
            settings.setHlsSegmentCount(10);  // files limit
            settings.setVideoBitrate((2 * 1000 * 1000));
            settings.setFps_min(30);
            settings.setFps_max(30);
            settings.setI_frame_interval(1); // less than segment length !!! Otherwise some segments will not have stream params
//        } else {
            // dragonboard version
            settings = (settingsStorage.hasSettings()
                ? settingsStorage.load()
                : new Settings());
//        }

        startHLSServer();

//        if (VIDEO_STREAMS_COUNT == 1) {
            startMotorolaAPIServer();
//        } else {
            startSettingsServer();
//        }

        // start streaming
        /*
        onApiStartRequested(settings);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    motorolaApiServer.getRecordsService().getLiveManifest();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 5000);
        */
    }

    private static final int MOTOROLA_API_PORT = 8081;

    private void startMotorolaAPIServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (motorolaApiServer == null) {
                    try {
                        motorolaApiServer = new CameraHttpServer(MOTOROLA_API_PORT, "api");

                        List<Mode> supportedModes = new LinkedList<Mode>();
                        supportedModes.add(new Mode(1280, 1280, 30));
                        supportedModes.add(new Mode(2048, 2048, 22));
                        supportedModes.add(new Mode(2780, 2780, 10));

                        IDao dao = new InMemoryDao();

                        ISettingsService settingsService = new SettingsService(supportedModes);
                        motorolaApiServer.setSettingsService(settingsService);
                        motorolaApiServer.setControlService(new ControlService(settingsService, dao, HWRecorderActivity.this));
                        motorolaApiServer.setEventsService(new EventsService(settingsService, dao));

                        ManifestBuilder manifestBuilder = new ManifestBuilder();
                        manifestBuilder.setUriMethod("getSegment");
                        manifestBuilder.setSegmentDuration(1); // TODO : read segment length from settings
                        motorolaApiServer.setStreamsService(new StreamService(dao, manifestBuilder));

                        motorolaApiServer.start();
                        Log.i(TAG, "Motorola API server started");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private File getBaseOutputFolder() {
        return new File(Environment.getExternalStorageDirectory(), "HLSRecorder");
    }

    @Override
    public void onSettingsChanged(Settings settings) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HWRecorderActivity.this, "Settings changed by HTTP", Toast.LENGTH_LONG).show();
            }
        });

        this.settings = settings;
        settingsStorage.save(settings);

        // restart with new parameters if is recording
        if (recording) {
            stopRecording();
            startRecording();
        }
    }

    private void initPaths(File outputPath) {
//        String uuid = UUID.randomUUID().toString();
        this.outputPath = outputPath;
        if (!outputPath.exists()) {
            if (!outputPath.mkdirs())
                Log.e(TAG, "Failed to create dir: " + outputPath.getAbsolutePath());
        }

        manifestFilename = new File(outputPath, "index.m3u8");
        liveManifestFilename = new File(outputPath, "live.m3u8");
    }

    private Settings settings;

    private HLSServer hlsServer;
    private HttpServer settingsServer; // dragonboard API
    private CameraHttpServer motorolaApiServer;
    private File outputPath;
    private File manifestFilename = null;
    private File liveManifestFilename = null;

    @Override
    public void onSegmentComplete(String path) {
        // nothing
    }

    @Override
    public void onManifestUpdated(String path) {
        if (manifestFilename == null)
            manifestFilename = new File(path);
    }

//    private Handler handler = new Handler();

    @Override
    public File onManifestRequested() {
        Log.i(TAG, "Manifest requested");

        if (manifestFilename != null)
            return manifestFilename;

        startRecording();

        /*
        Log.i(TAG, "Delayed stop scheduled");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "!!! Stopping recording (delayed)");
                stopRecording();
                stopHLSServer();
            }
        }, 30 * 1000); // 30 sec
        */

        // wait for manifest file created and return it's filename
        Log.i(TAG, "Waiting for manifest created ... ");
        while (manifestFilename == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        }

        Log.i(TAG, "Returning manifest");
        return manifestFilename;
    }

    @Override
    public void onSegmentDownloadStart(String url) {
        Log.i(TAG, "segment download started " + url);
    }

    @Override
    public void onSegmentDownloadFinished(String url) {
        Log.i(TAG, "download finished");
    }

    private static final int HLS_PORT = 8080;

    private void startHLSServer() {
        try {
            hlsServer = new HLSServer(HLS_PORT, outputPath);
            hlsServer.setListener(HWRecorderActivity.this);
        } catch (IOException e) {
            hlsServer = null;
            Log.e(TAG, "Failed to start HLS server", e);
        }
    }

    private void startSettingsServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (settingsServer == null) {
                    settingsServer = new HttpServer(HWRecorderActivity.this, 4000);
                    settingsServer.registerHandler(new SettingsRequestHandler(settings, HWRecorderActivity.this)); // settings
                    settingsServer.registerHandler(new ControlRequestHandler(HWRecorderActivity.this)); // start/stop
                    try {
                        settingsServer.start();
                        Log.i(TAG, "Settings server started");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void stopSettingsServer() {
        if (settingsServer == null)
            return;

        settingsServer.stop();
    }

    private void stopHLSServer() {
        if (hlsServer == null)
            return;

        Log.i(TAG, "Stopping HLS server");
        hlsServer.stop();
        hlsServer = null;
    }

    @Override
    public void onPause(){
        super.onPause();
        //glSurfaceView.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        //glSurfaceView.onResume();
    }
    
    @Override
    protected void onDestroy() {
      // Unregister since the activity is about to be closed.
      LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

      if (hlsServer != null)
          stopHLSServer();

      if (settingsServer != null)
          stopSettingsServer();

        if (motorolaApiServer != null)
            motorolaApiServer.stop();

      if (recording)
          stopRecording();

      super.onDestroy();
    }

    public static final int VIDEO_STREAMS_COUNT = 2;
    private final boolean RECORD_AUDIO_STREAM = false;

    private final int SOCKET_PORT = 2000;

    private CpuMonitor cpuMonitor;

    public void startRecording() {
        try {

            // delete existing files in outputPath

            if (outputPath == null) { // in case of 2 cam
                File folder = getBaseOutputFolder();
                initPaths(folder);
                hlsServer.setWwwroot(folder);
            }

            /*
            Log.d(TAG, "Cleaning existing files in " + outputPath);
            String[] files = outputPath.list();
            for (String eachFile : files) {
                File file = new File(outputPath, eachFile);
                Log.d(TAG, "Deleting " + file.getAbsolutePath());
                file.delete();
            }
            */

            liveRecorder = new LiveHLSRecorder(getApplicationContext(), settings, VIDEO_STREAMS_COUNT, RECORD_AUDIO_STREAM);
            liveRecorder.setExternalCallback(new HLSFileObserver.HLSCallback() {
                @Override
                public void onSegmentComplete(String path) {
                    Calendar finished = GregorianCalendar.getInstance();
                    Calendar started = (Calendar) finished.clone();
                    started.add(Calendar.SECOND, -settings.getHlsSegmentLength());

                    Log.i(CameraHttpServer.TAG, "segment added " + path);

                    motorolaApiServer.getControlService().notifySegment(started.getTime(), finished.getTime(), path);
                }

                @Override
                public void onManifestUpdated(String path) {

                }
            });

            if (VIDEO_STREAMS_COUNT == 1)
                liveRecorder.setStreamer(new TcpStreamer(SOCKET_PORT)); // streaming to socket (1 camera)
            else
                liveRecorder.setStreamer(new TsStreamer(SOCKET_PORT, VIDEO_STREAMS_COUNT, null)); // streaming to socket // (2 cameras)

            liveRecorder.startRecording(outputPath.getAbsolutePath());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    button.setText("Stop Recording");
                }
            });

            recording = true;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void stopRecording() {
        if (!recording)
            return;

        Log.i(TAG, "Stopped recording");

        liveRecorder.stopRecording();
        button.setText("Start Recording");
        liveIndicator.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_to_left));

        recording = false;
    }

    public void onRecordButtonClicked(View v){
        if (!recording){
        	broadcastUrl = null;
            startRecording();
        } else {
            stopRecording();
        }
    }
    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    	    // Get extra data included in the Intent
    		if (LiveHLSRecorder.HLS_STATUS.LIVE ==  (LiveHLSRecorder.HLS_STATUS) intent.getSerializableExtra("status")){
    			broadcastUrl = intent.getStringExtra("url");
    			liveIndicator.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_from_left));
            	liveIndicator.setVisibility(View.VISIBLE);
    		}  
    	  }
    };
}