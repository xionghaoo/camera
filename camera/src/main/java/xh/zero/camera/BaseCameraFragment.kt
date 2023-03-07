package xh.zero.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.opengl.GLES20
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import xh.zero.camera.utils.StorageUtil
import xh.zero.camera.widgets.BaseSurfaceView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.min

typealias CaptureCallback = (path: String?) -> Unit

abstract class BaseCameraFragment<V: ViewBinding> : Fragment() {

    protected val binding: V get() = _binding!!
    private var _binding: V? = null
    protected lateinit var surfaceTexture: SurfaceTexture
    private var displayId: Int = -1

    protected val cameraId: String by lazy {
        arguments?.getString(ARG_CAMERA_ID)
            ?: throw IllegalArgumentException("camera id not found in arguments")
    }

    /**
     * 是否开启预览回调
     */
    protected val isAnalysis: Boolean by lazy {
        arguments?.getBoolean(ARG_IS_ANALYSIS) ?: false
    }

    protected lateinit var captureSize: Size

    protected val aspectRatio: Size by lazy {
        if (captureSize.width > captureSize.height) {
            val r = captureSize.width.toFloat() / captureSize.height
            if (abs(r - 4f / 3) < 0.01) {
                Size(4, 3)
            } else if (abs(r - 16f / 9) < 0.01) {
                Size(16, 9)
            } else {
                Size(captureSize.width, captureSize.height)
            }
        } else {
            val r = captureSize.height.toFloat() / captureSize.width
            if (abs(r - 3f / 4) < 0.01) {
                Size(3, 4)
            } else if (abs(r - 9f / 16) < 0.01) {
                Size(9, 16)
            } else {
                Size(captureSize.height, captureSize.width)
            }
        }
    }

    protected val cameraManager: CameraManager by lazy {
        requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val width = arguments?.getInt(ARG_CAPTURE_SIZE_WIDTH)
        val height = arguments?.getInt(ARG_CAPTURE_SIZE_HEIGHT)
        captureSize = if (width == null || height == null) {
            Log.d(TAG, "use default capture size: $DEFAULT_ANALYZE_IMAGE_WIDTH x $DEFAULT_ANALYZE_IMAGE_HEIGHT")
            Size(DEFAULT_ANALYZE_IMAGE_WIDTH, DEFAULT_ANALYZE_IMAGE_HEIGHT)
        } else {
            Size(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (_binding == null) {
            _binding = getBindingView(inflater, container)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sfv = getSurfaceView()
        // 获取SurfaceTexture, 相机的画面可以直接在上面渲染
        sfv.setOnSurfaceCreated { sfTexture ->
            surfaceTexture = sfTexture
            setSurfaceBufferSize(surfaceTexture)
            displayId = sfv.display.displayId
//            sfv.holder.setFixedSize(sfv.width, sfv.height)
            onSurfaceCreated()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected abstract fun onSurfaceCreated()

    abstract fun getBindingView(inflater: LayoutInflater, container: ViewGroup?): V

    abstract fun getSurfaceView(): BaseSurfaceView

    @WorkerThread
    abstract fun onAnalysisImage(bitmap: Bitmap)

    @MainThread
    abstract fun onOpened()

    @MainThread
    abstract fun onError(e: String?)

    /**
     * 拍照
     *
     * @param complete
     */
    abstract fun capture(complete: CaptureCallback)

    fun setCaptureSize(w: Int, h: Int) {
        captureSize = Size(w, h)
    }

    /**
     * 设置纹理缓冲区大小，用来接收相机输出的图像帧缓冲。
     * 相机的图像输出会根据设置的目标Surface来生成缓冲区
     * 如果相机输出的缓冲区和我们设置的Surface buffer size尺寸不一致，那么输出到Surface时的图像就会变形
     * 如果我们Surface buffer size的尺寸和SurfaceView的尺寸不一致，那么输出的图像也会变形
     */
    private fun setSurfaceBufferSize(surfaceTexture: SurfaceTexture) {
        val characteristic = cameraManager.getCameraCharacteristics(cameraId)
        val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        configurationMap?.getOutputSizes(ImageFormat.JPEG)
            ?.filter { size ->
                // 尺寸要求不大于 GL_MAX_VIEWPORT_DIMS and GL_MAX_TEXTURE_SIZE
                val limit = min(GLES20.GL_MAX_VIEWPORT_DIMS, GLES20.GL_MAX_TEXTURE_SIZE)
                val isFitGLSize = size.width <= limit && size.height <= limit
                // 不大于屏幕尺寸
                val screenW = requireContext().resources.displayMetrics.widthPixels
                val screenH = requireContext().resources.displayMetrics.heightPixels
                val screenMinSize = if (screenW < screenH) screenW else screenH
                val sizeMin = if (size.width < size.height) size.width else size.height
                val isFitScreenSize = sizeMin <= screenMinSize
                isFitGLSize && isFitScreenSize
            }
            ?.filter { size ->
                // 寻找4:3的预览尺寸比例
                abs(size.width.toFloat() / aspectRatio.width - size.height.toFloat() / aspectRatio.height) < 0.01f
            }
            ?.maxByOrNull { size -> size.height * size.width }
            ?.also { maxBufferSize ->
                surfaceTexture.setDefaultBufferSize(maxBufferSize.width, maxBufferSize.height)
                Log.d(TAG, "设置纹理缓冲区尺寸：${maxBufferSize}")
            }
    }

    companion object {
        const val TAG = "XHCamera"
        private const val FILENAME = "yyyyMMdd_HHmmssSSS"
        const val ARG_CAMERA_ID = "arg_camera_id"
        const val ARG_IS_ANALYSIS = "arg_is_analysis"
        const val ARG_CAPTURE_SIZE_WIDTH = "arg_capture_size_width"
        const val ARG_CAPTURE_SIZE_HEIGHT = "arg_capture_size_height"

        const val DEFAULT_ANALYZE_IMAGE_WIDTH = 480
        const val DEFAULT_ANALYZE_IMAGE_HEIGHT = 640

        fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat(FILENAME, Locale.US)
            val dir = StorageUtil.getDownloadDirectory(context.applicationContext, "xh_camera")
            return File(dir, "IMG_${sdf.format(Date())}.$extension")
        }
    }
}