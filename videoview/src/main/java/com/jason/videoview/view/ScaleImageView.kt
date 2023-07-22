package com.jason.videoview.view

import android.content.Context
import android.util.AttributeSet
import com.jason.videoview.R

class ScaleImageView(context: Context, attrs: AttributeSet?) :
    RoundCornerImageView(context, attrs) {
    private var scale = -1f
    private var baseOnWidth = true

    init {
        if (scale == -1f) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScaleImageView)
            if (typedArray.hasValue(R.styleable.ScaleImageView_scale_based_on_width)) {
                baseOnWidth =
                    typedArray.getBoolean(R.styleable.ScaleImageView_scale_based_on_width, true)
            }
            if (typedArray.hasValue(R.styleable.ScaleImageView_scale_w) && typedArray.hasValue(R.styleable.ScaleImageView_scale_h)) {
                val wScale = typedArray.getInt(R.styleable.ScaleImageView_scale_w, 1)
                val hScale = typedArray.getInt(R.styleable.ScaleImageView_scale_h, 1)
                scale = wScale / hScale.toFloat()
            }
            typedArray.recycle()
        }
    }

    fun setScale(w: Int, h: Int) {
        scale = w / h.toFloat()
        requestLayout()
    }

    /**
     * 设置是否保持宽度不变，缩放高度，反之高度不变
     */
    fun setBasedOnWidth(baseOnWidth: Boolean) {
        this.baseOnWidth = baseOnWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (baseOnWidth) {
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(widthSize, (widthSize / scale).toInt())
        } else {
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            setMeasuredDimension((heightSize * scale).toInt(), heightSize)
        }
    }
}