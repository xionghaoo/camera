package xh.zero.camera.render

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Size
import xh.zero.camera.R
import xh.zero.camera.widgets.BaseSurfaceView
import xh.zero.camera.widgets.OnTextureCreated

class CameraSurfaceView : BaseSurfaceView, CameraRenderer.OnViewSizeAvailableListener {

    private lateinit var renderer: CameraRenderer
    private lateinit var transformType: TransformType

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        var ta: TypedArray? = null

        try {
            ta = context.theme.obtainStyledAttributes(attrs, R.styleable.CameraSurfaceView, 0, 0)
            transformType = TransformType.values()[ta.getInt(R.styleable.CameraSurfaceView_csv_horizontalTransform, 0)]
        } finally {
            ta?.recycle()
        }

        setEGLContextClientVersion(2)
        renderer = CameraRenderer(context, transformType, this)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun getViewSize(): Size = Size(width, height)

    override fun setOnSurfaceCreated(callback: OnTextureCreated) {
        renderer.setOnSurfaceCreated(callback)
    }

}