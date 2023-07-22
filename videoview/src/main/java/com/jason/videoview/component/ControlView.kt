package com.jason.videoview.component

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class ControlView(context: Context) : FrameLayout(context), IControlComponent {
    private lateinit var wrapper: ControlWrapper

    private val seekBar: SeekBar by lazy { findViewById(R.id.seekBar) }
    private val tvPosition: TextView by lazy { findViewById(R.id.tvPosition) }
    private val tvDuration: TextView by lazy { findViewById(R.id.tvDuration) }
    private val ibPlay: ImageButton by lazy { findViewById(R.id.ibPlay) }
    private val container: View by lazy { findViewById(R.id.container) }
    private val ibFullscreen: ImageButton by lazy { findViewById(R.id.ibFullscreen) }
    private var dragging = false

    init {
        visibility = View.GONE
        LayoutInflater.from(context)
            .inflate(R.layout.layout_player_portrait_control_view, this, true)
        ibPlay.setOnClickListener {
            wrapper.togglePlay()
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

        //5.1以下系统SeekBar高度需要设置成WRAP_CONTENT
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            seekBar.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = wrapper.duration
                    val newPosition = duration * progress / seekBar.max
                    tvPosition.text = PlayerUtils.stringForTime(newPosition.toInt())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                dragging = true
                wrapper.stopProgress()
                wrapper.stopFadeOut()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val duration = wrapper.duration
                var newPosition = duration * seekBar.progress / seekBar.max
                if (newPosition == wrapper.duration) { //防止直接播放完毕导致重新播放进度条不刷新
                    newPosition = wrapper.duration - 1
                }
                wrapper.seekTo(newPosition)
//                wrapper.start()
                wrapper.startProgress()
                wrapper.startFadeOut()
                dragging = false
            }
        })
    }

    override fun attach(controlWrapper: ControlWrapper) {
        wrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            if (visibility == GONE) {
                this.isVisible = wrapper.isFullScreen.not()
                bringToFront()
                if (anim != null && wrapper.isFullScreen.not()) {
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
            VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED -> {
                visibility = GONE
                seekBar.progress = 0
                seekBar.secondaryProgress = 0
            }

            VideoView.STATE_ERROR -> {
                visibility = GONE
            }

            VideoView.STATE_PREPARING -> {
                visibility = GONE
            }

            VideoView.STATE_PREPARED -> {
                visibility = GONE
            }

            VideoView.STATE_START_ABORT -> {
                visibility = GONE
            }

            VideoView.STATE_PLAYING -> {
                if (!ibPlay.isSelected) {
                    ibPlay.isSelected = true
                } //开始刷新进度
                wrapper.startProgress()
            }

            VideoView.STATE_PAUSED -> {
                if (ibPlay.isSelected) {
                    ibPlay.isSelected = false
                }
            }

            VideoView.STATE_BUFFERING -> {
                wrapper.stopProgress() //亲测必须停止进度刷新才不会导致UI卡顿，否则会阻塞UI
//                if (ibPlay.isSelected) {
//                    ibPlay.isSelected = false
//                }
            }

            VideoView.STATE_BUFFERED -> {
                wrapper.startProgress()
//                if (!ibPlay.isSelected) {
//                    ibPlay.isSelected = true
//                }
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        val isFullScreen = playerState == VideoView.PLAYER_FULL_SCREEN
        if (wrapper.isShowing && !wrapper.isLocked) {
            isVisible = isFullScreen.not()
        }
        applyCutout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCutout()
    }

    private fun applyCutout() {
        val activity = PlayerUtils.scanForActivity(context)
        if (activity != null && wrapper.hasCutout()) {
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

    override fun setProgress(duration: Int, position: Int) {
        if (!dragging) {
            seekBar.isEnabled = duration > 0
            if (seekBar.isEnabled) {
                seekBar.progress = (position * 1.0 / duration * seekBar.max).toInt()
                val percent: Int = wrapper.bufferedPercentage
                if (percent >= 95) { //解决缓冲进度不能100%问题
                    seekBar.secondaryProgress = seekBar.max
                } else {
                    seekBar.secondaryProgress = percent * 10
                }
            }
            tvDuration.text = PlayerUtils.stringForTime(duration)
            tvPosition.text = PlayerUtils.stringForTime(position)
        }
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        isVisible = !isLocked && wrapper.isFullScreen.not()
    }

    override fun onTimedText(timedText: TimedText?) {

    }

    override fun setDataController(controller: MediaDataController) {

    }

    private fun toggleFullScreen() {
        PlayerUtils.scanForActivity(context)?.also {
            wrapper.toggleFullScreen(it)
        }
    }

    fun getIbFullScreen(): View = ibFullscreen

}