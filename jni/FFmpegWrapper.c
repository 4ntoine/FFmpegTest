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

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <unistd.h> // for "access" function
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"

#define LOG_TAG "FFmpegWrapper"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Output
const char *outputPath;
const char *outputFormatName = "hls";
int hlsSegmentDurationSec = 10;
int *audioStreamIndex;
int *videoStreamIndex;

// Video
int VIDEO_PIX_FMT = PIX_FMT_YUV420P;
int VIDEO_CODEC_ID = CODEC_ID_H264;
int VIDEO_WIDTH = 1280;
int VIDEO_HEIGHT = 720;

// Audio
int AUDIO_CODEC_ID = CODEC_ID_AAC;
int AUDIO_SAMPLE_FMT = AV_SAMPLE_FMT_S16;
int AUDIO_SAMPLE_RATE = 44100;
int AUDIO_CHANNELS = 1;

AVFormatContext *outputFormatContext;
AVStream *audioStream;
AVStream *videoStream;
AVCodec *audioCodec;
AVCodec *videoCodec;
AVRational *videoSourceTimeBase;
AVRational *audioSourceTimeBase;

AVPacket *packet; // recycled across calls to writeAVPacketFromEncodedData

// Example h264 file: Used to configure AVFormatContext
const char *sampleFilePath = "/sdcard/sample.ts";

// Debugging
int *videoFrameCount;
int WRITE_RAW_FILE = 0;		// Write raw video packets to file

FILE *raw_video;

// FFmpeg Utilities

void init(){
    av_register_all();
    avformat_network_init();
    avcodec_register_all();

    if(WRITE_RAW_FILE){
    	raw_video = fopen("/sdcard/raw.h264", "w");
    }
}

char* stringForAVErrorNumber(int errorNumber){
    char *errorBuffer = malloc(sizeof(char) * AV_ERROR_MAX_STRING_SIZE);

    int strErrorResult = av_strerror(errorNumber, errorBuffer, AV_ERROR_MAX_STRING_SIZE);
    if (strErrorResult != 0) {
        LOGE("av_strerror error: %d", strErrorResult);
        return NULL;
    }
    return errorBuffer;
}

void addVideoStream(AVFormatContext *dest, int videoStreamsCount){
	AVCodecContext **c =  malloc(sizeof(AVCodecContext*) * videoStreamsCount);
	AVStream **st = malloc(sizeof(AVStream*) * videoStreamsCount);
	videoStreamIndex = malloc(sizeof(int) * videoStreamsCount);
	videoFrameCount = malloc(sizeof(int) * videoStreamsCount);
	AVCodec *codec;

	/* find the video encoder */
	codec = avcodec_find_encoder(VIDEO_CODEC_ID);
	if (!codec) {
		LOGI("add_video_stream codec not found, as expected. No encoding necessary");
	}

    int i;
    for (i=0; i<videoStreamsCount; i++) {
        st[i] = avformat_new_stream(dest, codec);
    	if (!st[i]) {
    		LOGE("add_video_stream could not alloc stream");
    	}
	    videoStreamIndex[i] = st[i]->index;
	    LOGI("addVideoStream at index %d", videoStreamIndex[i]);
	    c[i] = st[i]->codec;

	    avcodec_get_context_defaults3(c[i], codec);

        // context

        c[i]->codec_id = VIDEO_CODEC_ID;

        /* Put sample parameters. */
        // c->bit_rate = 400000;
        /* Resolution must be a multiple of two. */
        c[i]->width    = VIDEO_WIDTH;
        c[i]->height   = VIDEO_HEIGHT;

        /* timebase: This is the fundamental unit of time (in seconds) in terms
         * of which frame timestamps are represented. For fixed-fps content,
         * timebase should be 1/framerate and timestamp increments should be
         * identical to 1. */
        c[i]->time_base.den = 30;
        c[i]->time_base.num = 1;
        /*
        c->gop_size      = 12; // emit one intra frame every twelve frames at most
        */
        c[i]->pix_fmt       = VIDEO_PIX_FMT;

        /* Not encoding
        if(codec_id == CODEC_ID_H264){
                av_opt_set(c->priv_data, "preset", "ultrafast", 0);
                if(crf)
                        av_opt_set_double(c->priv_data, "crf", crf, 0);
                else
                        av_opt_set_double(c->priv_data, "crf", 24.0, 0);
        }
        */

        /* Some formats want stream headers to be separate. */
        if (dest->oformat->flags & AVFMT_GLOBALHEADER)
            c[i]->flags |= CODEC_FLAG_GLOBAL_HEADER;
	}


}

