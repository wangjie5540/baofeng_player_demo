#include <unistd.h>
#include <jni.h>
#include <android/log.h>

///////////////////////////////////////////////////////////////////////////////

#ifdef __ANDROID__

#ifdef __cplusplus
extern "C" {
#endif

#include "player.h"

///////////////////////////////////////////////////////////////////////////////

#define USE_LOG
#ifdef USE_LOG
  #define LOG_TAG "bfplayer"
  #define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#else
  #define LOGD(...)
#endif

///////////////////////////////////////////////////////////////////////////////

extern "C" void SDL_Android_Init(JNIEnv* env, jclass cls);
extern void Android_SetScreenResolution(int width, int height, uint32_t format);

///////////////////////////////////////////////////////////////////////////////
JNIEXPORT jboolean JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerInit(JNIEnv* env, jclass cls)
{
	LOGD("nativePlayerInit");
	bool result = player_init();
	if (result)
	{
		//LOGD("SDL_Android_Init");
		//SDL_Android_Init(env, cls);
	}
	return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerSetSource(JNIEnv* env, jclass cls, jstring url)
{
	const char *url_str = env->GetStringUTFChars(url, NULL);
	player_set_source(url_str);
	env->ReleaseStringUTFChars(url, url_str);
}

JNIEXPORT void JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerStart(JNIEnv* env, jclass cls)
{
	player_start();
}

JNIEXPORT void JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerPause(JNIEnv* env, jclass cls)
{
	player_pause();
}

JNIEXPORT void JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerStop(JNIEnv* env, jclass cls)
{
	player_stop();
}

JNIEXPORT void JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerSeekTo(JNIEnv* env, jclass cls, jint msec)
{
	player_seek_to(msec);
}

JNIEXPORT jint JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerGetDuration(JNIEnv* env, jclass cls)
{
	return player_get_duration();
}

JNIEXPORT jint JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerGetCurrentPosition(JNIEnv* env, jclass cls)
{
	return player_get_cur_pos();
}

JNIEXPORT jboolean JNICALL Java_bf_cloud_android_components_mediaplayer_proxy_MediaPlayerSw_nativePlayerIsPlaying(JNIEnv* env, jclass cls)
{
	return player_is_playing() ? JNI_TRUE : JNI_FALSE;
}

///////////////////////////////////////////////////////////////////////////////

#ifdef __cplusplus
}
#endif

#endif /* ANDROID */
