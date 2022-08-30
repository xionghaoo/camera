package xh.zero.camera.utils

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class OpenGLUtil {
    companion object {
        // 每个float 4byte
        fun createByteBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(data)
                    position(0)
                }
            }


        /**
         * 创建外部渲染纹理
         */
        fun createExternalTexture() : Int {
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
//            cameraRenderTextureId = textureIds[0]
            val textureId = textureIds[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            // 设置纹理的环绕方式
            // 环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT
            )
            // 在纹理放大和缩小时需要设置纹理的过滤方式，有两种最重要的：
            // GL_NEAREST: 邻近过滤，离纹理坐标最近的像素命中
            // GL_LINEAR: 线性过滤，根据坐标点附近的像素来计算一个颜色插值，离坐标点最近的颜色贡献最大
            // 过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
            return textureId
        }
    }
}