void addAudioStream(AVFormatContext *formatContext, int audioStreamsCount){
	AVCodecContext **codecContext =  malloc(sizeof(AVCodecContext*) * audioStreamsCount);
	AVStream **st = malloc(sizeof(AVStream*) * audioStreamsCount);
	audioStreamIndex = malloc(sizeof(int) * audioStreamsCount);
	AVCodec *codec;

	/* find the audio encoder */
	codec = avcodec_find_encoder(AUDIO_CODEC_ID);
	if (!codec) {
		LOGE("add_audio_stream codec not found");
	}

	int i;
	for (i=0; i<audioStreamsCount; i++) {

        //LOGI("add_audio_stream found codec_id: %d",codec_id);
        st[i] = avformat_new_stream(formatContext, codec);
        if (!st[i]) {
            LOGE("add_audio_stream could not alloc stream");
        }

        audioStreamIndex[i] = st[i]->index;

        //st->id = 1;
        codecContext[i] = st[i]->codec;
        avcodec_get_context_defaults3(codecContext[i], codec);
        codecContext[i]->strict_std_compliance = FF_COMPLIANCE_UNOFFICIAL; // for native aac support
        /* put sample parameters */
        //codecContext->sample_fmt  = AV_SAMPLE_FMT_FLT;
        codecContext[i]->sample_fmt  = AUDIO_SAMPLE_FMT;
        codecContext[i]->time_base.den = 44100;
        codecContext[i]->time_base.num = 1;
        //c->bit_rate    = bit_rate;
        codecContext[i]->sample_rate = AUDIO_SAMPLE_RATE;
        codecContext[i]->channels    = AUDIO_CHANNELS;

        LOGI("addAudioStream sample_rate %d index %d", codecContext[i]->sample_rate, st[i]->index);
        //LOGI("add_audio_stream parameters: sample_fmt: %d bit_rate: %d sample_rate: %d", codec_audio_sample_fmt, bit_rate, audio_sample_rate);
        // some formats want stream headers to be separate
        if (formatContext->oformat->flags & AVFMT_GLOBALHEADER)
            codecContext[i]->flags |= CODEC_FLAG_GLOBAL_HEADER;

    }
}

void copyAVFormatContext(AVFormatContext **dest, AVFormatContext **source){
    int numStreams = (*source)->nb_streams;
    LOGI("copyAVFormatContext source has %d streams", numStreams);
    int i;
    for (i = 0; i < numStreams; i++) {
        // Get input stream
        AVStream *inputStream = (*source)->streams[i];
        AVCodecContext *inputCodecContext = inputStream->codec;

        // Add new stream to output with codec from input stream
        //LOGI("Attempting to find encoder %s", avcodec_get_name(inputCodecContext->codec_id));
        AVCodec *outputCodec = avcodec_find_encoder(inputCodecContext->codec_id);
        if(outputCodec == NULL){
            LOGI("Unable to find encoder %s", avcodec_get_name(inputCodecContext->codec_id));
        }

			AVStream *outputStream = avformat_new_stream(*dest, outputCodec);
			AVCodecContext *outputCodecContext = outputStream->codec;

			// Copy input stream's codecContext for output stream's codecContext
			avcodec_copy_context(outputCodecContext, inputCodecContext);
			outputCodecContext->strict_std_compliance = FF_COMPLIANCE_UNOFFICIAL;

			LOGI("copyAVFormatContext Copied stream %d with codec %s sample_fmt %s", i, avcodec_get_name(inputCodecContext->codec_id), av_get_sample_fmt_name(inputCodecContext->sample_fmt));
    }
}

// FFInputFile functions
// Using these to deduce codec parameters from test file

