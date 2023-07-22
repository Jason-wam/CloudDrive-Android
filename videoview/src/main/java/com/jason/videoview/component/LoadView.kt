package com.jason.videoview.component

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.player.VideoViewManager

class LoadView(context: Context) : FrameLayout(context), IControlComponent {
    private lateinit var wrapper: ControlWrapper
    private val tvStatus: TextView by lazy { findViewById(R.id.tvStatus) }
    private val loadLayout: LinearLayout by lazy { findViewById(R.id.load_layout) }
    private val netWarningLayout: FrameLayout by lazy { findViewById(R.id.net_warning_layout) }
    private val statusBtn: TextView by lazy { findViewById(R.id.status_btn) }
    private var isBuffing = false

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_player_load_view, this, true)
        //isClickable = false
        statusBtn.setOnClickListener {
            VideoViewManager.instance().setPlayOnMobileNetwork(true)
            netWarningLayout.visibility = View.GONE
            wrapper.start()
        }
    }

    /**
     * 设置点击此界面开始播放
     */
    fun setClickStart() {
        setOnClickListener { wrapper.start() }
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
        when (playState) {
            VideoView.STATE_PREPARING -> {
                visibility = VISIBLE
                loadLayout.visibility = VISIBLE
                netWarningLayout.visibility = GONE
                tvStatus.text = context.getString(R.string.player_opening)
                tvStatus.visibility = View.VISIBLE
                bringToFront()
            }

            VideoView.STATE_BUFFERING -> {
                visibility = View.VISIBLE
                isBuffing = true
                netWarningLayout.visibility = View.GONE
                loadLayout.visibility = View.VISIBLE
                tvStatus.text = context.getString(R.string.player_buffing)
                tvStatus.visibility = View.VISIBLE
                bringToFront()
            }

            VideoView.STATE_IDLE -> {
                visibility = VISIBLE
                loadLayout.visibility = GONE
                netWarningLayout.visibility = GONE
                bringToFront()
            }

            VideoView.STATE_START_ABORT -> {
                visibility = VISIBLE
                netWarningLayout.visibility = VISIBLE
                netWarningLayout.bringToFront()
            }

            else -> {
                visibility = GONE
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

    fun setLoadingText(text: String) {
        this.tvStatus.text = text
    }

    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
    }
}