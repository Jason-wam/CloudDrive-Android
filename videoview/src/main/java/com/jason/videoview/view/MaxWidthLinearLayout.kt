package com.jason.videoview.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.jason.videoview.R

class MaxWidthLinearLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val maxWidth: Float

    init {
        val typedArray =
            context.theme.obtainStyledAttributes(attrs, R.styleable.MaxWidthLinearLayout, 0, 0)
        maxWidth = typedArray.getDimension(R.styleable.MaxWidthLinearLayout_maxWidth, -1f)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (maxWidth != -1f) {
            val size = MeasureSpec.getSize(widthMeasureSpec)
            val min = kotlin.math.min(size.toFloat(), maxWidth)
            val measureSpec = MeasureSpec.makeMeasureSpec(min.toInt(), MeasureSpec.AT_MOST)
            super.onMeasure(measureSpec, heightMeasureSpec)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}