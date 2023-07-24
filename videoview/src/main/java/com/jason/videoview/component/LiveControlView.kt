package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class LiveControlView(context: Context) : FrameLayout(context), IControlComponent {
    private lateinit var wrapper: ControlWrapper

    private val tvScale: TextView by lazy { findViewById(R.id.tvScale) }
    private val tvSelect: TextView by lazy { findViewById(R.id.tvSelect) }

    private val ibPlay: ImageButton by lazy { findViewById(R.id.ibPlay) }
    private val ibNext: ImageButton by lazy { findViewById(R.id.ibNext) }
    private val ibReload: ImageButton by lazy { findViewById(R.id.ibReload) }

    private val ibFullscreen: ImageButton by lazy { findViewById(R.id.ibFullscreen) }

    private val container: View by lazy { findViewById(R.id.container) }

    init {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.layout_player_live_control_view, this, true)

        ibNext.setOnClickListener {
            controller?.next()
        }

        ibPlay.setOnClickListener {
            wrapper.togglePlay()
        }

        ibReload.setOnClickListener {
            wrapper.replay(true)
        }

        ibFullscreen.setOnClickListener {
            if (wrapper.isFullScreen) {
                it.animate().rotationBy(-360f).setDuration(300).withEndAction {
                    toggleFullScreen()
                }
            } else {
                it.animate().rotationBy(360f).setDuration(300).withEndAction {
                    toggleFullScreen()
                }
            }
        }
    }

    override fun attach(wrapper: ControlWrapper) {
        this.wrapper = wrapper
    }

    override fun getView(): View {
        return this
    }

    @SuppressLint("SetTextI18n")
    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            if (visibility == GONE) {
                visibility = VISIBLE
                bringToFront()
                if (anim != null && wrapper.isFullScreen) {
                    startAnimation(anim)
                }
            }
        } else {
            if (visibility == VISIBLE) {
                visibility = GONE
                if (anim != null && wrapper.isFullScreen) {
                    startAnimation(anim)
                }
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED, VideoView.STATE_ERROR,
            VideoView.STATE_PREPARING, VideoView.STATE_PREPARED, VideoView.STATE_START_ABORT -> {
                visibility = GONE
            }

            VideoView.STATE_PLAYING -> {
                if (!ibPlay.isSelected) {
                    ibPlay.isSelected = true
                }
                //开始刷新进度
                wrapper.startProgress()

                if (wrapper.isFullScreen && (wrapper.videoSize[0] > 0 && wrapper.videoSize[1] > 0)) {
                    tvScale.alpha = 1f
                    tvScale.isEnabled = true
                } else {
                    tvScale.alpha = 0.5f
                    tvScale.isEnabled = false
                }
            }

            VideoView.STATE_PAUSED -> {
                if (ibPlay.isSelected) {
                    ibPlay.isSelected = false
                }
            }

            VideoView.STATE_BUFFERING -> {
                wrapper.stopProgress() //亲测必须停止进度刷新才不会导致UI卡顿，否则会阻塞UI
                if (ibPlay.isSelected) {
                    ibPlay.isSelected = false
                }
            }

            VideoView.STATE_BUFFERED -> {
                wrapper.startProgress()
                if (!ibPlay.isSelected) {
                    ibPlay.isSelected = true
                }
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        applyCutout()
        val isFullScreen = playerState == VideoView.PLAYER_FULL_SCREEN
        if (isFullScreen.not()) {
            ibNext.isVisible = false
        } else {
            ibNext.isVisible = true
            if (controller?.hasNext() == true) {
                ibNext.alpha = 1f
                ibNext.isEnabled = true
            } else {
                ibNext.alpha = 0.5f
                ibNext.isEnabled = false
            }
        }

        if (isFullScreen && (controller?.getData()?.size ?: 0) > 1) {
            tvSelect.alpha = 1f
            tvSelect.isEnabled = true
        } else {
            tvSelect.alpha = 0.5f
            tvSelect.isEnabled = false
        }
        if (isFullScreen && (wrapper.videoSize[0] > 0 && wrapper.videoSize[1] > 0)) {
            tvScale.alpha = 1f
            tvScale.isEnabled = true
        } else {
            tvScale.alpha = 0.5f
            tvScale.isEnabled = false
        }
        if (wrapper.isShowing && !wrapper.isLocked && isFullScreen) {
            isVisible = true
        }
    }

    private fun applyCutout() {
        PlayerUtils.scanForActivity(context)?.let { activity ->
            if (wrapper.hasCutout()) {
                val orientation = activity.requestedOrientation
                if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    container.setPadding(0, 0, 0, 0)
                } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    container.setPadding(wrapper.cutoutHeight, 0, 0, 0)
                } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    container.setPadding(0, 0, wrapper.cutoutHeight, 0)
                }
            }
        }
    }

    override fun setProgress(duration: Int, position: Int) {

    }

    override fun onLockStateChanged(isLocked: Boolean) {
        if (isLocked) {
            visibility = GONE
        } else {
            visibility = VISIBLE
            bringToFront()
        }
    }

    override fun onTimedText(timedText: TimedText?) {

    }

    /**
     * 横竖屏切换
     */
    private fun toggleFullScreen() {
        PlayerUtils.scanForActivity(context)?.also {
            wrapper.toggleFullScreen(it)
        }
    }

    fun getIbScale(): View = tvScale

    fun getIbSelect(): View = tvSelect

    fun getIbFullScreen(): View = ibFullscreen

    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
    }
}