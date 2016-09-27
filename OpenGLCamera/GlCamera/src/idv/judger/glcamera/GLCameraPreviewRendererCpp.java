package idv.judger.glcamera;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;

public class GLCameraPreviewRendererCpp extends GLCameraPreviewRenderer {

	public GLCameraPreviewRendererCpp(GLSurfaceView view, Camera camera) {
		super(view, camera);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mTexture = GlCameraPreviewRendererJniWrapper.on_surface_created();
		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);
		GlCameraPreviewRendererJniWrapper.on_surface_changed(width, height);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);
		mSurface.updateTexImage();
		GlCameraPreviewRendererJniWrapper.on_draw_frame();
	}
}
