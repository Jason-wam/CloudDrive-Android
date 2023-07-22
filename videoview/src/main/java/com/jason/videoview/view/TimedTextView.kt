package com.jason.videoview.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.jason.videoview.R
import xyz.doikki.videoplayer.model.TimedText

class TimedTextView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val tvTimedText: TextView
    private val ivTimedText: ImageView
    private var timedText: TimedText? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_player_subtitle_view, this)
        tvTimedText = findViewById(R.id.tvTimedText)
        ivTimedText = findViewById(R.id.ivTimedText)
    }

    fun updateTimeText(timedText: TimedText?) {
        this.timedText = timedText
        refresh()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        refresh()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        refresh()
    }

    fun refresh() {
        if (timedText == null) {
            tvTimedText.isVisible = false
            ivTimedText.isVisible = false
            return
        }

        tvTimedText.text = timedText!!.text
        tvTimedText.isVisible = timedText!!.text?.isNotBlank() == true

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            tvTimedText.textSize = 25f
        } else {
            tvTimedText.textSize = 15f
        }

        ivTimedText.isVisible = timedText!!.bitmap != null
        if (timedText!!.bitmap != null) {
            ivTimedText.setImageBitmap(timedText!!.bitmap)
            if (timedText!!.bitmapHeight != -Float.MAX_VALUE) {
                val bitmapHeight = timedText!!.bitmapHeight * height
                val layoutParams = ivTimedText.layoutParams
                layoutParams.height = bitmapHeight.toInt()
                layoutParams.width = LayoutParams.WRAP_CONTENT

                ivTimedText.adjustViewBounds = true
                ivTimedText.scaleType = ImageView.ScaleType.FIT_XY
                ivTimedText.layoutParams = layoutParams
            } else {
                ivTimedText.layoutParams.width = timedText!!.bitmap.width
                ivTimedText.layoutParams.height = timedText!!.bitmap.height
            }
        }
    }

}