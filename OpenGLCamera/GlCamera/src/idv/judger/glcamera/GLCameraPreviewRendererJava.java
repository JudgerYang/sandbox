package idv.judger.glcamera;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;

public class GLCameraPreviewRendererJava extends GLCameraPreviewRenderer {
	/** Store our model data in a float buffer. */
	private final FloatBuffer mTriangle1Vertices;
	// private final FloatBuffer mTriangle2Vertices = null;
	// private final FloatBuffer mTriangle3Vertices = null;

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix
	 * transforms world space to eye space; it positions things relative to our
	 * eye.
	 */
	private float[] mViewMatrix = new float[16];

	/**
	 * Store the projection matrix. This is used to project the scene onto a 2D
	 * viewport.
	 */
	private float[] mProjectionMatrix = new float[16];

	/**
	 * Store the model matrix. This matrix is used to move models from object
	 * space (where each model can be thought of being located at the center of
	 * the universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 * Allocate storage for the final combined matrix. This will be passed into
	 * the shader program.
	 */
	private float[] mMVPMatrix = new float[16];

	final String vertexShader =
			  "uniform mat4 u_MVPMatrix;						\n"
			+ "attribute vec4 a_Position;						\n"
			+ "attribute vec4 a_Color;							\n"
			+ "attribute vec2 inputTextureCoordinate;			\n"
			+ "varying vec2 textureCoordinate;					\n"
			+ "varying vec4 v_Color;							\n"
			+ "void main()										\n"
			+ "{												\n"
			+ "   v_Color = a_Color;							\n"
			+ "   gl_Position = u_MVPMatrix						\n"
			+ "               * a_Position;						\n"
			+ "   textureCoordinate = inputTextureCoordinate;	\n"
			+ "}												\n";

	final String fragmentShader =
			  "#extension GL_OES_EGL_image_external : require	\n"
			+ "precision mediump float;							\n"
			+ "varying vec4 v_Color;							\n"
			+ "varying vec2 textureCoordinate;					\n"
			+ "uniform samplerExternalOES s_texture;			\n"
			+ "void main()										\n"
			+ "{												\n"
			+ "   gl_FragColor = texture2D( s_texture, textureCoordinate );	\n"
			//+ "   gl_FragColor = v_Color;						\n"
			+ "}												\n";

	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;

	/** This will be used to pass in model position information. */
	private int mPositionHandle;

	/** This will be used to pass in model color information. */
	private int mColorHandle;

	/** How many elements per vertex. */
	private final int mStrideBytes = 7 * mBytesPerFloat;

	/** Offset of the position data. */
	private final int mPositionOffset = 0;

	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;

	/** Offset of the color data. */
	private final int mColorOffset = 3;

	/** Size of the color data in elements. */
	private final int mColorDataSize = 4;

	private ShortBuffer drawListBuffer;
	private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

	private FloatBuffer textureVerticesBuffer;
	private int mTextureCoordHandle;

	public GLCameraPreviewRendererJava(GLSurfaceView view, Camera camera) {
		super(view, camera);

		// This triangle is red, green, and blue.
		final float[] triangle1VerticesData = {
				// X, Y, Z,
				// R, G, B, A
				-0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,

				-0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f,

				0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,

				0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, };

		float textureVertices[] = { 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f,
				0.0f };

		// Initialize the buffers.
		mTriangle1Vertices = ByteBuffer
				.allocateDirect(triangle1VerticesData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();

		mTriangle1Vertices.put(triangle1VerticesData).position(0);

		ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
		dlb.order(ByteOrder.nativeOrder());
		drawListBuffer = dlb.asShortBuffer();
		drawListBuffer.put(drawOrder);
		drawListBuffer.position(0);

		ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
		bb2.order(ByteOrder.nativeOrder());
		textureVerticesBuffer = bb2.asFloatBuffer();
		textureVerticesBuffer.put(textureVertices);
		textureVerticesBuffer.position(0);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background clear color to gray.
		GLES20.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

		// Position the eye behind the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = 1.5f;

		// We are looking toward the distance
		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = -5.0f;

		// Set our up vector. This is where our head would be pointing were we
		// holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera
		// position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
		// of a model and
		// view matrix. In OpenGL 2, we can keep track of these matrices
		// separately if we choose.
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY,
				lookZ, upX, upY, upZ);

		// Prepare shaders and program
		int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		int fragmentShaderHandle = GLES20
				.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		int programHandle = GLES20.glCreateProgram();

		try {
			if (vertexShaderHandle == 0)
				throw new RuntimeException("Error creating vertex shader.");

			if (fragmentShaderHandle == 0)
				throw new RuntimeException("Error creating fragement shader.");

			if (programHandle == 0)
				throw new RuntimeException("Error creating program.");

			// // Load in the vertex shader.
			// Pass in the shader source.
			GLES20.glShaderSource(vertexShaderHandle, vertexShader);

			// Compile the shader.
			GLES20.glCompileShader(vertexShaderHandle);

			// Get the compilation status.
			final int[] glStatus = new int[1];
			GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS,
					glStatus, 0);

			// If the compilation failed, delete the shader.
			if (glStatus[0] == 0)
				throw new RuntimeException("Error compiling vertex shader.");

			// // Load in the fragment shader.
			// Pass in the shader source.
			GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

			// Compile the shader.
			GLES20.glCompileShader(fragmentShaderHandle);

			// Get the compilation status.
			GLES20.glGetShaderiv(fragmentShaderHandle,
					GLES20.GL_COMPILE_STATUS, glStatus, 0);

			// If the compilation failed, delete the shader.
			if (glStatus[0] == 0)
				throw new RuntimeException("Error compiling fragement shader.");

			// // Create a program object and store the handle to it.
			// Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);

			// Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			// Bind attributes
			GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
			GLES20.glBindAttribLocation(programHandle, 1, "a_Color");

			// Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);

			// Get the link status.
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS,
					glStatus, 0);

