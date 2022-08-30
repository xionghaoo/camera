package xh.zero.camera.utils

import android.content.Context
import android.opengl.GLES20
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * 着色器程序
 */
class ShaderProgram(
    private val context: Context,
    private val vertexPath: String? = null,
    private val fragmentPath: String? = null,
    private val vertexShaderCode: String? = null,
    private val fragmentShaderCode: String? = null
) {

    companion object {
        private const val INVALID_PROGRAM_ID = -1
    }

    var ID: Int
        private set

    init {
        if ((vertexPath == null || fragmentPath == null)
            && (vertexShaderCode == null || fragmentShaderCode == null)) {
            throw IllegalArgumentException("Please enter file path or code string for ShaderProgram")
        }

        // 加载着色器代码
        val vShaderCode = if (vertexPath != null) {
            readAssetsJson(vertexPath, context)
        } else {
            vertexShaderCode
        }
        val fShaderCode = if (fragmentPath != null) {
            readAssetsJson(fragmentPath, context)
        } else {
            fragmentShaderCode
        }
        // 生成着色器程序，由顶点着色器和片段着色器组成
        ID = createProgram(vShaderCode, fShaderCode)
    }

    fun use() {
        GLES20.glUseProgram(ID)
    }

    fun setBool(name: String, value: Boolean) {
        GLES20.glUniform1i(GLES20.glGetUniformLocation(ID, name), if (value) 1 else 0)
    }

    fun setInt(name: String, value: Int) {
        GLES20.glUniform1i(GLES20.glGetUniformLocation(ID, name), value)
    }

    fun setFloat(name: String, value: Float) {
        GLES20.glUniform1f(GLES20.glGetUniformLocation(ID, name), value)
    }

    fun setMat4(name: String, matrix: FloatArray) {
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(ID, name), 1, false, matrix, 0)
    }

    fun destroy() {
        GLES20.glDeleteProgram(ID)
        ID = INVALID_PROGRAM_ID
    }

    fun getAttribute(name: String): Int {
        return GLES20.glGetAttribLocation(ID, name)
    }

    /**
     * 编译着色器代码
     */
    private fun compileShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            // 添加着色器和着色器代码，并编译
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            // 检查着色器编译结果
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] != GLES20.GL_TRUE) {
                Timber.e("Shader $shader compile failure: ${GLES20.glCompileShader(shader)}")
            } else {
                Timber.d("Shader $shader compile success")
            }

        }
    }

    /**
     * 生成着色器程序
     */
    private fun createProgram(vertexShaderCode: String?, fragmentShaderCode: String?) : Int {
        Timber.d("------ vertexShaderCode --------\n")
        Timber.d(vertexShaderCode)
        Timber.d("------ fragmentShaderCode --------\n")
        Timber.d(fragmentShaderCode)
        if (vertexShaderCode == null || fragmentShaderCode == null) {
            throw IllegalArgumentException("vertex code or fragment code is null")
        }
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)

            // 检查着色器程序链接结果
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Timber.e("Program $it link failure: ${GLES20.glGetProgramInfoLog(ID)}")
                GLES20.glDeleteProgram(it)
            } else {
                Timber.d("Program $it link success")
            }
        }
    }

    private fun readAssetsJson(file: String, context: Context): String {
        val result = StringBuffer()
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(InputStreamReader(context.assets.open(file)))
            var line = reader.readLine()
            while (line != null) {
                result.append(line).append("\n")
                line = reader.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                reader?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return result.toString()
    }
}