package xh.zero.xh_camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceControl
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import timber.log.Timber
import xh.zero.camera.BaseCameraActivity
import xh.zero.xh_camera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.btn_camera_test -> {
                startActivity(Intent(this, CameraTestActivity::class.java))
            }
        }
    }
}