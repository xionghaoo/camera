package xh.zero.xh_camera

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import timber.log.Timber
import xh.zero.camera.utils.CameraUtil
import xh.zero.xh_camera.databinding.ActivityCameraTestBinding

class CameraTestActivity : AppCompatActivity(), Camera2PreviewFragment.OnFragmentActionListener {

    private lateinit var binding: ActivityCameraTestBinding

//    private lateinit var cameraFragment: CameraXPreviewFragment
    private lateinit var cameraFragment: Camera2PreviewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraTestBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val cameraId = CameraUtil.selectCamera(this, 0)

        CameraUtil.adjustCameraArea(
            this,
            binding.fragmentContainer,
            cameraId
        ) { previewSize ->
            Timber.d("preview size: $previewSize")
//            cameraFragment = CameraXPreviewFragment.newInstance(cameraId)
            cameraFragment = Camera2PreviewFragment.newInstance(cameraId)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .commit()
        }
    }

    override fun showAnalysisResult(result: Bitmap?) {
        runOnUiThread {
            binding.imgResult.setImageBitmap(result)
        }
    }

    override fun showAnalysisText(txt: String) {
        
    }
}