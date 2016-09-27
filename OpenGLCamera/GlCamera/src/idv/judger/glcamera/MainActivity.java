package idv.judger.glcamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
	public static final int MODE_DIRECT = 1;
	public static final int MODE_OPENGL_CPP = 2;
	public static final int MODE_OPENGL_JAVA = 3;

	private Camera mCamera;
	private CameraPreview mCameraPreview;
	private int cameraId = 0;
	private int mode = MODE_DIRECT;

	private GLSurfaceView glSurfaceView;
	private boolean supportsEs2 = false;

	private SharedPreferences mPrefs;

	@TargetApi(17)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (checkCameraHardware(getApplicationContext()) == false) {
			Toast.makeText(getApplicationContext(),
					"This device doesn't have a camera.", Toast.LENGTH_LONG)
					.show();
		}

		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo configurationInfo = activityManager
				.getDeviceConfigurationInfo();

		supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
				|| isProbablyEmulator();

		mPrefs = getSharedPreferences("OpenGLCamera", MODE_PRIVATE);
		mode = mPrefs.getInt("camera_mode", MODE_DIRECT);
		cameraId = mPrefs.getInt("camera_id", 0);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (glSurfaceView != null) {
			glSurfaceView.onPause();
		}
		releaseCamera(); // release the camera immediately on pause event

		SharedPreferences.Editor ed = mPrefs.edit();
		ed.putInt("camera_mode", mode);
		ed.putInt("camera_id", cameraId);
		ed.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (glSurfaceView != null) {
			glSurfaceView.onResume();
		}
		getCameraInstance();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onButtonCaptureClick(View v) {
		if (mCamera == null)
			return;

		// get an image from the camera
		mCamera.takePicture(null, null, mPictureCallback);
	}

	public void onButtonSwitchCameraClick(View v) {
		int cameraCount = Camera.getNumberOfCameras();
		cameraId = ++cameraId % cameraCount;
		getCameraInstance();
	}

	public void onButtonDirectClick(View v) {
		mode = MODE_DIRECT;
		getCameraInstance();
	}

	public void onButtonOpenGLCppClick(View v) {
		mode = MODE_OPENGL_CPP;
		getCameraInstance();
	}

	public void onButtonOpenGLJavaClick(View v) {
		mode = MODE_OPENGL_JAVA;
		getCameraInstance();
	}

	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	private boolean isProbablyEmulator() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
				&& (Build.FINGERPRINT.startsWith("generic")
						|| Build.FINGERPRINT.startsWith("unknown")
						|| Build.MODEL.contains("google_sdk")
						|| Build.MODEL.contains("Emulator") || Build.MODEL
							.contains("Android SDK built for x86"));
	}

	private void getCameraInstance() {
		releaseCamera();

		try {
			mCamera = Camera.open(cameraId); // attempt to get a Camera instance
			if (mode == MODE_DIRECT) {
				if (glSurfaceView != null) {
					glSurfaceView = null;
				}
				mCameraPreview = new CameraPreview(this, mCamera, cameraId);
				FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
				preview.addView(mCameraPreview);
			} else {
				if (!supportsEs2) {
					throw new Exception("Not Support OpenGL ES 2.0");
				}

				glSurfaceView = new GLSurfaceView(this);
				if (isProbablyEmulator()) {
					// Avoids crashes on startup with some emulator images.
					glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
				}

				GLCameraPreviewRenderer renderer = null;
				if (mode == MODE_OPENGL_CPP) {
					renderer = new GLCameraPreviewRendererCpp(glSurfaceView,
							mCamera);
				} else if (mode == MODE_OPENGL_JAVA) {
					renderer = new GLCameraPreviewRendererJava(glSurfaceView,
							mCamera);
				} else {
					throw new Exception("Unknown Mode: " + mode);
				}

				glSurfaceView.setEGLContextClientVersion(2);
				glSurfaceView.setRenderer(renderer);

				FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
				preview.addView(glSurfaceView);
			}
		} catch (Exception e) {
			releaseCamera();
			Toast.makeText(getApplicationContext(),
					"Getting camera fail: " + e, Toast.LENGTH_LONG).show();
		}
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.removeAllViews();
	}

	private PictureCallback mPictureCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			if (pictureFile == null) {
				Log.d("Error",
						"Error creating media file, check storage permissions: ");
				return;
			}

			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {
				Log.d("Error", "File not found: " + e.getMessage());
			} catch (IOException e) {
				Log.d("Error", "Error accessing file: " + e.getMessage());
			}
		}
	};

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	/** Create a File for saving an image or video */
	private static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"MyCameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
				.format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else if (type == MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}
}
