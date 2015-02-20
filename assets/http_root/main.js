function asyncRequest(path, onResponse, onError){
    var xmlhttp=window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
    xmlhttp.onreadystatechange=function(){
        if (xmlhttp.readyState==4 && xmlhttp.status==200){
            if (onResponse !== undefined)
                onResponse(xmlhttp.responseText)
        }
        else
            if (onError !== undefined)
                onError(xmlhttp.readyState, xmlhttp.status)
    }
    xmlhttp.open("GET",path,true);
    xmlhttp.send();
}

function onSetClick() { refresh(true); }

function onStartClick() { start(); }

function onStopClick() { stop(); }

function start() {
    asyncRequest("/control/start", function(str) {
        setTimeout(function() { refresh(false); }, 500);
    });
}

function stop() {
    asyncRequest("/control/stop", function(str) {
        setTimeout(function() { refresh(false); }, 500);
    });
}

function get(id) { return document.getElementById(id); }

var selectorWidth, selectorHeight, selectorFpsMin, selectorFpsMax, selectorIFrameInterval, selectorBitrate, selectorLength, selectorCount;
var btnStart, btnStop;

function refresh(apply){
    var suffix = "";
    if (apply) {
        var obj = {};
        obj.width = selectorWidth.value;
        obj.height = selectorHeight.value;
        obj.fps_min = selectorFpsMin.value;
        obj.fps_max = selectorFpsMax.value;
        obj.i_frame_interval = selectorIFrameInterval.value;
        obj.video_bitrate = selectorVideoBitrate.value;
        obj.hls_segment_length = selectorLength.value;
        obj.hls_segment_count = selectorCount.value;

        suffix = "?data=" + encodeURIComponent(JSON.stringify(obj));
    }

    asyncRequest("/settings" + suffix, function(str) {

        var obj = JSON.parse(str);

        selectorWidth.value = obj.width;
        selectorHeight.value = obj.height;
        selectorFpsMin.value = obj.fps_min;
        selectorFpsMax.value = obj.fps_max;
        selectorIFrameInterval.value = obj.i_frame_interval;
        selectorVideoBitrate.value = obj.video_bitrate;
        selectorLength.value = obj.hls_segment_length;
        selectorCount.value = obj.hls_segment_count;

        btnStart.disabled = obj.started;
        btnStop.disabled = !obj.started;
    });
}

function onLoad() {
    // bind controls
    selectorWidth = get('selectorWidth');
    selectorHeight = get('selectorHeight');
    selectorFpsMin = get('selectorFpsMin');
    selectorFpsMax = get('selectorFpsMax');
    selectorIFrameInterval = get('selectorIframeInterval');
    selectorVideoBitrate = get('selectorVideoBitrate');
    selectorLength = get('selectorLength');
    selectorCount = get('selectorCount');

    btnStart = get('btnStart');
    btnStop = get('btnStop');

    // just load without settings new settings
    refresh(false);
}
