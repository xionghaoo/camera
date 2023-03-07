package xh.zero.xh_camera

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import timber.log.Timber
import xh.zero.camera.BaseCameraFragment
import xh.zero.camera.CameraXFragment
import xh.zero.camera.widgets.BaseSurfaceView
import xh.zero.xh_camera.databinding.FragmentCameraXPreviewBinding

class CameraXPreviewFragment : CameraXFragment<FragmentCameraXPreviewBinding>() {

    private var listener: OnFragmentActionListener? = null
    override val initialExposureIndex: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentActionListener) {
            listener = context
        } else {
            throw IllegalArgumentException("Activity must implement OnFragmentActionListener")
        }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

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

    override fun onAnalysisImage(bitmap: Bitmap) {
        listener?.showAnalysisResult(bitmap)
    }

    override fun onOpened() {

    }

    override fun onError(e: String?) {

    }

    interface OnFragmentActionListener {
        fun showAnalysisResult(result: Bitmap?)
        fun showAnalysisText(txt: String)
    }

    companion object {
        fun newInstance(id: String) = CameraXPreviewFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CAMERA_ID, id)
                putInt(ARG_CAPTURE_SIZE_WIDTH, 1600)
                putInt(ARG_CAPTURE_SIZE_HEIGHT, 1200)
            }
        }
    }
}