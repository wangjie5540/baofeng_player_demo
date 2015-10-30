LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

SDL_PATH := ../sdl2
FFMPEG_PATH := ../ffmpeg

LOCAL_MODULE := ffmpeg
LOCAL_SRC_FILES := $(FFMPEG_PATH)/libs/libffmpeg.so
include $(PREBUILT_SHARED_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE := media_center
LOCAL_SRC_FILES := ../libmediacenter.so
include $(PREBUILT_SHARED_LIBRARY)
include $(CLEAR_VARS)

LOCAL_MODULE := player

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/$(SDL_PATH)/include \
	$(LOCAL_PATH)/$(FFMPEG_PATH)/include

# Add your application source files here...
LOCAL_SRC_FILES := \
	$(SDL_PATH)/src/main/android/SDL_android_main.c \
	player.cpp \
	native.cpp

LOCAL_CFLAGS += -DANDROID
LOCAL_CFLAGS += -D__STDC_CONSTANT_MACROS
LOCAL_CFLAGS += -O2

LOCAL_CPPFLAGS += -fpermissive

LOCAL_SHARED_LIBRARIES := ffmpeg SDL2

LOCAL_LDLIBS := -lGLESv1_CM -lGLESv2 -lz -llog

include $(BUILD_SHARED_LIBRARY)