AVFormatContext* avFormatContextForInputPath(const char *inputPath, const char *inputFormatString){
    // You can override the detected input format
    AVFormatContext *inputFormatContext = NULL;
    AVInputFormat *inputFormat = NULL;
    //AVDictionary *inputOptions = NULL;

    if (inputFormatString) {
        inputFormat = av_find_input_format(inputFormatString);
        LOGI("avFormatContextForInputPath got inputFormat from string");
    }
    LOGI("avFormatContextForInputPath post av_Find_input_format");
    // It's possible to send more options to the parser
    // av_dict_set(&inputOptions, "video_size", "640x480", 0);
    // av_dict_set(&inputOptions, "pixel_format", "rgb24", 0);
    // av_dict_free(&inputOptions); // Don't forget to free

    LOGI("avFormatContextForInputPath pre avformat_open_input path: %s format: %s", inputPath, inputFormatString);
    int openInputResult = avformat_open_input(&inputFormatContext, inputPath, inputFormat, /*&inputOptions*/ NULL);
    LOGI("avFormatContextForInputPath avformat_open_input result: %d", openInputResult);
    if (openInputResult != 0) {
        LOGE("avformat_open_input failed: %s", stringForAVErrorNumber(openInputResult));
        avformat_close_input(&inputFormatContext);
        return NULL;
    }

    int streamInfoResult = avformat_find_stream_info(inputFormatContext, NULL);
    LOGI("avFormatContextForInputPath avformat_find_stream_info result: %d", streamInfoResult);
    if (streamInfoResult < 0) {
        avformat_close_input(&inputFormatContext);
        LOGE("avformat_find_stream_info failed: %s", stringForAVErrorNumber(openInputResult));
        return NULL;
    }

    LOGI("avFormatContextForInputPath Complete!");
    LOGI("AVInputFormat %s Stream0 codec: %s Stream1 codec: %s", inputFormatContext->iformat->name, avcodec_get_name(inputFormatContext->streams[0]->codec->codec_id), avcodec_get_name(inputFormatContext->streams[1]->codec->codec_id) );
    LOGI("Stream0 time_base: (num: %d, den: %d)", inputFormatContext->streams[0]->codec->time_base.num, inputFormatContext->streams[0]->codec->time_base.den);
    LOGI("Stream1 time_base: (num: %d, den: %d)", inputFormatContext->streams[1]->codec->time_base.num, inputFormatContext->streams[1]->codec->time_base.den);
    return inputFormatContext;
}

// FFOutputFile functions

int file_exists(const char * path) {
    return access(path, F_OK) != -1 ? 1 : 0;
}

char *last_path;

AVFormatContext* avFormatContextForOutputPath(const char *path, const char *formatName){
    AVFormatContext *outputFormatContext;

    int len = strlen(path);
    last_path = malloc(len + 1);
    strcpy(last_path, path);
    last_path[len] = 0;

    LOGI("avFormatContextForOutputPath format=[%s] path=[%s]", formatName, path);
    int openOutputValue = avformat_alloc_output_context2(&outputFormatContext, NULL, formatName, path);
    if (openOutputValue < 0) {
        avformat_free_context(outputFormatContext);
        LOGE("failed to avformat_alloc_output_context2: %d", openOutputValue);
    } else {
        LOGI("file %s", file_exists(path) ? "exists" : "NOT EXISTS!");

        LOGI("avformat_alloc_output_context2() result is %d", openOutputValue);
    }
    return outputFormatContext;
}

int openFileForWriting(AVFormatContext *avfc, const char *path){
    if (!(avfc->oformat->flags & AVFMT_NOFILE)) {
        LOGI("Opening output file for writing at path %s", path);
        return avio_open(&avfc->pb, path, AVIO_FLAG_WRITE);
    }
    return 0;		// This format does not require a file
}

int writeFileHeader(AVFormatContext *avfc){
    AVDictionary *options = NULL;

    LOGI("file %s %s", last_path, file_exists(last_path) ? "exists" : "NOT EXISTS!");

    // Write header for output file
    LOGI("avformat_write_header()");
    int writeHeaderResult = avformat_write_header(avfc, &options);
    if (writeHeaderResult < 0) {
        LOGE("Error writing header: %d (%s)", writeHeaderResult, stringForAVErrorNumber(writeHeaderResult));

        usleep(10 * 1000); // 10 milliseconds

        LOGE("2nd write header attempt");
        writeHeaderResult = avformat_write_header(avfc, &options);
    }

    LOGI("Wrote file header: %d", writeHeaderResult);
    LOGI("file %s %s", last_path, file_exists(last_path) ? "exists" : "NOT EXISTS!");

    av_dict_free(&options);
    return writeHeaderResult;
}

