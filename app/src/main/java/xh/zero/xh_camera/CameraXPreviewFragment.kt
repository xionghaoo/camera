package xh.zero.xh_camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.camera.CameraXFragment
import xh.zero.camera.widgets.BaseSurfaceView
import xh.zero.xh_camera.databinding.FragmentCameraXPreviewBinding

class CameraXPreviewFragment : CameraXFragment<FragmentCameraXPreviewBinding>() {

    override val cameraId: String by lazy { arguments?.getString("cameraId") ?: "0" }

    override fun getBindingView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraXPreviewBinding {
        return FragmentCameraXPreviewBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView = binding.viewfinder

    override fun onFocusTap(x: Float, y: Float) {
        binding.focusCricleView.show(x, y)
    }

    companion object {
        fun newInstance(id: String) = CameraXPreviewFragment().apply {
            arguments = Bundle().apply {
                putString("cameraId", id)
            }
        }
    }
}