package xh.zero.camera

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLES20
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import timber.log.Timber
import xh.zero.camera.widgets.BaseSurfaceView
import kotlin.math.abs
import kotlin.math.min

abstract class BaseCameraFragment<V: ViewBinding> : Fragment() {
    protected lateinit var binding: V

    protected abstract val cameraId: String

    protected val cameraManager: CameraManager by lazy {
        requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getBindingView(inflater, container)
        return binding.root
    }

    abstract fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): V

    abstract fun getSurfaceView(): BaseSurfaceView

    /**
     * 设置纹理缓冲区大小，用来接收相机输出的图像帧缓冲。
     * 相机的图像输出会根据设置的目标Surface来生成缓冲区
     * 如果相机输出的缓冲区和我们设置的Surface buffer size尺寸不一致，那么输出到Surface时的图像就会变形
     * 如果我们Surface buffer size的尺寸和SurfaceView的尺寸不一致，那么输出的图像也会变形
     */
    protected fun setSurfaceBufferSize(surfaceTexture: SurfaceTexture) {
        val characteristic = cameraManager.getCameraCharacteristics(cameraId.toString())
        val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        configurationMap?.getOutputSizes(ImageFormat.JPEG)
            ?.filter { size ->
                // 尺寸要求不大于 GL_MAX_VIEWPORT_DIMS and GL_MAX_TEXTURE_SIZE
                val limit = min(GLES20.GL_MAX_VIEWPORT_DIMS, GLES20.GL_MAX_TEXTURE_SIZE)
                val isFitGLSize = size.width <= limit && size.height <= limit
                // 不大于屏幕尺寸
                val isPortrait = requireContext().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                val screenMinSize = if (isPortrait) {
                    requireContext().resources.displayMetrics.widthPixels
                } else {
                    requireContext().resources.displayMetrics.heightPixels
                }
                val isFitScreenSize = size.height <= screenMinSize
                isFitGLSize && isFitScreenSize
            }
            ?.filter { size ->
                // 寻找4:3的预览尺寸比例
                abs(size.width / 4f - size.height / 3f) < 0.01f
            }
            ?.maxByOrNull { size -> size.height * size.width }
            ?.also { maxBufferSize ->
                surfaceTexture.setDefaultBufferSize(maxBufferSize.width, maxBufferSize.height)
                Timber.d("纹理缓冲区尺寸：${maxBufferSize}")
            }
    }
}