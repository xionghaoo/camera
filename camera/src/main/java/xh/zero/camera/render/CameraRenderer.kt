package xh.zero.camera.render

import android.content.Context
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Size
import timber.log.Timber
import xh.zero.camera.utils.OpenGLUtil
import xh.zero.camera.utils.ShaderProgram
import xh.zero.camera.widgets.OnTextureCreated
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Nexus6P: 横屏选用 ROTATE
 */
enum class TransformType{
    NONE, ROTATE_AND_SCALE, ROTATE
}

class CameraRenderer(
    private val context: Context,
    private val transformHorizontal: TransformType,
    private var listener: OnViewSizeAvailableListener
) : GLSurfaceView.Renderer {

    /**
     * GLSL程序，GPU程序段
     * 顶点着色器
     */
    private val vertexShaderCode =
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aCoord;\n" +
        "varying vec2 vCoord;\n" +
        "uniform mat4 uMatrix;\n" +
        "uniform mat4 posMatrix;\n" +
        "void main() {\n" +
        "   gl_Position = posMatrix * aPosition;\n" +
        "   vCoord = (uMatrix * vec4(aCoord, 1.0, 1.0)).xy;\n" +
        "}\n"

    /**
     * GLSL程序，GPU程序段
     * 片段着色器
     */
    private val fragmentShaderCode =
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "varying vec2 vCoord;\n" +
        // 采样器，相机的输入数据先传递给此采样器
        "uniform samplerExternalOES uTexture;\n" +
        "void main() {\n" +
        "   gl_FragColor = texture2D(uTexture, vCoord);\n" +
        "}\n"

    private val vertexData = floatArrayOf(
        -1f, -1f, // bottomLeft
        1f, -1f, // bottomRight
        -1f, 1f, // topLeft
        1f, 1f // topRight
    )

    private val textureData = floatArrayOf(
        0f, 0f, // bottomLeft
        1f, 0f, // bottomRight
        0f, 1f, // topLeft
        1f, 1f // topRight
    )

    private lateinit var shaderProgram: ShaderProgram
    private val vertexBuffer = OpenGLUtil.createByteBuffer(vertexData)
    private val textureBuffer = OpenGLUtil.createByteBuffer(textureData)
    // 着色器默认使用的纹理就是0
    private var externalTextureID: Int = 0

    private var aPos: Int = -1
    private var aTextureCoord: Int = -1

    // 相机预览或者视频解码专用的Texture，提供给OpenGL处理
    private lateinit var surfaceTexture: SurfaceTexture
    private val textureMatrix = FloatArray(16)
    private val horizontalAdjustMatrix = FloatArray(16)

    private var onTextureCreated: OnTextureCreated? = null

    fun setOnSurfaceCreated(callback: OnTextureCreated) {
        this.onTextureCreated = callback
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        shaderProgram = ShaderProgram(context, vertexShaderCode = vertexShaderCode, fragmentShaderCode = fragmentShaderCode)

        aPos = shaderProgram.getAttribute("aPosition")
        aTextureCoord = shaderProgram.getAttribute("aCoord")

        // 根据摄像头本身的角度不同，这个旋转可能不需要
        initialTransformMatrix(transformHorizontal)

        externalTextureID = OpenGLUtil.createExternalTexture()
        // 创建一个接收相机预览的texture
        surfaceTexture = SurfaceTexture(externalTextureID)
        onTextureCreated?.invoke(surfaceTexture)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Timber.d("onSurfaceChanged: $width, $height")
        // 视口变换，属于OpenGL坐标系变换的最后一步
        // 在对观察者坐标进行裁剪以后，再进行正视投影或者透视投影得到OpenGL标准化坐标
        // ，再把OpenGL的标准化坐标转变为屏幕坐标，和屏幕上的像素点一一对应
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureMatrix)

//        GLES20.glClearColor(0f, 0f, 0f, 1f)
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        shaderProgram.use()

        // 给顶点赋值
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPos)
        // 给纹理坐标顶点赋值
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(aTextureCoord)

        // 纹理坐标矫正矩阵
        shaderProgram.setMat4("uMatrix", textureMatrix)

        // 顶点坐标矫正矩阵
        shaderProgram.setMat4("posMatrix", horizontalAdjustMatrix)

        // 激活外部纹理（这个步骤不是必须的，渲染的纹理默认会绑定到位置0）
