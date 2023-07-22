package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils


@SuppressLint("SourceLockedOrientationActivity")
class CompleteView(context: Context) : FrameLayout(context), IControlComponent {
    private lateinit var wrapper: ControlWrapper
    private var stopFullscreen: View

    init {
        visibility = GONE
        isClickable = true
        LayoutInflater.from(getContext()).inflate(R.layout.layout_player_complete_view, this, true)
        findViewById<View>(R.id.iv_replay).setOnClickListener {
            wrapper.replay(true)
        }

        stopFullscreen = findViewById(R.id.stop_fullscreen)
        stopFullscreen.setOnClickListener {
            if (wrapper.isFullScreen) {
                PlayerUtils.scanForActivity(context)?.let {
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    wrapper.stopFullScreen()
                }
            }
        }
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
        if (playState == VideoView.STATE_PLAYBACK_COMPLETED) {
            visibility = VISIBLE
            stopFullscreen.visibility = if (wrapper.isFullScreen) VISIBLE else GONE
            bringToFront()
        } else {
            visibility = GONE
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCutout()
    }

    private fun applyCutout() {
        PlayerUtils.scanForActivity(context)?.let { activity ->
            val orientation = activity.requestedOrientation
            val cutoutHeight: Int = wrapper.cutoutHeight
            val layoutParams = stopFullscreen.layoutParams as LayoutParams
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                layoutParams.setMargins(0, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                layoutParams.setMargins(cutoutHeight, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                layoutParams.setMargins(0, 0, 0, 0)
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        if (playerState == VideoView.PLAYER_FULL_SCREEN) {
            stopFullscreen.visibility = VISIBLE
        } else if (playerState == VideoView.PLAYER_NORMAL) {
            stopFullscreen.visibility = GONE
        }
        applyCutout()
    }

    override fun setProgress(duration: Int, position: Int) {
    }

    override fun onLockStateChanged(isLocked: Boolean) {
    }

    override fun onTimedText(timedText: TimedText?) {

    }

    override fun setDataController(controller: MediaDataController) {
        findViewById<View>(R.id.iv_replay).setOnClickListener {
            controller.setIndex(0)
            controller.start()
        }
    }
}