package xh.zero.camera.utils

import android.annotation.SuppressLint
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.LensFacingCameraFilter
import androidx.core.util.Preconditions

@SuppressLint("RestrictedApi")
class UsbCameraFilter(
    private val facing: Int,
    private val cameraId: String
) : LensFacingCameraFilter(facing) {

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> {
        val result: MutableList<CameraInfo> = ArrayList()
        for (cameraInfo in cameraInfos) {
            Preconditions.checkArgument(
                cameraInfo is CameraInfoInternal,
                "The camera info doesn't contain internal implementation."
            )

            val id = (cameraInfo as CameraInfoInternal).cameraId
            if (id == cameraId) {
                result.add(cameraInfo)
            }
        }

        return result
    }

}