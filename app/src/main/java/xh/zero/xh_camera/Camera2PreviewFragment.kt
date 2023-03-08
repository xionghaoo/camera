package xh.zero.xh_camera

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import xh.zero.camera.Camera2Fragment
import xh.zero.camera.widgets.BaseSurfaceView
import xh.zero.xh_camera.databinding.FragmentCamera2PreviewBinding

class Camera2PreviewFragment: Camera2Fragment<FragmentCamera2PreviewBinding>() {

    private var listener: OnFragmentActionListener? = null

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
    ): FragmentCamera2PreviewBinding {
        return FragmentCamera2PreviewBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView {
        return binding.viewfinder
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
        fun newInstance(id: String) = Camera2PreviewFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CAMERA_ID, id)
                putInt(ARG_CAPTURE_SIZE_WIDTH, 1200)
                putInt(ARG_CAPTURE_SIZE_HEIGHT, 1600)
                putBoolean(ARG_IS_ANALYSIS, true)
            }
        }
    }
}