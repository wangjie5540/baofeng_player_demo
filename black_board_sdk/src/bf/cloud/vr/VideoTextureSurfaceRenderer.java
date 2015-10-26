package bf.cloud.vr;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import bf.cloud.android.utils.MatrixUtil;
import bf.cloud.black_board_sdk.R;


public class VideoTextureSurfaceRenderer extends TextureSurfaceRenderer implements
        SurfaceTexture.OnFrameAvailableListener
{

    public static final String TAG = VideoTextureSurfaceRenderer.class.getSimpleName();
    
    // 模型旋转矩阵
    private float[] m4Rotate;
    // 摄像机矩阵
    private float[] m4lookAt;
    // 投影矩阵
    private float[] m4Perspective;

    private float rotationX = -45f;
    private float rotationY = 0f;
    private float distance = 0f;
    /**
     *
     */
    
    private static float[] squareCoords;

    private static short[] drawOrder;

    private Context context;

    private FloatBuffer textureBuffer;
    private float[] textureCoords;
    private int[] textures = new int[1];

    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private SurfaceTexture surfaceTexture;
    
    private float[] vertexTransform;
    private float[] videoTextureTransform;
    private boolean frameAvailable = false;
    float xx[] = {
    		0.8033332824707031f, 0, 0, 0, 
    		0, 1.4281480312347412f, 0, 0, 
    		0, 0, -1.000249981880188f, -1, 
    		0, 0, -0.20002500712871552f, 0};
    public VideoTextureSurfaceRenderer(Context context, SurfaceTexture texture, int width, int height, String agent, String url)
    {
        super(texture, width, height, agent, url);
        this.context = context;
        
        m4Rotate = new float[16];
        m4lookAt = new float[16];
        m4Rotate = new float[16];
        m4Perspective = new float[16];
        vertexTransform = new float[16];
        videoTextureTransform = new float[16];
        
        surfaceTexture = new SurfaceTexture(textures[0]);
        surfaceTexture.setOnFrameAvailableListener(this);
    }

    private void loadShaders()
    {
    	
    	
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.vetext_sharder);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(context, R.raw.fragment_sharder);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        shaderProgram = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"texture","vPosition","vTexCoordinate","textureTransform"});
    }

    private void setupVertexBuffer()
    {
     	textureCoords = Points.getUV();
     	squareCoords = Points.getXYZ();
    	drawOrder = Points.index;
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder. length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
    }

    private void setupTexture()
    {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");

        if (surfaceTexture == null) {
            surfaceTexture = new SurfaceTexture(textures[0]);
            surfaceTexture.setOnFrameAvailableListener(this);
        }
    }

    @Override
    protected boolean draw()
    {
        synchronized (this)
        {
            if (frameAvailable)
            {
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(videoTextureTransform);
                frameAvailable = false;
            }
            else
            {
                return false;
            }

        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glViewport(0, 0, width, height);
        drawTexture();

        return true;
    }

    private void drawTexture() {
        // Draw texture
        GLES20.glUseProgram(shaderProgram);
        int textureParamHandle = GLES20.glGetUniformLocation(shaderProgram, "texture");
        int textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int textureTranformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");
        int vertexTransformHandle = GLES20.glGetUniformLocation(shaderProgram, "vertexTransform");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE0, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        Matrix.setIdentityM(vertexTransform, 0);
        Matrix.setIdentityM(m4Rotate, 0);
        Matrix.setIdentityM(m4lookAt, 0);
        Matrix.setIdentityM(m4Perspective, 0);
        
    	// 旋转
        MatrixUtil.m4RotateX(m4Rotate, m4Rotate, (float)(rotationY * Math.PI /180f));
        MatrixUtil.m4RotateY(m4Rotate, m4Rotate, (float)(rotationX * Math.PI /180f));
        
        
    	// 摄像机
    	float[] eye = {0f, 0f, distance};
    	float[] center = {0f, 0f, 0f};
    	float[] up = {0f, 0f, 0f};
    	MatrixUtil.m4LookAt(m4lookAt, eye, center, up);
    	// 投影
    	Matrix.perspectiveM(m4Perspective, 0, 70f, (float)width/(float)height, 0.1f, 1000f);
    	// 合并所有矩阵
    	MatrixUtil.m4Multiply(vertexTransform, m4lookAt,  m4Perspective);
    	MatrixUtil.m4Multiply(vertexTransform, vertexTransform,  m4Rotate);
    	
        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);
//        GLES20.glUniformMatrix4fv(vertexTransformHandle, 1, false, xx, 0);
        GLES20.glUniformMatrix4fv(vertexTransformHandle, 1, false, vertexTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
    }


    @Override
    protected void initGLComponents()
    {
        setupVertexBuffer();
        setupTexture();
        loadShaders();
    }

    @Override
    protected void deinitGLComponents()
    {
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteProgram(shaderProgram);
        surfaceTexture.release();
        surfaceTexture.setOnFrameAvailableListener(null);
    }


    public void checkGlError(String op)
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    @Override
    public SurfaceTexture getSurfaceTexture()
    {
        return surfaceTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        synchronized (this)
        {
            frameAvailable = true;
        }
    }

	public float getRotationX() {
		return rotationX;
	}

	public void setRotationX(float rotationX) {
		
		System.out.println("setx:" + rotationX);
		rotationX %= 360f;
		if(rotationX >= 360f) rotationX -= 360f;
		if(rotationX < 0f) rotationX += 360f;
		System.out.println("setx-->:" + rotationX);
		this.rotationX = rotationX;
	}

	public float getRotationY() {
		return rotationY;
	}

	public void setRotationY(float rotationY) {
		rotationY = Math.min(rotationY, 90f);
		rotationY = Math.max(rotationY, -90f);
		this.rotationY = rotationY;
	}

	public float getDistance() {
		return distance;
	}

	public void setDistance(float distance) {
		this.distance = distance;
	}
}