//        GLES20.glActiveTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureID)
//        // 绑定外部纹理到glsl，设置uniform属性必须要先使用程序，因为它是在当前激活的着色器程序中使用
//        shaderProgram.setInt("uTexture", 0)

        // 以相同的方向绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTextureCoord)

    }

    /**
     * 摄像头预览方向矫正矩阵 - 仅限横屏情况
     * 作用：将预览画面逆时针旋转90度
     * 横屏状态下：
     * 1. 如果摄像头传感器的角度为0，则不需要这个矫正矩阵
     * 2. 如果摄像头传感器的角度为90度，则需要加上这个矫正矩阵
     *
     * 如果屏幕方向是水平方向，假设拍摄的画面是4:3，那么这个4:3的画面会放入一个竖直方向的顶点坐标中。
     * 正常如果不做矫正，水平方向看到的画面是旋转的90度的(顺时针)。原因是相机是以水平方向拍摄的画面，
     * 在放入竖直方向的纹理坐标时发生了变形，这个变形的画面又在水平方向上的SurfaceView上预览。
     *
     * 这里有两个地方需要矫正，旋转和形变
     * 1. 旋转矫正，需要乘以反方向的矩阵(逆时针矩阵)
     * 2. 形变矫正，需要乘以一个缩放矩阵(放大4/3倍)，因为是把宽度方向的尺寸放入了高度方向长度的容器
     *
     * 实际测试Nexus6p需要加上形变矫正矩阵，小米Pad则不需要加上形变矩阵，原因未知，因此这里的
     * 形变矩阵不是必须的
     *
     * needScale: 是否需要缩放矩阵
     * ignore: 是否忽略矫正行为
     */
    private fun initialTransformMatrix(type: TransformType) {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val isNeedScale = type == TransformType.ROTATE_AND_SCALE
        val ignoreTransform = type == TransformType.NONE
        if (isPortrait || ignoreTransform) {
            // 竖屏不需要形变
            Matrix.setIdentityM(horizontalAdjustMatrix, 0)
        } else {
            // 横屏，width > height
            // 缩放矩阵
            val scale: Float = listener.getViewSize().height.toFloat() / listener.getViewSize().width.toFloat()
            Timber.d("缩放比例：${scale}")
            val scaleMatrix = FloatArray(16)
            // 生成单位矩阵
            Matrix.setIdentityM(scaleMatrix, 0)
            // 由于缩放以后需要逆时针旋转90度，因此这里x和y的缩放方向是旋转前预览的水平和竖直方向，
            // 旋转前的预览画面是顺时针旋转90的，因此:
            // 缩放预览的宽度方向：修改y的值
            // 缩放预览的高度方向：修改x的值
            Matrix.scaleM(scaleMatrix, 0, if (isNeedScale) scale else 1f, 1f, 1f)

            val rotateMatrix = FloatArray(16)
            // 正常相机的后置镜头角度为顺时针90度
            // 修正矩阵：矩阵屏幕朝里方向顺时针旋转90度，相当于画面逆时针旋转90度
            Matrix.setRotateM(rotateMatrix, 0, 90f, 0f, 0f, 1f)
            // 矩阵乘顺序：从右到左，缩放 -> 旋转 -> 平移
            Matrix.multiplyMM(horizontalAdjustMatrix, 0, rotateMatrix, 0, scaleMatrix, 0)
        }
    }

    interface OnViewSizeAvailableListener {
        fun getViewSize() : Size
    }


}