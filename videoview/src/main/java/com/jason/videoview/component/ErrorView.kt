package com.jason.videoview.component

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import kotlin.math.abs

class ErrorView(context: Context?) : LinearLayout(context), IControlComponent {
    private lateinit var wrapper: ControlWrapper
    private var mDownX = 0f
    private var mDownY = 0f

    private val ibNext: View
    private val statusBtn: View
    private val message: TextView

    init {
        visibility = GONE
        LayoutInflater.from(getContext()).inflate(R.layout.layout_player_error_view, this, true)
        message = findViewById(R.id.message)
        ibNext = findViewById<View>(R.id.btnNext).also {
            it.setOnClickListener {
                controller?.next()
            }
        }
        statusBtn = findViewById(R.id.status_btn)
        statusBtn.setOnClickListener {
            visibility = GONE
            wrapper.replay(false)
        }
        isClickable = true
    }

    override fun attach(controlWrapper: ControlWrapper) {
        wrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {

    }

    override fun onPlayStateChanged(playState: Int) {
        if (playState == VideoView.STATE_ERROR) {
            bringToFront()
            visibility = VISIBLE
            message.setText(R.string.player_error_message)
            ibNext.isVisible = controller?.hasNext() == true
            statusBtn.setOnClickListener {
                visibility = GONE
                wrapper.replay(false)
            }
        } else {
            visibility = GONE
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {

    }

    override fun setProgress(duration: Int, position: Int) {

    }

    override fun onLockStateChanged(isLocked: Boolean) {

    }

    override fun onTimedText(timedText: TimedText?) {

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = ev.x
                mDownY = ev.y
                // True if the child does not want the parent to intercept touch events.
                parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val absDeltaX = abs(ev.x - mDownX)
                val absDeltaY = abs(ev.y - mDownY)
                if (absDeltaX > ViewConfiguration.get(context).scaledTouchSlop || absDeltaY > ViewConfiguration.get(
                        context
                    ).scaledTouchSlop
                ) {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }

            MotionEvent.ACTION_UP -> {}
        }
        return super.dispatchTouchEvent(ev)
    }

    fun setErrorText(text: String, retry: () -> Unit) {
        message.text = text
        statusBtn.setOnClickListener {
            visibility = GONE
            retry.invoke()
        }
    }

    fun takeoverRetryLogic(block: () -> Unit) {
        statusBtn.setOnClickListener {
            block.invoke()
        }
    }


    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
    }
}