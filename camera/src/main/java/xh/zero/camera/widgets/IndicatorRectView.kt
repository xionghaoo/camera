package xh.zero.camera.widgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import xh.zero.camera.R

class IndicatorRectView : View {

    companion object {
        private const val WRAP_WIDTH = 200
        private const val WRAP_HEIGHT = 200
        private const val STROKE_WIDTH = 2f
    }
    private lateinit var paint: Paint
    private lateinit var textPaint: Paint
    private var rect: Rect? = null

    private lateinit var rectTextPaint: RectTextPaint

    constructor(context: Context?) : super(context) {
        init()
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isDither = true
        paint.strokeWidth = STROKE_WIDTH
//        paint.strokeJoin = Paint.Join.ROUND
//        paint.strokeCap = Paint.Cap.ROUND
//        paint.pathEffect = CornerPathEffect(10f)
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE

        rectTextPaint = RectTextPaint(context)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(WRAP_WIDTH, WRAP_HEIGHT)
        } else if (widthMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(WRAP_WIDTH, heightSize)
        } else if (heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSize, WRAP_HEIGHT)
        } else {
            setMeasuredDimension(widthSize, heightSize)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        if (rect != null) {
            canvas?.drawRect(rect!!, paint)
            rectTextPaint.draw(rect!!, canvas)
        }
    }

    fun drawRect(rect: Rect) {
        this.rect = rect
        rectTextPaint.setRect(rect)
        postInvalidate()
    }

    class RectTextPaint(context: Context, textSize: Float = context.resources.getDimension(R.dimen.indicator_rect_text_size)) {
        private var leftTopPos: String = ""
        private var leftTopRect = Rect()
        private var leftBottomPos: String = ""
        private var leftBottomRect = Rect()
        private var rightTopPos: String = ""
        private var rightTopRect = Rect()
        private var rightBottomPos: String = ""
        private var rightBottomRect = Rect()

        private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            textPaint.textSize = textSize
//        textPaint.strokeWidth = STROKE_WIDTH
//        paint.strokeJoin = Paint.Join.ROUND
//        paint.strokeCap = Paint.Cap.ROUND
//        paint.pathEffect = CornerPathEffect(10f)
            textPaint.color = Color.RED
//        textPaint.style = Paint.Style.STROKE
        }

        fun setRect(rect: Rect) {
            leftTopPos = "(${rect.left}, ${rect.top})"
            leftBottomPos = "(${rect.left}, ${rect.bottom})"
            rightTopPos = "(${rect.right}, ${rect.top})"
            rightBottomPos = "(${rect.right}, ${rect.bottom})"
        }

        fun draw(rect: Rect, canvas: Canvas?) {
            textPaint.getTextBounds(leftTopPos, 0, leftTopPos.length, leftTopRect)
            canvas?.drawText(leftTopPos, rect.left.toFloat(), rect.top.toFloat() + leftTopRect.height(), textPaint)

            textPaint.getTextBounds(leftBottomPos, 0, leftBottomPos.length, leftBottomRect)
            canvas?.drawText(leftBottomPos, rect.left.toFloat(), rect.bottom.toFloat() - leftBottomRect.height(), textPaint)

            textPaint.getTextBounds(rightTopPos, 0, rightTopPos.length, rightTopRect)
            canvas?.drawText(rightTopPos, rect.right.toFloat() - rightTopRect.width(), rect.top.toFloat() + rightTopRect.height(), textPaint)

            textPaint.getTextBounds(rightBottomPos, 0, rightBottomPos.length, rightBottomRect)
            canvas?.drawText(rightBottomPos, rect.right.toFloat() - rightBottomRect.width(), rect.bottom.toFloat() - rightBottomRect.height(), textPaint)
        }
    }
}