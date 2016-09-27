LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := GlCameraPreviewRenderer
LOCAL_CFLAGS     := -Wall -Wextra
LOCAL_CPPFLAGS   := -std=c++11
LOCAL_SRC_FILES  := ../../common/GlCameraPreviewRenderer.cpp jni.cpp
LOCAL_LDLIBS     := -lGLESv2

include $(BUILD_SHARED_LIBRARY)
