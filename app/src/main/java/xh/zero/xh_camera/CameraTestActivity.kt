package xh.zero.xh_camera

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import xh.zero.camera.utils.CameraUtil
import xh.zero.xh_camera.databinding.ActivityCameraTestBinding

class CameraTestActivity : AppCompatActivity(), CameraXPreviewFragment.OnFragmentActionListener {

    private lateinit var binding: ActivityCameraTestBinding

    private lateinit var cameraFragment: CameraXPreviewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraTestBinding.inflate(layoutInflater)
        setContentView(binding.root)


        CameraUtil.adjustCameraArea(
            this,
            binding.fragmentContainer,
            0
        ) { cameraId ->
            cameraFragment = CameraXPreviewFragment.newInstance(cameraId)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .commit()
        }


    }

    override fun showAnalysisResult(result: Bitmap?) {

    }

    override fun showAnalysisText(txt: String) {
        
    }
}