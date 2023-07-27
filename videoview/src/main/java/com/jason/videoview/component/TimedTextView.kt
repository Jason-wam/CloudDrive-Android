package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.TextView
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.util.CenteredImageSpan
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.util.PlayerUtils

@SuppressLint("AppCompatCustomView")
class TimedTextView(context: Context) : TextView(context), IControlComponent {
    private lateinit var controlWrapper: ControlWrapper

    init {
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {

    }

    override fun onPlayStateChanged(playState: Int) {

    }

    override fun onPlayerStateChanged(playerState: Int) {

    }

    override fun setProgress(duration: Int, position: Int) {

    }

    override fun onLockStateChanged(isLocked: Boolean) {

    }

    override fun onTimedText(timedText: TimedText) {
        if (timedText.bitmap != null) {
            val drawable = BitmapDrawable(resources, timedText.bitmap)
            if (PlayerUtils.isInPortrait(context) && controlWrapper.isFullScreen) {
                if (timedText.bitmapHeight != -Float.MAX_VALUE) {
                    val videoSize = controlWrapper.videoSize
                    val newHeight = videoSize[1] / height.toFloat() * videoSize[0]
                    val bitmapHeight = timedText.bitmapHeight * newHeight
                    val scale = drawable.intrinsicWidth * (bitmapHeight / drawable.intrinsicHeight)
                    drawable.setBounds(0, 0, scale.toInt(), bitmapHeight.toInt())
                } else {
                    drawable.setBounds(0, 0, timedText.bitmap.width, timedText.bitmap.height)
                }
            } else {
                if (timedText.bitmapHeight != -Float.MAX_VALUE) {
                    val bitmapHeight = timedText.bitmapHeight * height
                    val scale = drawable.intrinsicWidth * (bitmapHeight / drawable.intrinsicHeight)
                    drawable.setBounds(0, 0, scale.toInt(), bitmapHeight.toInt())
                } else {
                    drawable.setBounds(0, 0, timedText.bitmap.width, timedText.bitmap.height)
                }
            }
            text = createSpannable(drawable)
        }
        if (timedText.text?.isNotBlank() == true) {
            text = timedText.text
        }
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 3000)
    }

    private fun createSpannable(drawable: Drawable): SpannableStringBuilder {
        val text = "bitmap"
        val spannableStringBuilder = SpannableStringBuilder(text)
        val span = CenteredImageSpan(drawable) //ImageSpan.ALIGN_BOTTOM);
        spannableStringBuilder.setSpan(span, 0, text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannableStringBuilder
    }

    private val handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = Runnable {
        text = ""
    }

    override fun setDataController(controller: MediaDataController) {

    }
}