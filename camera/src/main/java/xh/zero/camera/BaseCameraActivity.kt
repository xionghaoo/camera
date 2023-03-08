package xh.zero.camera

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import androidx.window.WindowProperties
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

typealias OnCameraIdSelectCallback = (String) -> Unit

abstract class BaseCameraActivity<V: ViewBinding> : AppCompatActivity() {

    companion object {
        private const val TAG = "BaseCameraActivity"
    }

    protected lateinit var binding: V
    private var isInit = true
    private lateinit var windowBounds: Rect

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getBindingView()
        setContentView(binding.root)
        windowBounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds

        Log.d(TAG, "window bounds: $windowBounds")


    }

    abstract fun getBindingView(): V

    abstract fun getPreviewSize(): Size?

    abstract fun getCameraFragmentLayout(): ViewGroup?

    abstract fun selectCameraId(cameraIds: Array<String>): String

    /**
     * cameraId: 摄像头id
     * area: 预览区域相对于屏幕尺寸的无畸变画面尺寸
     * screen: 屏幕尺寸
     * supportImage：相机支持的照片尺寸
     */
    abstract fun onCameraAreaCreated(cameraId: String, previewArea: Size, screen: Size, supportImage: Size)

    private fun getPreviewRect(): Rect? {
        val size = getPreviewSize()
        return if (size == null) null else Rect(0, 0, size.width, size.height)
    }

    private fun initialCameraArea() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = selectCameraId(cameraManager.cameraIdList)
        initialCameraPreviewSize(cameraId, cameraManager)
    }

    private fun initialCameraPreviewSize(cameraId: String, cameraManager: CameraManager) {
        val characteristic = cameraManager.getCameraCharacteristics(cameraId)

        // 打开第一个摄像头
        val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        configurationMap?.getOutputSizes(ImageFormat.JPEG)
            ?.maxByOrNull { it.height * it.width }
            ?.also { maxImageSize ->
                // Nexus6P相机支持的最大尺寸：4032x3024
                Timber.d("相机支持的最大尺寸：${maxImageSize}")

                val metrics = getPreviewRect() ?: windowBounds
                // Nexus6P屏幕尺寸：1440 x 2560，包含NavigationBar的高度
                Timber.d("屏幕尺寸：${metrics.width()} x ${metrics.height()}")
                val layout =  getCameraFragmentLayout()
                var previewAreaSize = Size(0, 0)
                if (layout != null) {
                    val lp = layout.layoutParams as ViewGroup.LayoutParams

                    Timber.d("屏幕方向: ${if (resources.configuration.orientation == 1) "竖直" else "水平"}")
                    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        // 竖直方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                        val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                        lp.width = metrics.width()
                        // Nexus6P 竖直方向屏幕计算高度
                        // 等比例关系：1440 / height = 3024 / 4032
                        // height = 4032 / 3024 * 1440
                        lp.height = (metrics.width() / ratio).toInt()
                    } else {
                        // 水平方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                        val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                        // Nexus6P 竖直方向屏幕计算高度
                        // 等比例关系：width / 1440 = 4032 / 3024
                        // width = 4032 / 3024 * 1440
                        lp.width = (metrics.height() / ratio).toInt()
                        lp.height = metrics.height()
                    }
                    previewAreaSize = Size(lp.width, lp.height)
                }
                onCameraAreaCreated(
                    cameraId,
                    previewAreaSize,
                    Size(metrics.width(), metrics.height()),
                    Size(maxImageSize.width, maxImageSize.height)
                )
            }

//        characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?.let { orientation ->
//            Timber.d("摄像头方向：${orientation}")
//        }
    }
}