package com.jason.cloud.drive.views.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet

class RoundImageView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {
    private val path = Path()
    private var cx: Float = 0f
    private var cy: Float = 0f
    private var radius: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        radius = w / 2f
    }

    override fun onDraw(canvas: Canvas) {
        path.reset()
        path.addCircle(cx, cy, radius, Path.Direction.CW)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }

}