#include <jni.h>
#include <cstddef>
#include "assert.h"
#include "stdio.h"
#include "native_common_header.h"
#include "streamuploader.h"
#include "video_codec.h"
#include "audio_codec.h"

extern "C"{
#include "libavcodec/avcodec.h"
#include "live_stream_uploader.h"
#include "libswscale/swscale.h"
}

#include <vector>
#include <string>
using std::vector;
#ifdef __cplusplus
extern "C" {
#endif
#ifndef JNIREG_CLASS
#define JNIREG_CLASS "bf/cloud/android/datasource/UploadCenter"
#endif

#ifndef CALLBACK_CLASS
#define CALLBACK_CLASS "bf/cloud/android/datasource/UploadCenter$Listener"
#endif

/**********************************************************/
// Global Variables

const int MAX_BUFFER_SIZE = 1024*1024*10;

JavaVM *_jvm = NULL;
jclass _callback_class = NULL;
jmethodID _on_error_mid = NULL;

static video_encoder_t _video_encoder_handle = INVALID_VIDEO_CODEC;
static audio_encoder_t _audio_encoder_handle = INVALID_AUDIO_CODEC;

static int _video_width = 640;
static int _video_height = 480;
static int _video_rotate_degree = 0;

static vector<char> _video_out_buffer;
static vector<char> _video_convert_buffer;
static vector<char> _video_rotate_buffer;
static vector<char> _audio_out_buffer;
static vector<jobject> callback_object;

static bool _is_ready = false;

SwsContext *_convert_context = NULL;

static int registerNatives(JNIEnv*);
static JNINativeMethod gMethods[] = {
	{	"nativeInit", "(Ljava/lang/String;Lbf/cloud/android/datasource/UploadCenter$Listener;)V", (void*)init  },
	{	"nativeOpen", "(Ljava/lang/String;IIIIIIII)V", (void*)openStream  },
	{	"nativeUploadVideoSample", "([BIJ)V", (void*)uploadVideoSample  },
	{	"nativeUploadAudioSample", "([BIJ)V", (void*)uploadAudioSample  },
	{	"nativeClose", "()V", (void*)closeStream  },
	{	"nativeUninit", "()V", (void*)uninit  },
	{	"nativeSetRatateDegree", "(I)V", (void*)setRatateDegree  },
};
/**********************************************************/

///////////////////////////////////////////////////////////////////////////////
// class jni_callback_env

class jni_callback_env
{
public:
	jni_callback_env(JavaVM *jvm)
	{
		_jvm = jvm;
		_attached = false;
		_env = get_current_env();
	}

	~jni_callback_env()
	{
		release();
	}

	JNIEnv* env() { return _env; }

private:
	JNIEnv* get_current_env()
	{
		if (_jvm == NULL)
			return NULL;

		JNIEnv *result = NULL;
		bool attached = false;

		int status = _jvm->GetEnv((void**)&result, JNI_VERSION_1_4);
		if (status == JNI_EDETACHED)
		{
			status = _jvm->AttachCurrentThread(&result, NULL);
			attached = (status >= 0);
		}

		_attached = attached;
		return result;
	}

	void release()
	{
		if (_jvm != NULL && _attached)
			_jvm->DetachCurrentThread();
		_attached = false;
	}

private:
	JavaVM *_jvm;
	bool _attached;
	JNIEnv *_env;
};

///////////////////////////////////////////////////////////////////////////////
// Callbacks

static void liblsu_status_cb(LIBLSU_STATUS status, void *arg)
{
	jni_callback_env cb_env(_jvm);
	JNIEnv *env = cb_env.env();
	jclass clazz = env->GetObjectClass(callback_object[0]);
	jmethodID callback_methoid;
	switch (status){
	case LIBLSU_STATUS_OPENED:
		LOGD("ready!");
		_is_ready = true;
		callback_methoid = env->GetMethodID(clazz, "onReady", "()V");
		env->CallVoidMethod(callback_object[0], callback_methoid);
		break;
	case LIBLSU_STATUS_CLOSED:
		LOGD("closed!");
		_is_ready = false;
		callback_methoid = env->GetMethodID(clazz, "onClose", "()V");
		env->CallVoidMethod(callback_object[0], callback_methoid);
		break;

	case LIBLSU_STATUS_ERROR:
		{
			int error_code = liblsu_get_last_error();
			LOGD("error: %d", error_code);
			_is_ready = false;
			callback_methoid = env->GetMethodID(clazz, "onError", "(I)V");
			env->CallVoidMethod(callback_object[0], callback_methoid,error_code);
			break;
		}
	}
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env = NULL;
	_jvm = vm;
	jint result = -1;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
		return JNI_FALSE;
	assert(env != NULL);

	if (registerNatives(env) != JNI_OK)
	return JNI_FALSE;

	return JNI_VERSION_1_4;
}

static int registerNativeMethods(JNIEnv* env, const char* className,
		JNINativeMethod* gMethods, int numMethods) {
	jclass clazz = NULL;
	clazz = (env)->FindClass(className);
	if (clazz == NULL) {
		return JNI_FALSE;
	}

	if ((env)->RegisterNatives(clazz, gMethods, numMethods) < 0) {
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

static int registerNatives(JNIEnv* env) {
	if (registerNativeMethods(env, JNIREG_CLASS, gMethods,
					(int) sizeof(gMethods) / sizeof(JNINativeMethod)))
		return JNI_FALSE;
	return JNI_TRUE;
}



//native methods are below
JNIEXPORT void JNICALL init(JNIEnv* env, jclass clasz,
							jstring logDir, jobject cb_object)
{
	LOGD("init");
	callback_object.push_back(env->NewGlobalRef(cb_object));
	const char *log_dir_str = env->GetStringUTFChars(logDir, NULL);

	liblsu_init(
		MAX_BUFFER_SIZE,
		log_dir_str,
		liblsu_status_cb,
		NULL
		);

	env->ReleaseStringUTFChars(logDir, log_dir_str);
}

JNIEXPORT void JNICALL openStream(JNIEnv* env, jclass clasz,
					jstring channelId, jint videoWidth,
					jint videoHeight, jint videoFrameRate, jint videoDataRate,
					jint audioSampleRate, jint audioDataRate, jint audioChannelCount,
					jint audioSampleSize)
{
	LOGD("openStream");
	const char *channel_id_str = env->GetStringUTFChars(channelId, NULL);

//	_video_width = videoWidth;
//	_video_height = videoHeight;

	// create encoders
	if (_video_encoder_handle != INVALID_VIDEO_CODEC)
		destroy_video_encoder(_video_encoder_handle);
	LOGD("video encode width = %d, height = %d, dataRate = %d, frameRate = %d", videoWidth, videoHeight, videoDataRate, videoFrameRate);
	_video_encoder_handle = create_video_encoder(
		VIDEO_CODEC_H264, videoWidth, videoHeight, videoDataRate, videoFrameRate);
	if (_video_encoder_handle == INVALID_VIDEO_CODEC)
	{
		LOGD("create video encoder failed!");
		return;
	}
	if (_audio_encoder_handle != INVALID_VIDEO_CODEC)
		destroy_audio_encoder(_audio_encoder_handle);
	_audio_encoder_handle = create_audio_encoder(
		AUDIO_CODEC_AAC, audioSampleRate, audioChannelCount, audioSampleSize, audioDataRate);
	if (_audio_encoder_handle == INVALID_VIDEO_CODEC)
	{
		LOGD("create audio encoder failed!");
		return;
	}

	// open

	liblsu_video_meta_data vmd;
	liblsu_audio_meta_data amd;
	memset(&vmd, 0, sizeof(vmd));
	memset(&amd, 0, sizeof(amd));

	vmd.codec_type = LIBLSU_VCT_H264;
	vmd.width = videoWidth;
	vmd.height = videoHeight;
	vmd.frame_rate = videoFrameRate;
	vmd.data_rate = videoDataRate;
	vmd.extra_data_size = video_encoder_extradata_size(_video_encoder_handle);
	if (vmd.extra_data_size > 0)
	{
		vmd.extra_data = new char[vmd.extra_data_size];
		video_encoder_extradata(_video_encoder_handle, (uint8_t*)vmd.extra_data, vmd.extra_data_size);
	}

	amd.codec_type = LIBLSU_ACT_AAC;
	amd.sample_rate = audioSampleRate;
	amd.data_rate = audioDataRate;
	amd.channel_count = audioChannelCount;
	amd.sample_size = audioSampleRate;
	amd.extra_data_size = audio_encoder_extradata_size(_audio_encoder_handle);
	if (amd.extra_data_size > 0)
	{
		amd.extra_data = new char[amd.extra_data_size];
		audio_encoder_extradata(_audio_encoder_handle, (uint8_t*)amd.extra_data, amd.extra_data_size);
	}

	liblsu_open(
		channel_id_str,
		&amd,
		&vmd
		);

	delete[] vmd.extra_data;
	delete[] amd.extra_data;

	env->ReleaseStringUTFChars(channelId, channel_id_str);
}

static int yuv_rotate_90(char *des, char *src,int width,int height)
{
	int n = 0;
	int hw = width / 2;
	int hh = height / 2;
	//copy y
	for(int j = 0; j < width;j++)
	{
		for(int i = height - 1; i >= 0; i--)
		{
			des[n++] = src[width * i + j];
		}
	}

	//copy u
	char *ptemp = src + width * height;
	for(int j = 0;j < hw;j++)
	{
		for(int i = hh - 1;i >= 0;i--)
		{
			des[n++] = ptemp[ hw*i + j ];
		}
	}

	//copy v
	ptemp += width * height / 4;
	for(int j = 0; j < hw; j++)
	{
		for(int i = hh - 1;i >= 0;i--)
		{
			des[n++] = ptemp[hw*i + j];
		}
	}
	return n;
}

static int yuv_rotate_180(char *des,char *src,int width,int height)
{
	int n = 0;
	int hw = width / 2;
	int hh = height / 2;
	//copy y
	for(int j = height - 1; j >= 0; j--)
	{
		for(int i = width; i > 0; i--)
		{
			des[n++] = src[width*j + i];
		}
	}

	//copy u
	char *ptemp = src + width * height;
	for(int j = hh - 1;j >= 0; j--)
	{
		for(int i = hw; i > 0; i--)
		{
			des[n++] = ptemp[hw * j + i];
		}
	}

	//copy v
	ptemp += width * height / 4;
	for(int j = hh - 1;j >= 0; j--)
	{
		for(int i = hw; i > 0; i--)
		{
			des[n++] = ptemp[hw * j + i];
		}
	}
	return n;
}

static int yuv_rotate_270(char *des,char *src,int width,int height)
{
	int n = 0;
	int hw = width / 2;
	int hh = height / 2;
	//copy y
	for(int j = width; j > 0; j--)
	{
		for(int i = 0; i < height;i++)
		{
			des[n++] = src[width*i + j];
		}
	}

	//copy u
	char *ptemp = src + width * height;
	for(int j = hw; j > 0;j--)
	{
		for(int i = 0; i < hh;i++)
		{
			des[n++] = ptemp[hw * i + j];
		}
	}

	//copy v
	ptemp += width * height / 4;
	for(int j = hw; j > 0;j--)
	{
		for(int i = 0; i < hh;i++)
		{
			des[n++] = ptemp[hw * i + j];
		}
	}
	return n;
}

JNIEXPORT void JNICALL uploadVideoSample(JNIEnv* env, jclass clasz,
					jcharArray data, jint size,
					jlong timestamp)
{
	if (!_is_ready)
			return;

	jchar *data_ptr = env->GetCharArrayElements(data, NULL);
	video_frame_type_t video_frame_type = VIDEO_FRAME_UNKNOWN;

	if (_video_out_buffer.size() < size)
		_video_out_buffer.resize(size);

	int image_size = avpicture_get_size(AV_PIX_FMT_YUV420P, _video_width, _video_height);

	if (_video_convert_buffer.size() < image_size)
		_video_convert_buffer.resize(image_size);
	if (_video_rotate_buffer.size() < image_size)
		_video_rotate_buffer.resize(image_size);

	AVPicture in_pic, out_pic;
	avpicture_fill(&in_pic, (unsigned char*)data_ptr, AV_PIX_FMT_NV21, _video_width, _video_height);
	avpicture_fill(&out_pic, (unsigned char*)&_video_convert_buffer[0], AV_PIX_FMT_YUV420P, _video_width, _video_height);

	if (!_convert_context)
	{
		_convert_context = sws_getContext(
			_video_width, _video_height, AV_PIX_FMT_NV21,
			_video_width, _video_height, AV_PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);
	}

	sws_scale(_convert_context, (const uint8_t* const*)in_pic.data, in_pic.linesize, 0, _video_height,
			out_pic.data, out_pic.linesize);
	switch(_video_rotate_degree){
	case 0:
		break;
	case 90:
		yuv_rotate_90((char*)&_video_rotate_buffer[0], (char*)&_video_convert_buffer[0], _video_width, _video_height);
		break;
	case 180:
		yuv_rotate_180((char*)&_video_rotate_buffer[0], (char*)&_video_convert_buffer[0], _video_width, _video_height);
		break;
	case 270:
		yuv_rotate_270((char*)&_video_rotate_buffer[0], (char*)&_video_convert_buffer[0], _video_width, _video_height);
		break;
	default:
		;
	}
	int out_size;
	if (_video_rotate_degree == 0)
		out_size = video_encoder_encode(_video_encoder_handle, (unsigned char*)&_video_convert_buffer[0], image_size,
				(unsigned char*)&_video_out_buffer[0], _video_out_buffer.size(), &video_frame_type);
	else
		out_size = video_encoder_encode(_video_encoder_handle, (unsigned char*)&_video_rotate_buffer[0], image_size,
			(unsigned char*)&_video_out_buffer[0], _video_out_buffer.size(), &video_frame_type);
	bool is_key_frame = (video_frame_type == VIDEO_FRAME_I);

	liblsu_upload_video_frame(
		is_key_frame ? LIBLSU_FRAME_KEY : LIBLSU_FRAME_NON_KEY,
		timestamp,
		timestamp,
		(char*)&_video_out_buffer[0],
		out_size
		);

//	LOGD("write video. keyframe:%d, dts:%lld, size:%d", (int)is_key_frame, timestamp, out_size);

	env->ReleaseCharArrayElements(data, data_ptr, 0);
}

JNIEXPORT void JNICALL uploadAudioSample(JNIEnv* env, jclass clasz,
					jcharArray data, jint size,
					jlong timestamp)
{
//	LOGD("uploadAudioSample");
	if (!_is_ready)
			return;

	jchar *data_ptr = env->GetCharArrayElements(data, NULL);

	if (_audio_out_buffer.size() < size)
		_audio_out_buffer.resize(size);

	int out_size = audio_encoder_encode(_audio_encoder_handle, (unsigned char*)data_ptr, size,
		(unsigned char*)&_audio_out_buffer[0], _audio_out_buffer.size());

	liblsu_upload_audio_frame(timestamp, &_audio_out_buffer[0], out_size);

	//LOGD("write audio. size:%d", out_size);

	env->ReleaseCharArrayElements(data, data_ptr, 0);
}

JNIEXPORT void JNICALL closeStream(JNIEnv* env, jclass clasz)
{
	LOGD("closeStream");
	liblsu_close();
}

JNIEXPORT void JNICALL uninit(JNIEnv* env, jclass clasz)
{
	LOGD("uninit");

	if (_video_encoder_handle != INVALID_VIDEO_CODEC)
	{
		destroy_video_encoder(_video_encoder_handle);
		_video_encoder_handle = INVALID_VIDEO_CODEC;
	}

	if (_audio_encoder_handle != INVALID_VIDEO_CODEC)
	{
		destroy_audio_encoder(_audio_encoder_handle);
		_audio_encoder_handle = INVALID_VIDEO_CODEC;
	}

	liblsu_uninit();
	env->DeleteGlobalRef(callback_object[0]);
	callback_object.clear();
	_is_ready = false;
}

/**
 * 参数只能是0,90,180,270
 */
JNIEXPORT void JNICALL setRatateDegree(JNIEnv* env, jclass clazz, jint degree){
	LOGD("setRatateDegree");
	if (degree != 0 && degree != 90 && degree != 180 && degree != 270){
		LOGD("ratateDegree is invailid");
		return;
	}
	_video_rotate_degree = degree;
}


#ifdef __cplusplus
}
#endif
