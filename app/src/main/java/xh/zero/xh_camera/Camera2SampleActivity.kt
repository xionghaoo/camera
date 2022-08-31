package xh.zero.xh_camera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.ViewGroup
import xh.zero.camera.BaseCameraActivity
import xh.zero.xh_camera.databinding.ActivityCamera2SampleBinding

class Camera2SampleActivity : BaseCameraActivity<ActivityCamera2SampleBinding>() {

    private lateinit var fragment: Camera2PreviewFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getBindingView(): ActivityCamera2SampleBinding = ActivityCamera2SampleBinding.inflate(layoutInflater)

    override fun getPreviewSize(): Size? = Size(400, 400)

    override fun getCameraFragmentLayout(): ViewGroup? = binding.fragmentContainer

    override fun selectCameraId(cameraIds: Array<String>): String = cameraIds[0]

    override fun onCameraAreaCreated(
        cameraId: String,
        previewArea: Size,
        screen: Size,
        supportImage: Size
    ) {
        fragment = Camera2PreviewFragment.newInstance(cameraId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}