int writeFileTrailer(AVFormatContext *avfc){
	if(WRITE_RAW_FILE){
		fclose(raw_video);
	}
    return av_write_trailer(avfc);
}

  /////////////////////
  //  JNI FUNCTIONS  //
  /////////////////////

/*
 * Prepares an AVFormatContext for output.
 * Currently, the output format and codecs are hardcoded in this file.
 */
void Java_com_example_ffmpegtest_recorder_FFmpegWrapper_prepareAVFormatContext(
    JNIEnv *env, jobject obj, jstring jOutputPath, jint audioStreamsCount, jint videoStreamsCount)
{
    init();

    // Create AVRational that expects timestamps in microseconds
    videoSourceTimeBase = av_malloc(sizeof(AVRational));
    videoSourceTimeBase->num = 1;
    videoSourceTimeBase->den = 1000000;

    audioSourceTimeBase = av_malloc(sizeof(AVRational));
	audioSourceTimeBase->num = 1;
	audioSourceTimeBase->den = 1000000;

    AVFormatContext *inputFormatContext;
    outputPath = (*env)->GetStringUTFChars(env, jOutputPath, NULL);

    LOGI("prepareAVFormat(path=%s, audio_streams_count=%i, video_streams_count=%i)",
        outputPath, audioStreamsCount, videoStreamsCount);

    outputFormatContext = avFormatContextForOutputPath(outputPath, outputFormatName);
    LOGI("post avFormatContextForOutputPath");

    //  For copying AVFormatContext from sample file:
    /*
    inputFormatContext = avFormatContextForInputPath(sampleFilePath, outputFormatName);
    LOGI("post avFormatContextForInputPath");
    copyAVFormatContext(&outputFormatContext, &inputFormatContext);
    LOGI("post copyAVFormatContext");
    */

    // For manually crafting AVFormatContext
    addVideoStream(outputFormatContext, videoStreamsCount);
    addAudioStream(outputFormatContext, audioStreamsCount);
    av_opt_set_int(outputFormatContext->priv_data, "hls_time", hlsSegmentDurationSec, 0);

    int result = openFileForWriting(outputFormatContext, outputPath);
    if(result < 0){
        LOGE("openFileForWriting error: %d", result);
    }

    writeFileHeader(outputFormatContext);
}

/*
 * Override default AV Options. Must be called before prepareAVFormatContext
 */

void Java_com_example_ffmpegtest_recorder_FFmpegWrapper_setAVOptions(JNIEnv *env, jobject obj, jobject jOpts){
	// 1: Get your Java object's "jclass"!
	jclass ClassAVOptions = (*env)->GetObjectClass(env, jOpts);

	// 2: Get Java object field ids using the jclasss and field name as **hardcoded** strings!
	jfieldID jVideoHeightId = (*env)->GetFieldID(env, ClassAVOptions, "videoHeight", "I");
	jfieldID jVideoWidthId = (*env)->GetFieldID(env, ClassAVOptions, "videoWidth", "I");

	jfieldID jAudioSampleRateId = (*env)->GetFieldID(env, ClassAVOptions, "audioSampleRate", "I");
	jfieldID jNumAudioChannelsId = (*env)->GetFieldID(env, ClassAVOptions, "numAudioChannels", "I");

	jfieldID jHlsSegmentDurationSec = (*env)->GetFieldID(env, ClassAVOptions, "hlsSegmentDurationSec", "I");

	// 3: Get the Java object field values with the field ids!

	// assuming both video streams have the same video params
	VIDEO_HEIGHT = (*env)->GetIntField(env, jOpts, jVideoHeightId);
	VIDEO_WIDTH = (*env)->GetIntField(env, jOpts, jVideoWidthId);

	AUDIO_SAMPLE_RATE = (*env)->GetIntField(env, jOpts, jAudioSampleRateId);
	AUDIO_CHANNELS = (*env)->GetIntField(env, jOpts, jNumAudioChannelsId);

	hlsSegmentDurationSec = (*env)->GetIntField(env, jOpts, jHlsSegmentDurationSec);

	LOGI("setAVOptions(width=%i, height=%i, audio_rate=%i, audio_channels=%i, duration=%i)",
	    VIDEO_WIDTH, VIDEO_HEIGHT, AUDIO_SAMPLE_RATE, AUDIO_CHANNELS, hlsSegmentDurationSec);

	// that's how easy love can be!
}

