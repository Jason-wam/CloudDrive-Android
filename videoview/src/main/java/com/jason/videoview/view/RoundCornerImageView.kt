package com.jason.videoview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.jason.videoview.R

open class RoundCornerImageView(context: Context, attrs: AttributeSet?) :
    AppCompatImageView(context, attrs) {
    private val rect = RectF()
    private val path = Path()
    private var lt = 0f
    private var rt = 0f
    private var lb = 0f
    private var rb = 0f

    private var radius: Float = 0f
    private var radii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)

    init {
        val typedArray =
            context.theme.obtainStyledAttributes(attrs, R.styleable.RoundCornerImageView, 0, 0)
        radius = typedArray.getDimension(R.styleable.RoundCornerImageView_radius, 0f)
        lt = typedArray.getDimension(R.styleable.RoundCornerImageView_radius_left_top, 0f)
        rt = typedArray.getDimension(R.styleable.RoundCornerImageView_radius_right_top, 0f)
        lb = typedArray.getDimension(R.styleable.RoundCornerImageView_radius_left_bottom, 0f)
        rb = typedArray.getDimension(R.styleable.RoundCornerImageView_radius_right_bottom, 0f)

        radii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
        if (radius == 0f) {
            radii = floatArrayOf(lt, lt, rt, rt, rb, rb, lb, lb)
        }
        typedArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        path.reset()
        path.addRoundRect(rect, radii, Path.Direction.CW)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}