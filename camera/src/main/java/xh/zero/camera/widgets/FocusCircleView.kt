package xh.zero.camera.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import xh.zero.camera.R

class FocusCircleView : FrameLayout {
    constructor(context: Context) : super(context) {

    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflate(context, R.layout.widget_focus_view, this)
    }

    fun show(x: Float, y: Float) {
        animFocusView(findViewById(R.id.focus_view), x, y, true)
        animFocusView(findViewById(R.id.focus_view_circle), x, y, false)
    }

    private fun animFocusView(v: View, focusX: Float, focusY: Float, isRing: Boolean) {
        v.visibility = View.VISIBLE
        v.x = focusX - v.width / 2
        v.y = focusY - v.height / 2

        // 圆环和圆饼是不同的View，因此得到的ViewPropertyAnimator是不同的
        val anim = v.animate()
        anim.cancel()

        if (isRing) {
            // 圆环 1.6 -> 1
            v.scaleX = 1.6f
            v.scaleY = 1.6f
            v.alpha = 1f
            anim.scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .withEndAction {
                    v.animate()
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction { v.visibility = View.INVISIBLE }
                        .start()
                }
                .start()
        } else {
            // 圆饼 0 -> 1
            v.scaleX = 0f
            v.scaleY = 0f
            v.alpha = 1f
            anim.scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .withEndAction {
                    v.animate()
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction { v.visibility = View.INVISIBLE }
                        .start()
                }
                .start()
        }
    }
}