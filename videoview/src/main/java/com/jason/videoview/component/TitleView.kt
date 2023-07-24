package com.jason.videoview.component

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.*
import androidx.core.view.isVisible
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.controller.MediaDataController.OnPlayListener
import com.jason.videoview.model.VideoData
import com.jason.videoview.view.MarqueeTextView
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

@SuppressLint("SourceLockedOrientationActivity")
class TitleView(context: Context) : FrameLayout(context), IControlComponent {
    private lateinit var wrapper: ControlWrapper
    private val ibBack: ImageButton by lazy { findViewById(R.id.ibBack) }
    private val tvTitle: MarqueeTextView by lazy { findViewById(R.id.tvTitle) }
    private val ivBattery: ImageView by lazy { findViewById(R.id.ivBattery) }
    private val tvBattery: TextView by lazy { findViewById(R.id.tvBattery) }
    private val container: LinearLayout by lazy { findViewById(R.id.container) }
    private var showWhenPortrait: Boolean = false

    private val ibTrack: ImageButton by lazy { findViewById(R.id.ibTrack) }
    private val ibSubtitle: ImageButton by lazy { findViewById(R.id.ibSubtitle) }
    private var mIsRegister = false
    private val mBatteryReceiver: BatteryReceiver
    private val hideInPortrait = true

    init {
        isVisible = false
        LayoutInflater.from(context).inflate(R.layout.layout_player_title_view, this, true)
        mBatteryReceiver = BatteryReceiver(ivBattery, tvBattery)
        ibBack.setOnClickListener {
            val activity = PlayerUtils.scanForActivity(context)
            if (wrapper.isFullScreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                wrapper.stopFullScreen()
            } else {
                activity.finishActivity()
            }
        }
    }

    override fun getView(): View {
        return this
    }

    override fun attach(controlWrapper: ControlWrapper) {
        wrapper = controlWrapper
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (PlayerUtils.isInPortrait(context) && hideInPortrait) {
            if (this.isVisible) {
                this.isVisible = false
            }
            return
        }
        if (wrapper.isFullScreen.not() && showWhenPortrait.not()) { //只在全屏时才有效
            this.isVisible = false
            return
        }
        if (isVisible) {
            if (visibility == GONE) {
                this.isVisible = true
                if (anim != null && wrapper.isFullScreen) {
                    startAnimation(anim)
                }
            }
        } else {
            if (visibility == VISIBLE) {
                this.isVisible = false
                if (anim != null && wrapper.isFullScreen) {
                    startAnimation(anim)
                }
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            VideoView.STATE_PLAYING -> {
                ibTrack.isVisible = wrapper.isFullScreen && (wrapper.tracks?.size ?: 0) > 1
                ibSubtitle.isVisible = wrapper.isFullScreen && (wrapper.subtitles?.size ?: 0) > 0
            }

            VideoView.STATE_IDLE, VideoView.STATE_START_ABORT, VideoView.STATE_PREPARING,
            VideoView.STATE_PREPARED, VideoView.STATE_ERROR, VideoView.STATE_PLAYBACK_COMPLETED -> {
                visibility = GONE
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        applyCutout()
        if (PlayerUtils.isInPortrait(context) && hideInPortrait) {
            if (isVisible) {
                isVisible = false
            }
            return
        }
        val isFullScreen = playerState == VideoView.PLAYER_FULL_SCREEN
        if (playerState != VideoView.PLAYER_FULL_SCREEN) {
            isVisible = false
        }
        if (wrapper.isShowing && !wrapper.isLocked && playerState == VideoView.PLAYER_FULL_SCREEN) {
            isVisible = true
            bringToFront()
        }

        ibTrack.isVisible = isFullScreen && (wrapper.tracks?.size ?: 0) > 1
        ibSubtitle.isVisible = isFullScreen && (wrapper.subtitles?.size ?: 0) > 0
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        applyCutout()
        if (PlayerUtils.isInPortrait(context) && hideInPortrait) {
            if (isVisible) {
                isVisible = false
            }
        } else {
            if (wrapper.isShowing) {
                isVisible = true
            }
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

    @SuppressLint("SetTextI18n")
    override fun setProgress(duration: Int, position: Int) {
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        if (PlayerUtils.isInPortrait(context) && hideInPortrait) {
            if (isVisible) {
                isVisible = false
            }
            return
        }
        isVisible = !isLocked
        bringToFront()
    }

    override fun onTimedText(timedText: TimedText?) {

    }

    private var start = 0L

    private fun Activity.finishActivity() {
        if (!wrapper.isPlaying) {
            finish()
        } else {
            if (System.currentTimeMillis() - start < 2000) {
                finish()
            } else {
                start = System.currentTimeMillis()
                Toast.makeText(context, "再按一次退出播放", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mIsRegister) {
            context.unregisterReceiver(mBatteryReceiver)
            mIsRegister = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!mIsRegister) {
            context.registerReceiver(mBatteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            mIsRegister = true
        }
    }

    private class BatteryReceiver(val imageView: ImageView, val textView: TextView) :
        BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras ?: return
            imageView.drawable.level = extras.getInt("level") * 100 / extras.getInt("scale")
            textView.text = (extras.getInt("level") * 100 / extras.getInt("scale")).toString() + "%"
        }
    }

    fun setTitle(title: String) {
        tvTitle.text = title
    }

    fun getIbBack(): View = ibBack

    fun getIbTrack(): View = ibTrack

    fun getIbSubtitle(): View = ibSubtitle

    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
        controller.addOnPlayListener(object : OnPlayListener {
            override fun onPlay(position: Int, videoData: VideoData) {
                val videoName = controller.getVideoName()
                if (videoName.isNotBlank()) {
                    setTitle(videoName + " - " + videoData.name)
                } else {
                    setTitle(videoData.name)
                }
            }
        })
    }
}