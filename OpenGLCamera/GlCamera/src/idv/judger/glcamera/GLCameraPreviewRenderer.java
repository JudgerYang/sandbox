package idv.judger.glcamera;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.util.Log;

public class GLCameraPreviewRenderer implements GLSurfaceView.Renderer,
		SurfaceTexture.OnFrameAvailableListener {
	protected GLSurfaceView mView;
	protected Camera mCamera;
	protected int mTexture;
	protected SurfaceTexture mSurface;

	public GLCameraPreviewRenderer(GLSurfaceView view, Camera camera) {
		mView = view;
		mCamera = camera;
	}

	public int getTexture() {
		return mTexture;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		mSurface = new SurfaceTexture(mTexture);
		mSurface.setOnFrameAvailableListener(this);
        
        try {
            mCamera.setPreviewTexture(mSurface);
            mCamera.startPreview();
        } catch(Exception e) {
        	Log.e("GLCameraPreviewRenderer", "Camera Launch Fail: " + e);
        }
	}

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		mView.requestRender();
	}
}
