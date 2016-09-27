#include "GlCameraPreviewRenderer.h"

#include <stdexcept>
#include <string>
#include <chrono>

#include "opengl_oswrapper.h"

#define GLM_FORCE_RADIANS
#include "glm/glm.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "glm/gtc/type_ptr.hpp"

class Renderer
{
private:
    glm::mat4 mModelMatrix;
    glm::mat4 mViewMatrix;
    glm::mat4 mProjectionMatrix;

    static const glm::vec3 smEye;
    static const glm::vec3 smCenter;
    static const glm::vec3 smUp;

    static const char* vertexShader;

    static const char* fragmentShader;

	/** This will be used to pass in the transformation matrix. */
	GLuint m_MVPMatrixHandle;

	/** This will be used to pass in model position information. */
	GLuint m_PositionHandle;

	/** This will be used to pass in model color information. */
	GLuint m_ColorHandle;
	GLuint m_Texture;
	GLuint m_TextureCoordHandle;
	static const float m_textureVertices[];
	static const float m_objVertices[];
	static const short drawOrder[];

	const int mBytesPerFloat = 4;
	const int mStrideBytes = 7 * mBytesPerFloat;
	const int mPositionDataSize = 3;
	const int mColorDataSize = 4;
public:
	Renderer()
		: mViewMatrix(glm::lookAt(smEye, smCenter, smUp))
		, m_Texture(GL_INVALID_VALUE)
	{
	    glClearColor(0.5f, 0.5f, 0.5f, 0.5f);

		// Prepare shaders and program
		GLuint vertexShaderHandle = glCreateShader(GL_VERTEX_SHADER);
		GLuint fragmentShaderHandle = glCreateShader(GL_FRAGMENT_SHADER);
		GLuint programHandle = glCreateProgram();

		try {
			if (vertexShaderHandle == 0)
				throw std::runtime_error("Error creating vertex shader");

			if (fragmentShaderHandle == 0)
				throw std::runtime_error("Error creating fragment shader.");

			if (programHandle == 0)
				throw std::runtime_error("Error creating program.");

			// // Load in the vertex shader.
			// Pass in the shader source.
			glShaderSource(vertexShaderHandle, 1, &vertexShader, NULL);

			// Compile the shader.
			glCompileShader(vertexShaderHandle);

			// Get the compilation status.
			// GL_COMPILE_STATUS: params returns GL_TRUE if the last compile operation on shader was successful, and GL_FALSE otherwise.
			GLint glStatus = GL_FALSE;
			glGetShaderiv(vertexShaderHandle, GL_COMPILE_STATUS, &glStatus);

			// If the compilation failed, delete the shader.
			if (glStatus == GL_FALSE)
				throw std::runtime_error("Error compiling vertex shader.");

			// // Load in the fragment shader.
			// Pass in the shader source.
			glShaderSource(fragmentShaderHandle, 1, &fragmentShader, NULL);

			// Compile the shader.
			glCompileShader(fragmentShaderHandle);

			// Get the compilation status.
			glStatus = GL_FALSE;
			glGetShaderiv(fragmentShaderHandle, GL_COMPILE_STATUS, &glStatus);

			// If the compilation failed, delete the shader.
			if (glStatus == GL_FALSE)
				throw std::runtime_error("Error compiling fragement shader.");

			// // Create a program object and store the handle to it.
			// Bind the vertex shader to the program.
			glAttachShader(programHandle, vertexShaderHandle);

			// Bind the fragment shader to the program.
			glAttachShader(programHandle, fragmentShaderHandle);

			// Bind attributes
			glBindAttribLocation(programHandle, 0, "a_Position");
			glBindAttribLocation(programHandle, 1, "a_Color");

			// Link the two shaders together into a program.
			glLinkProgram(programHandle);

			// Get the link status.
			glGetProgramiv(programHandle, GL_LINK_STATUS, &glStatus);

			// If the link failed, delete the program.
			if (glStatus == GL_FALSE)
				throw std::runtime_error("Error linking program.");
		} catch (std::exception e) {
			if (vertexShaderHandle != 0) {
				glDeleteShader(vertexShaderHandle);
				vertexShaderHandle = 0;
			}

			if (fragmentShaderHandle != 0) {
				glDeleteShader(fragmentShaderHandle);
				fragmentShaderHandle = 0;
			}

			if (programHandle != 0) {
				glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}

		// Set program handles. These will later be used to pass in values to
		// the program.
		m_MVPMatrixHandle	= glGetUniformLocation(programHandle, "u_MVPMatrix");
		m_PositionHandle		= glGetAttribLocation (programHandle, "a_Position");
		m_ColorHandle		= glGetAttribLocation (programHandle, "a_Color");

		m_TextureCoordHandle = glGetAttribLocation(programHandle, "inputTextureCoordinate");
		glEnableVertexAttribArray(m_TextureCoordHandle);
		glVertexAttribPointer(m_TextureCoordHandle, 2, GL_FLOAT, GL_FALSE, 2 * 4, m_textureVertices);

		// Tell OpenGL to use this program when rendering.
		glUseProgram(programHandle);

		// Prepare texture
		glGenTextures(1, &m_Texture);
		glBindTexture(GL_TEXTURE, m_Texture);
		glTexParameterf(GL_TEXTURE, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameterf(GL_TEXTURE, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	void setViewPort(int width, int height)
	{
		// Set the OpenGL viewport to the same size as the surface.
		glViewport(0, 0, width, height);

		// Create a new perspective projection matrix. The height will stay the
		// same
		// while the width will vary as per aspect ratio.
		const float ratio = (float) width / height;
		const float left = -ratio;
		const float right = ratio;
		const float bottom = -1.0f;
		const float top = 1.0f;
		const float near = 1.0f;
		const float far = 10.0f;

		// REF: http://www.lighthouse3d.com/tutorials/view-frustum-culling/
		mProjectionMatrix = glm::frustum(left, right, bottom, top, near, far);
	}

	void draw()
	{
	    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		//mSurface.updateTexImage();

		// Do a complete rotation every 10 seconds.
		long time = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::high_resolution_clock::now().time_since_epoch()).count() % 10000L;
		float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
		float shiftX = (time < 5000) ? ( (2.0f / 5000.0f) * ((int) time) - 1.0f ) : ((-4.0f / 10000.0f) * ((int) time) + 3.0f);

		mModelMatrix = glm::scale(glm::mat4(), glm::vec3(5.0f, 3.0f, 0.0f));
		drawFrame();

		// Draw the triangle facing straight on.
		mModelMatrix = glm::rotate(glm::translate(glm::mat4(), glm::vec3(0.0f, shiftX, 0.0f)), angleInDegrees, glm::vec3(0.5f, 1.0f, 2.0f));
		drawFrame();
	}

	int getTexture()
	{
		return m_Texture;
	}
private:
	void drawFrame()
	{
		// Pass in the position information
		glVertexAttribPointer(m_PositionHandle, mPositionDataSize, GL_FLOAT, GL_FALSE, mStrideBytes, m_objVertices);

		glEnableVertexAttribArray(m_PositionHandle);

		// Pass in the color information
		glVertexAttribPointer(m_ColorHandle, mColorDataSize, GL_FLOAT, GL_FALSE, mStrideBytes, m_objVertices + 3);

		glEnableVertexAttribArray(m_ColorHandle);

		// This multiplies the view matrix by the model matrix, and stores the
		// result in the MVP matrix
		// (which currently contains model * view).
		glm::mat4 MVPMatrix = mProjectionMatrix * (mViewMatrix * mModelMatrix);

		glUniformMatrix4fv(m_MVPMatrixHandle, 1, GL_FALSE, glm::value_ptr(MVPMatrix));
		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, drawOrder);
	}
};

const glm::vec3 Renderer::smEye = glm::vec3(0.0f, 0.0f, 1.5f);
const glm::vec3 Renderer::smCenter = glm::vec3(0.0f, 0.0f, -5.0f);
const glm::vec3 Renderer::smUp = glm::vec3(0.0f, 1.0f, 0.0f);

const char* Renderer::vertexShader =
		"uniform mat4 u_MVPMatrix;						\n"
		"attribute vec4 a_Position;						\n"
		"attribute vec4 a_Color;						\n"
		"attribute vec2 inputTextureCoordinate;			\n"
		"varying vec2 textureCoordinate;				\n"
		"varying vec4 v_Color;							\n"
		"void main()									\n"
		"{												\n"
		"   v_Color = a_Color;							\n"
		"   gl_Position = u_MVPMatrix					\n"
		"               * a_Position;					\n"
		"   textureCoordinate = inputTextureCoordinate;	\n"
		"}												\n";

const char* Renderer::fragmentShader =
		"#extension GL_OES_EGL_image_external : require					\n"
		"precision mediump float;										\n"
		"varying vec4 v_Color;											\n"
		"varying vec2 textureCoordinate;								\n"
		"uniform samplerExternalOES s_texture;							\n"
		"void main()													\n"
		"{																\n"
		"   gl_FragColor = texture2D( s_texture, textureCoordinate );	\n"
		//"   gl_FragColor = v_Color;										\n"
		"}																\n";

const float Renderer::m_textureVertices[] = { 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f };
const float Renderer::m_objVertices[] = {
		// X, Y, Z,
		// R, G, B, A
		-0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,

		-0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f,

		0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f,

		0.5f, 0.5f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, };

const short Renderer::drawOrder[] = { 0, 1, 2, 0, 2, 3 };

Renderer* renderer = NULL;

int on_surface_created() {
    renderer = new Renderer();
    return renderer->getTexture();
}

void on_surface_changed(int width, int height) {
    if (!renderer)
    	return;
    renderer->setViewPort(width, height);
}

void on_draw_frame() {
    if (!renderer)
    	return;
    renderer->draw();
}
