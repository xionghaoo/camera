package xh.zero.camera.widgets

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent

typealias OnTextureCreated = (SurfaceTexture) -> Unit

abstract class BaseSurfaceView: GLSurfaceView {

    private var gestureDetector: GestureDetector? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    abstract fun setOnSurfaceCreated(callback: OnTextureCreated)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector?.onTouchEvent(event)
        return true
    }

    fun setOnGestureDetect(listener: GestureDetector.OnGestureListener) {
        gestureDetector = GestureDetector(context, listener)
    }
}