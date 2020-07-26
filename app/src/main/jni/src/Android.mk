LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := Decorder2Show

FFMPEG_PATH := ../ffmpeg

LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(FFMPEG_PATH)/include
					


LOCAL_SRC_FILES := GL2JNILib.cpp \
                   empty.c

LOCAL_CFLAGS = -Werror -O3 -ffast-math  
LOCAL_SHARED_LIBRARIES = avcodec avdevice avfilter avformat avutil postproc swresample swscale

LOCAL_LDLIBS    += -llog -ldl -llog -lGLESv2  
include $(BUILD_SHARED_LIBRARY)
