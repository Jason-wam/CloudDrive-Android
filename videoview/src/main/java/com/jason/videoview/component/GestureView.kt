package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.*
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IGestureComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class GestureView(context: Context) : FrameLayout(context), IGestureComponent {
    private lateinit var wrapper: ControlWrapper
    private val ivIcon: ImageView by lazy { findViewById(R.id.ivIcon) }
    private val tvPercent: TextView by lazy { findViewById(R.id.tvPercent) }
    private val container: View by lazy { findViewById(R.id.container) }
    private val progressBar: LinearProgressIndicator by lazy { findViewById(R.id.progressBar) }

    init {
        isVisible = false
        LayoutInflater.from(context).inflate(R.layout.layout_player_gesture_view, this, true)
        bringToFront()
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
        isVisible = when (playState) {
            VideoView.STATE_IDLE, VideoView.STATE_START_ABORT, VideoView.STATE_PREPARING, VideoView.STATE_PREPARED, VideoView.STATE_ERROR, VideoView.STATE_PLAYBACK_COMPLETED -> {
                false
            }

            else -> {
                true
            }
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

    override fun onStartSlide() {
        wrapper.hide()
        container.isVisible = true
        bringToFront()
    }

    override fun onStopSlide() {
        container.isVisible = false
    }

    @SuppressLint("SetTextI18n")
    override fun onPositionChange(slidePosition: Int, currentPosition: Int, duration: Int) {
        if (slidePosition > currentPosition) {
            ivIcon.setImageResource(R.drawable.ic_player_fast_forward_24)
        } else {
            ivIcon.setImageResource(R.drawable.ic_player_fast_rewind_24)
        }
        this.tvPercent.text =
            PlayerUtils.stringForTime(slidePosition) + " / " + PlayerUtils.stringForTime(wrapper.duration.toInt())
        this.progressBar.setProgressCompat(
            (slidePosition.toFloat() / duration * 100).toInt(),
            false
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBrightnessChange(percent: Int) {
        if (percent < 40) {
            ivIcon.setImageResource(R.drawable.ic_player_brightness_low_24)
        } else if (percent in 41..79) {
            ivIcon.setImageResource(R.drawable.ic_player_brightness_medium_24)
        } else if (percent > 80) {
            ivIcon.setImageResource(R.drawable.ic_player_brightness_high_24)
        }
        this.progressBar.setProgressCompat(percent, false)
        this.tvPercent.text = "$percent / 100"
    }

    @SuppressLint("SetTextI18n")
    override fun onVolumeChange(percent: Int) {
        if (percent < 30) {
            ivIcon.setImageResource(R.drawable.ic_player_volume_mute_24)
        } else if (percent in 31..69) {
            ivIcon.setImageResource(R.drawable.ic_player_volume_down_24)
        } else if (percent > 70) {
            ivIcon.setImageResource(R.drawable.ic_player_volume_up_24)
        }
        this.tvPercent.text = "$percent / 100"
        this.progressBar.setProgressCompat(percent, false)
    }

    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
    }
}