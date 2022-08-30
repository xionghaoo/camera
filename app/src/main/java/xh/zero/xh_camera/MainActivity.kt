package xh.zero.xh_camera

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import timber.log.Timber
import xh.zero.camera.BaseCameraActivity
import xh.zero.xh_camera.databinding.ActivityMainBinding

class MainActivity : BaseCameraActivity<ActivityMainBinding>(), CameraXPreviewFragment.OnFragmentActionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getBindingView(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun getPreviewRect(): Rect? {
        return Rect(0, 0, 400, 400)
    }

    override fun getCameraFragmentLayout(): ViewGroup? {
        return null
    }

    override fun selectCameraId(cameraIds: Array<String>): String {
        return cameraIds[0]
    }

    override fun onCameraAreaCreated(
        cameraId: String,
        previewArea: Size,
        screen: Size,
        supportImage: Size
    ) {
        val lp = binding.fragmentContainer.layoutParams as FrameLayout.LayoutParams
        lp.gravity = Gravity.END or Gravity.TOP

        Timber.d("preview: $previewArea, screen: $screen, supportImage: $supportImage")
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CameraXPreviewFragment.newInstance(cameraId))
            .commit()
    }

    override fun showAnalysisResult(result: Bitmap?) {
        Timber.d("showAnalysisResult: ${result?.width} x ${result?.height}")
        runOnUiThread {
            binding.imgResult.setImageBitmap(result)
        }
    }

    override fun showAnalysisText(txt: String) {

    }
}