/*
 * Construct an AVPacket from MediaCodec output and call
 * av_interleaved_write_frame with our AVFormatContext
 */
void Java_com_example_ffmpegtest_recorder_FFmpegWrapper_writeAVPacketFromEncodedData(
    JNIEnv *env, jobject obj, jobject jData, jint jIsVideo, jint jstreamIndex, jint jOffset, jint jSize, jint jFlags, jlong jPts)
 {
    LOGI("Java_com_example_ffmpegtest_recorder_FFmpegWrapper_writeAVPacketFromEncodedData()");

    if(packet == NULL){
        packet = av_malloc(sizeof(AVPacket));
        LOGI("av_malloc packet");
    }

    if( ((int) jIsVideo) == JNI_TRUE ){
        videoFrameCount[jstreamIndex] = videoFrameCount[jstreamIndex] + 1;
    }

    // jData is a ByteBuffer managed by Android's MediaCodec.
    // Because the audio track of the resulting output mostly works, I'm inclined to rule out this data marshaling being an issue
    uint8_t *data = (*env)->GetDirectBufferAddress(env, jData);

    if( WRITE_RAW_FILE && ((int) jIsVideo) == JNI_TRUE ){
    	fwrite(data, sizeof(uint8_t), (int)jSize, raw_video);
    }

    if(((int) jSize ) < 15){
    	if( ((int) jIsVideo) == JNI_TRUE ){
    		//LOGI("video: %d data: %s size: %d videoPacket#: %d", (int) jIsVideo, (char*)data, (int) jSize, videoFrameCount);
    	}else{
    		//LOGI("video: %d data: %s size: %d", (int) jIsVideo, data, (int) jSize);
    	}
    	//return;
    }

    LOGI("av_init_packet()");
    av_init_packet(packet);

	if( ((int) jIsVideo) == JNI_TRUE){
		packet->stream_index = jstreamIndex;// jVideoStreamIndex;
	}

    LOGI("writePacket(buffer = %p, isVideo = %d, stream_index = %d, offset = %d, size = %d, flags = %d, pts = %ld )",
        data, (int)jIsVideo, (int)jstreamIndex, (int)jOffset, (int)jSize, (int)jFlags, (long)jPts);

    if (jSize < 100) {
        int i;
        for (i=0; i<jSize; i++) {
            LOGI("buffer[%d] = %d", i, data[i]);
        }
    }

    packet->size = (int) jSize;
    packet->data = data;
    packet->pts = (int) jPts;

    LOGI("av_rescale_q()");
    LOGI("av_rescale_q(%li, %p, stream_index=%i)", packet->pts, videoSourceTimeBase, packet->stream_index);
	packet->pts = av_rescale_q(packet->pts, *videoSourceTimeBase, (outputFormatContext->streams[packet->stream_index]->time_base));

	/* Use this to break on specific frame */
	//if(videoFrameCount == 3){
		//LOGI("break on frame");
		//LOGI("Payload size: %d", (int) jSize);
	//}

    LOGI("av_interleaved_write_frame()");
    int writeFrameResult = av_interleaved_write_frame(outputFormatContext, packet);
    if(writeFrameResult < 0){
        LOGE("av_interleaved_write_frame video: %d pkt: %d size: %d error: %s",
            ((int) jIsVideo), videoFrameCount[ (int) jstreamIndex ], ((int) jSize), stringForAVErrorNumber(writeFrameResult));
    }

    LOGI("av_free_packet()");
    av_free_packet(packet);
}

/*
 * Finalize file. Basically a wrapper around av_write_trailer
 */
void Java_com_example_ffmpegtest_recorder_FFmpegWrapper_finalizeAVFormatContext(JNIEnv *env, jobject obj){
    LOGI("finalizeAVFormatContext");

    LOGI("file %s %s", last_path, file_exists(last_path) ? "exists" : "NOT EXISTS!");

    int writeTrailerResult = writeFileTrailer(outputFormatContext);
    if(writeTrailerResult < 0){
        LOGE("av_write_trailer error: %d", writeTrailerResult);
    }

}
