package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.*
import androidx.core.view.isVisible
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.model.VideoData
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class LandscapeControlView(context: Context) : FrameLayout(context), IControlComponent {
    private lateinit var wrapper: ControlWrapper
    private val tvScale: TextView by lazy { findViewById(R.id.tvScale) }
    private val tvSpeed: TextView by lazy { findViewById(R.id.tvSpeed) }
    private val tvSelect: TextView by lazy { findViewById(R.id.tvSelect) }

    private val seekBar: SeekBar by lazy { findViewById(R.id.seekBar) }
    private val tvSize: TextView by lazy { findViewById(R.id.tvSize) }
    private val tvPosition: TextView by lazy { findViewById(R.id.tvPosition) }
    private val tvDuration: TextView by lazy { findViewById(R.id.tvDuration) }

    private val ibNext: ImageButton by lazy { findViewById(R.id.ibNext) }
    private val ibPlay: ImageButton by lazy { findViewById(R.id.ibPlay) }
    private val container: View by lazy { findViewById(R.id.container) }
    private val ibFullscreen: ImageButton by lazy { findViewById(R.id.ibFullscreen) }
    private var dragging = false

    init {
        visibility = View.GONE
        LayoutInflater.from(context)
            .inflate(R.layout.layout_player_landscape_control_view, this, true)
        ibNext.setOnClickListener {
            controller?.next()
        }
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

    private var hasAttached = false
    override fun attach(controlWrapper: ControlWrapper) {
        hasAttached = true
        wrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    @SuppressLint("SetTextI18n")
    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        tvSize.isVisible = PlayerUtils.isInLandscape(context) && wrapper.isFullScreen
        if (isVisible) {
            if (visibility == GONE) {
                bringToFront()
                this.isVisible = wrapper.isFullScreen
                this.tvSize.text =
                    "${wrapper.videoSize[0]} × ${wrapper.videoSize[1]}"
                if (anim != null && wrapper.isFullScreen) {
                    startAnimation(anim)
                }
            }
            applyCutout()
        } else {
            if (visibility == VISIBLE) {
                this.isVisible = false
                if (anim != null) {
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
        if (isFullScreen && controller?.hasNext() == true) {
            ibNext.alpha = 1f
            ibNext.isEnabled = true
        } else {
            ibNext.alpha = 0.5f
            ibNext.isEnabled = false
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

        if (wrapper.isShowing && !wrapper.isLocked) {
            isVisible = isFullScreen
        }

        tvSize.isVisible = PlayerUtils.isInLandscape(context) && wrapper.isFullScreen
        applyCutout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tvSize.isVisible = PlayerUtils.isInLandscape(context) && wrapper.isFullScreen
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
        isVisible = !isLocked
    }

    override fun onTimedText(timedText: TimedText?) {

    }

    private fun toggleFullScreen() {
        PlayerUtils.scanForActivity(context)?.also {
            wrapper.toggleFullScreen(it)
        }
    }

    fun getIbNext(): View = ibNext

    fun getIbSelect(): View = tvSelect

    fun getIbScale(): View = tvScale

    fun getIbSpeed(): View = tvSpeed

    fun getIbFullScreen(): View = ibFullscreen

    private var controller: MediaDataController? = null

    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
        controller.addOnPlayListener(object : MediaDataController.OnPlayListener {
            override fun onPlay(position: Int, videoData: VideoData) {
                ibNext.also { next ->
                    if (hasAttached) {
                        if (controller.hasNext() && wrapper.isFullScreen) {
                            next.alpha = 1f
                            next.isEnabled = true
                        } else {
                            next.alpha = 0.5f
                            next.isEnabled = false
                        }
                    }
                }
            }
        })
    }
}