			// If the link failed, delete the program.
			if (glStatus[0] == 0)
				throw new RuntimeException("Error linking program.");
		} catch (Exception e) {
			if (vertexShaderHandle != 0) {
				GLES20.glDeleteShader(vertexShaderHandle);
				vertexShaderHandle = 0;
			}

			if (fragmentShaderHandle != 0) {
				GLES20.glDeleteShader(fragmentShaderHandle);
				fragmentShaderHandle = 0;
			}

			if (programHandle != 0) {
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}

		// Set program handles. These will later be used to pass in values to
		// the program.
		mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle,
				"u_MVPMatrix");
		mPositionHandle = GLES20.glGetAttribLocation(programHandle,
				"a_Position");
		mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");

		mTextureCoordHandle = GLES20.glGetAttribLocation(programHandle,
				"inputTextureCoordinate");
		GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
		GLES20.glVertexAttribPointer(mTextureCoordHandle, 2,
				GLES20.GL_FLOAT, false, 2 * 4, textureVerticesBuffer);
		
		// Tell OpenGL to use this program when rendering.
		GLES20.glUseProgram(programHandle);

		// Prepare texture
		int[] texture = new int[2];

		GLES20.glGenTextures(2, texture, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[1]);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		
		int cameraPreviewTextureHandle = GLES20.glGetUniformLocation(programHandle, "texture_cp");
		GLES20.glUniform1i(cameraPreviewTextureHandle, texture[0]);

		mTexture = texture[0];

		super.onSurfaceCreated(gl, config);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		super.onSurfaceChanged(gl, width, height);

		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);

		final int offset = 0;
		// Create a new perspective projection matrix. The height will stay the
		// same
		// while the width will vary as per aspect ratio.
		final float ratio = (float) width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 10.0f;

		// REF: http://www.lighthouse3d.com/tutorials/view-frustum-culling/
		Matrix.frustumM(mProjectionMatrix, offset, left, right, bottom, top,
				near, far);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		super.onDrawFrame(gl);

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		mSurface.updateTexImage();

		updateTextTexture();
		//GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		//GLES20.glBindTexture(GLES20.GL_TEXTURE0, mTexture);

		// GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
		// GLES20.GL_UNSIGNED_SHORT, mTexture);

		// Do a complete rotation every 10 seconds.
		long time = SystemClock.uptimeMillis() % 10000L;
		float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
		float shiftX = (time < 5000) ? ( (2.0f / 5000.0f) * ((int) time) - 1.0f ) : ((-4.0f / 10000.0f) * ((int) time) + 3.0f);



		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.scaleM(mModelMatrix, 0, 5.0f, 3.0f, 0.0f);
		drawTriangle(mTriangle1Vertices);

		// Draw the triangle facing straight on.
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, shiftX, 0.0f, 0.0f);
		Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.5f, 1.0f, 2.0f);
		drawTriangle(mTriangle1Vertices);
	}

	/**
	 * Draws a triangle from the given vertex data.
	 * 
	 * @param aTriangleBuffer
	 *            The buffer containing the vertex data.
	 */
	private void drawTriangle(final FloatBuffer aTriangleBuffer) {
		// Pass in the position information
		aTriangleBuffer.position(mPositionOffset);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize,
				GLES20.GL_FLOAT, false, mStrideBytes, aTriangleBuffer);

		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Pass in the color information
		aTriangleBuffer.position(mColorOffset);
		GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize,
				GLES20.GL_FLOAT, false, mStrideBytes, aTriangleBuffer);

		GLES20.glEnableVertexAttribArray(mColorHandle);

		// This multiplies the view matrix by the model matrix, and stores the
		// result in the MVP matrix
		// (which currently contains model * view).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

		// This multiplies the modelview matrix by the projection matrix, and
		// stores the result in the MVP matrix
		// (which now contains model * view * projection).
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		// GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
				GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
	}
	
	private void updateTextTexture() {
		Bitmap bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bmp);
		bmp.eraseColor(0);
		
		canvas.drawColor(0);
		
		Paint textPaint = new Paint();
		textPaint.setTextSize(12);
		textPaint.setAntiAlias(true);
		textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
		
		canvas.drawText("Hello", 10, 10, textPaint);
		
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);

		bmp.recycle();
	}
}















