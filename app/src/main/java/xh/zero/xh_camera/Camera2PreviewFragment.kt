package xh.zero.xh_camera

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.camera.Camera2Fragment
import xh.zero.camera.widgets.BaseSurfaceView
import xh.zero.xh_camera.databinding.FragmentCamera2PreviewBinding

class Camera2PreviewFragment: Camera2Fragment<FragmentCamera2PreviewBinding>() {
    override fun getBindingView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCamera2PreviewBinding {
        return FragmentCamera2PreviewBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView {
        return binding.viewfinder
    }

    override fun onAnalysisImage(bitmap: Bitmap) {

    }

    override fun onOpened() {

    }

    override fun onError(e: String?) {

    }

    companion object {
        fun newInstance(id: String) = Camera2PreviewFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CAMERA_ID, id)
            }
        }
    }
}