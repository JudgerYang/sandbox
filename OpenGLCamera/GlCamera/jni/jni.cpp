#include "../../common/GlCameraPreviewRenderer.h"
#include <jni.h>
/* Header for class idv_judger_glcamera_GlCameraPreviewRendererJniWrapper */

#ifndef _Included_idv_judger_glcamera_GlCameraPreviewRendererJniWrapper
#define _Included_idv_judger_glcamera_GlCameraPreviewRendererJniWrapper
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     idv_judger_glcamera_GlCameraPreviewRendererJniWrapper
 * Method:    on_surface_created
 * Signature: ()V
 */
JNIEXPORT int JNICALL Java_idv_judger_glcamera_GlCameraPreviewRendererJniWrapper_on_1surface_1created
  (JNIEnv *, jclass)
{
	return on_surface_created();
}

/*
 * Class:     idv_judger_glcamera_GlCameraPreviewRendererJniWrapper
 * Method:    on_surface_changed
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_idv_judger_glcamera_GlCameraPreviewRendererJniWrapper_on_1surface_1changed
  (JNIEnv *, jclass, jint width, jint height)
{
	on_surface_changed(width, height);
}

/*
 * Class:     idv_judger_glcamera_GlCameraPreviewRendererJniWrapper
 * Method:    on_draw_frame
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_idv_judger_glcamera_GlCameraPreviewRendererJniWrapper_on_1draw_1frame
  (JNIEnv *, jclass)
{
	on_draw_frame();
}


#ifdef __cplusplus
}
#endif
#endif
