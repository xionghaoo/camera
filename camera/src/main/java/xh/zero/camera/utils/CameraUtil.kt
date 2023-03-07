package xh.zero.camera.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import android.view.ViewGroup
import androidx.window.WindowManager
import timber.log.Timber

class CameraUtil {
    companion object {

        fun adjustCameraArea(
            context: Context,
            cameraLayout: ViewGroup,
            cameraIndex: Int,
            cameraSize: Size? = null,
            callback: (cameraId: String) -> Unit
        ) {
            cameraLayout.post {
                val originWidth = cameraLayout.width
                val originHeight = cameraLayout.height
                val resources = context.resources
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = if (cameraManager.cameraIdList.size >= cameraIndex) {
                    cameraManager.cameraIdList[cameraIndex]
                } else throw IllegalStateException("camera id list size ${cameraManager.cameraIdList.size} < $cameraIndex")

                val characteristic = cameraManager.getCameraCharacteristics(cameraId)
                val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                configurationMap?.getOutputSizes(ImageFormat.JPEG)
                    ?.maxByOrNull { it.height * it.width }
                    ?.also { maxImageSize ->
                        // Nexus6P相机支持的最大尺寸：4032x3024
                        Timber.d("相机支持的最大尺寸：${maxImageSize}")
//                    val metrics = getPreviewRect() ?: WindowManager(context).getCurrentWindowMetrics().bounds
                        // Nexus6P屏幕尺寸：1440 x 2560，包含NavigationBar的高度
                        Timber.d("preview origin size：${originWidth} x $originHeight")
                        var previewAreaSize = Size(0, 0)
                        val lp = cameraLayout.layoutParams as ViewGroup.LayoutParams

                        Timber.d("屏幕方向: ${if (resources.configuration.orientation == 1) "竖直" else "水平"}")
                        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            // 竖直方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                            val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                            lp.width = originWidth
                            // Nexus6P 竖直方向屏幕计算高度
                            // 等比例关系：1440 / height = 3024 / 4032
                            // height = 4032 / 3024 * 1440
                            lp.height = (originWidth / ratio).toInt()
                        } else {
                            // 水平方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                            val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                            // Nexus6P 竖直方向屏幕计算高度
                            // 等比例关系：width / 1440 = 4032 / 3024
                            // width = 4032 / 3024 * 1440
                            lp.width = (originHeight / ratio).toInt()
                            lp.height = originHeight
                        }
                        previewAreaSize = Size(lp.width, lp.height)
                        callback(cameraId)
                    }
            }
        }
    }
}