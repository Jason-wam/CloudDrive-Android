package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextClock
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import com.jason.cloud.media3.R
import com.jason.cloud.media3.dialog.TrackSelectDialog
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.model.Media3Track
import com.jason.cloud.media3.model.TrackSelectEntity
import com.jason.cloud.media3.utils.CutoutArea
import com.jason.cloud.media3.utils.CutoutUtil
import com.jason.cloud.media3.utils.Media3VideoScaleMode
import com.jason.cloud.media3.utils.PlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Formatter
import java.util.Locale

@SuppressLint(
    "UnsafeOptInUsageError",
    "ClickableViewAccessibility",
    "SourceLockedOrientationActivity",
    "SetTextI18n"
)
class Media3PlayerControlView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
    Player.Listener {
    internal lateinit var statusView: View
    private lateinit var ibBackspace: ImageButton
    private lateinit var tvTitle: MarqueeTextView
    private lateinit var ivBattery: ImageView
    private lateinit var tvBattery: TextView
    private lateinit var tvClock: TextClock
    private lateinit var titleBar: LinearLayout
    private lateinit var ibLock: ImageButton
    private lateinit var tvBottomTitle: TextView
    private lateinit var tvBottomSubtitle: TextView
    private lateinit var bottomTitle: LinearLayout
    private lateinit var tvVideoPosition: TextView
    private lateinit var videoSeekBar: SeekBar
    private lateinit var tvVideoDuration: TextView
    private lateinit var bottomSeekLayout: LinearLayout
    private lateinit var ibPlay: ImageButton
    private lateinit var ibNext: ImageButton
    private lateinit var tvVideoSize: TextView
    private lateinit var ibList: ImageButton
    private lateinit var ibSubtitle: ImageButton
    private lateinit var ibAudioTrack: ImageButton
    private lateinit var ibPlaySpeed: ImageButton
    private lateinit var ibRatio: ImageButton
    private lateinit var ibRotation: ImageButton
    private lateinit var bottomBar: LinearLayout
    private lateinit var ibLargerLock: ImageButton

    private lateinit var batteryReceiver: BatteryReceiver

    private var playerView: Media3PlayerView? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var videoPositionJob: Job? = null

    private var isBatteryReceiverRegister = false
    private var onBackPressedListener: (() -> Unit)? = null
    private var cutoutArea: CutoutArea = CutoutArea.UNKNOWN
    internal var isDragging = false

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isInEditMode) return
        if (isBatteryReceiverRegister) {
            context.unregisterReceiver(batteryReceiver)
            isBatteryReceiverRegister = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInEditMode) return
        if (!isBatteryReceiverRegister) {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            isBatteryReceiverRegister = true
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

    init {
        LayoutInflater.from(context).inflate(R.layout.media3_player_control_view, this)
        if (isInEditMode.not()) {
            bindViews()

            setOnTouchListener { _, _ ->
                false
            }
            batteryReceiver = BatteryReceiver(ivBattery, tvBattery)
            ibBackspace.setOnClickListener {
                if (playerView?.isInFullscreen == true) {
                    toggleFullScreen()
                } else {
                    onBackPressedListener?.invoke()
                }
            }
            ibList.setOnClickListener {
                showEpisodeSelector()
            }
            ibSubtitle.isVisible = false
            ibSubtitle.setOnClickListener {
                showSubtitleSelector()
            }
            ibAudioTrack.isVisible = false
            ibAudioTrack.setOnClickListener {
                showAudioTrackSelector()
            }
            ibPlaySpeed.setOnClickListener {
                showSpeedSelector()
            }
            ibRatio.setOnClickListener {
                showRatioSelector()
            }
            ibLock.setOnClickListener {
                toggleLockState()
            }
            ibLargerLock.setOnClickListener {
                toggleLockState()
            }
            ibRotation.setOnClickListener {
                toggleFullScreen()
            }
        }
    }

    private fun bindViews() {
        statusView = findViewById(R.id.status_view)
        ibBackspace = findViewById(R.id.media3_ib_backspace)
        tvTitle = findViewById(R.id.media3_tv_title)
        ivBattery = findViewById(R.id.media3_iv_battery)
        tvBattery = findViewById(R.id.media3_tv_battery)
        tvClock = findViewById(R.id.media3_tv_clock)
        titleBar = findViewById(R.id.media3_title_bar)
        ibLock = findViewById(R.id.media3_ib_lock)
        tvBottomTitle = findViewById(R.id.media3_tv_bottom_title)
        tvBottomSubtitle = findViewById(R.id.media3_tv_bottom_subtitle)
        bottomTitle = findViewById(R.id.media3_bottom_title)
        tvVideoPosition = findViewById(R.id.media3_tv_video_position)
        videoSeekBar = findViewById(R.id.media3_video_seek_bar)
        tvVideoDuration = findViewById(R.id.media3_tv_video_duration)
        bottomSeekLayout = findViewById(R.id.media3_bottom_seek_layout)
        ibPlay = findViewById(R.id.media3_ib_play)
        ibNext = findViewById(R.id.media3_ib_next)
        tvVideoSize = findViewById(R.id.media3_tv_video_size)
        ibList = findViewById(R.id.media3_ib_list)
        ibSubtitle = findViewById(R.id.media3_ib_subtitle)
        ibAudioTrack = findViewById(R.id.media3_ib_audio_track)
        ibPlaySpeed = findViewById(R.id.media3_ib_play_speed)
        ibRatio = findViewById(R.id.media3_ib_ratio)
        ibRotation = findViewById(R.id.media3_ib_rotation)
        bottomBar = findViewById(R.id.media3_bottom_bar)
        ibLargerLock = findViewById(R.id.media3_ib_larger_lock)
    }

    fun setCutoutArea(cutoutRect: Rect) {
        if (cutoutRect.isEmpty) {
            this.cutoutArea = CutoutArea.UNKNOWN
        } else {
            val width = cutoutRect.right - cutoutRect.left
            val center = cutoutRect.right - width / 2
            if (cutoutRect.right < context.resources.displayMetrics.widthPixels / 2) {
                this.cutoutArea = CutoutArea.LEFT
            } else if (cutoutRect.left > context.resources.displayMetrics.widthPixels / 2) {
                this.cutoutArea = CutoutArea.RIGHT
            } else if (center == context.resources.displayMetrics.widthPixels / 2) {
                this.cutoutArea = CutoutArea.CENTER
            } else {
                this.cutoutArea = CutoutArea.UNKNOWN
            }
        }
    }

    private fun toggleFullScreen() {
        if (playerView?.isInFullscreen == true) {
            ibRotation.isEnabled = false
            ibRotation.animate().rotationBy(-360f).setDuration(250).withEndAction {
                ibRotation.isEnabled = true
                playerView?.cancelFullScreen()
                playerView?.isInFullscreen = false
                titleBar.visibility = View.INVISIBLE
                bottomTitle.visibility = View.VISIBLE
                tvVideoSize.isVisible = false
            }
        } else {
            ibRotation.isEnabled = false
            ibRotation.animate().rotationBy(360f).setDuration(250).withEndAction {
                ibRotation.isEnabled = true
                playerView?.startFullScreen()
                titleBar.visibility = View.VISIBLE
                bottomTitle.visibility = View.GONE
                playerView?.internalPlayer?.videoSize?.let { size ->
                    tvVideoSize.text = "${size.width} × ${size.height}"
                    tvVideoSize.isVisible = true
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        PlayerUtils.scanForActivity(context)?.let { activity ->
            Log.i(
                "onConfigurationChanged",
                "orientation = ${activity.requestedOrientation}"
            )
            val statusHeight: Int = PlayerUtils.getStatusBarHeightPortrait(context).toInt()
            if (CutoutUtil.hasCutout(activity).not()) { //没有刘海的情况下
                if (playerView?.isInFullscreen == true) { //非全屏情况下
                    titleBar.setPadding(0, 0, 0, 0)
                    bottomBar.setPadding(0, 0, 0, 0)
                    statusView.layoutParams.height = 0
                } else {
                    titleBar.setPadding(0, 0, 0, 0)
                    bottomBar.setPadding(0, 0, 0, 0)
                    statusView.layoutParams.height = statusHeight
                }
            } else { //有刘海
                if (playerView?.isInFullscreen == true) { //非全屏情况下
                    statusView.layoutParams.height = 0
                    if (cutoutArea == CutoutArea.CENTER) {
                        titleBar.setPadding(0, 0, 0, 0)
                        bottomBar.setPadding(0, 0, 0, 0)
                    } else {
                        titleBar.setPadding(statusHeight, 0, 0, 0)
                        bottomBar.setPadding(statusHeight, 0, 0, 0)
                    }
                } else {
                    titleBar.setPadding(0, 0, 0, 0)
                    bottomBar.setPadding(0, 0, 0, 0)
                    statusView.layoutParams.height = statusHeight
                }
            }
        }
    }

    private fun toggleLockState() {
        if (playerView?.isLocked == true) {
            playerView?.isLocked = false
            ibLargerLock.isVisible = false

            if (isVisible.not()) {
                if (playerView?.isInFullscreen == true) {
                    titleBar.visibility = View.VISIBLE
                }
            } else {
                if (playerView?.isInFullscreen == true) {
                    titleBar.visibility = View.VISIBLE
                    titleBar.alpha = 0f
                    titleBar.animate().alpha(1f).setDuration(500).withEndAction {
                        titleBar.visibility = View.VISIBLE
                    }
                }
            }

            if (isVisible.not()) {
                ibLock.isSelected = false
                ibLock.visibility = View.VISIBLE
                bottomBar.visibility = View.VISIBLE
            } else {
                ibLock.isSelected = false
                ibLock.visibility = View.VISIBLE
                ibLock.alpha = 0f
                ibLock.animate().alpha(1f).setDuration(500).withEndAction {
                    ibLock.visibility = View.VISIBLE
                }
                bottomBar.visibility = View.VISIBLE
                bottomBar.alpha = 0f
                bottomBar.animate().alpha(1f).setDuration(500).withEndAction {
                    bottomBar.visibility = View.VISIBLE
                }
            }
        } else {
            playerView?.isLocked = true
            ibLargerLock.isVisible = true

            ibLock.isSelected = true
            ibLock.animate().alpha(0f).setDuration(500).withEndAction {
                ibLock.visibility = View.INVISIBLE
                ibLock.alpha = 1f
            }

            titleBar.animate().alpha(0f).setDuration(500).withEndAction {
                titleBar.visibility = View.INVISIBLE
                titleBar.alpha = 1f
            }

            bottomBar.animate().alpha(0f).setDuration(500).withEndAction {
                bottomBar.visibility = View.INVISIBLE
                bottomBar.alpha = 1f
            }
        }
    }

    fun attachPlayerView(playerView: Media3PlayerView) {
        this.playerView = playerView
        this.playerView?.internalPlayer?.addListener(this)
        updateNextButtonAction()
    }

    fun show() {
        if (isVisible.not()) {
            alpha = 0f
            isVisible = true
            animate().alpha(1f).duration = 500
        }
    }

    fun hide() {
        if (isVisible) {
            alpha = 1f
            animate().alpha(0f).setDuration(500).withEndAction {
                isVisible = false
            }
        }
    }

    fun onBackPressed(listener: () -> Unit) {
        this.onBackPressedListener = listener
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        val tag = mediaItem?.localConfiguration?.tag
        if (tag is Media3Item) {
            Log.e("Media3PlayerControlView", "onMediaItemTransition: ${tag.title}")
        }
        updateNextButtonAction()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        updateMediaButtons(playbackState)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        ibPlay.isSelected = isPlaying
        ibPlay.setOnClickListener {
            if (isPlaying) {
                playerView?.pause()
            } else {
                playerView?.start()
            }
        }

        val formatBuilder = StringBuilder()
        val formatter = Formatter(formatBuilder, Locale.getDefault())
        if (isPlaying.not()) {
            videoPositionJob?.cancel()
            videoSeekBar.setOnSeekBarChangeListener(null)
        } else {
            playerView?.internalPlayer?.run {
                if (duration > 0) {
                    videoSeekBar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar, progress: Int, fromUser: Boolean
                        ) {
                            if (fromUser) {
                                val newPosition = duration * progress / seekBar.max
                                tvVideoPosition.text = Util.getStringForTime(
                                    formatBuilder, formatter, newPosition
                                )
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {
                            isDragging = true
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            if (seekBar.progress == seekBar.max) {
                                seekTo(duration - 1)
                            } else {
                                var newPosition = duration * seekBar.progress / seekBar.max
                                if (newPosition == duration) {
                                    newPosition = duration - 1
                                }
                                seekTo(newPosition)
                            }
                            isDragging = false
                        }
                    })
                }

                videoPositionJob?.cancel()
                videoPositionJob = scope.launch {
                    var progress: Float
                    var bufferedProgress: Float
                    while (isActive) {
                        delay(500)
                        if (isDragging) {
                            continue
                        }
                        progress = currentPosition / duration.toFloat() * 100
                        bufferedProgress = contentBufferedPosition / contentDuration.toFloat() * 100
                        Log.e("ControlView", "bufferedProgress = $bufferedProgress")
                        videoSeekBar.max = 100
                        videoSeekBar.progress = progress.toInt()
                        videoSeekBar.secondaryProgress = bufferedProgress.toInt()
                        tvVideoPosition.text = Util.getStringForTime(
                            formatBuilder, formatter, currentPosition
                        )
                        tvVideoDuration.text = Util.getStringForTime(
                            formatBuilder, formatter, duration
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateNextButtonAction() {
        playerView?.internalPlayer?.let { player ->
            ibList.isVisible = player.mediaItemCount > 1
            if (player.hasNextMediaItem().not()) {
                ibNext.alpha = 0.5f
                ibNext.isEnabled = false
            } else {
                ibNext.alpha = 1.0f
                ibNext.isEnabled = true
                ibNext.setOnClickListener {
                    playerView?.seekToNext()
                    playerView?.prepare()
                    playerView?.start()
                }
            }

            val tag = player.currentMediaItem?.localConfiguration?.tag
            if (tag is Media3Item) {
                tvBottomTitle.text = tag.title
                tvBottomTitle.isVisible = tag.title.isNotBlank()
                tvBottomSubtitle.text = tag.subtitle
                tvBottomSubtitle.isVisible = tag.subtitle.isNotBlank()
                tvTitle.text =
                    tag.title + if (tag.subtitle.isNotBlank()) " / " + tag.subtitle else ""
            }
        }
    }

    private fun updateMediaButtons(playbackState: Int) {
        playerView?.internalPlayer?.let {
            when (playbackState) {
                Player.STATE_READY -> {
                    ibSubtitle.isVisible = it.getTrackList(context, C.TRACK_TYPE_TEXT).isNotEmpty()
                    ibAudioTrack.isVisible = it.getTrackList(context, C.TRACK_TYPE_AUDIO).size > 1
                }
            }
        }
    }

    private fun showSubtitleSelector() {
        playerView?.internalPlayer?.let { player ->
            val subtitles = player.getTrackList(context, C.TRACK_TYPE_TEXT)
            val selectedPosition = subtitles.indexOfFirst { it.selected }
            val list = subtitles.map { TrackSelectEntity(it, it.name) }
            TrackSelectDialog(context).apply {
                setTitle("字幕选择")
                setOnShowListener { playerView?.pause() }
                setOnDismissListener { playerView?.start() }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    player.selectTrack(it.tag as Media3Track)
                }
                show()
            }
        }
    }

    private fun showAudioTrackSelector() {
        playerView?.internalPlayer?.let { player ->
            val audioTracks = player.getTrackList(context, C.TRACK_TYPE_AUDIO)
            val selectedPosition = audioTracks.indexOfFirst { it.selected }
            val list = audioTracks.map { TrackSelectEntity(it, it.name) }
            TrackSelectDialog(context).apply {
                setTitle("音轨选择")
                setOnShowListener { playerView?.pause() }
                setOnDismissListener { playerView?.start() }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    player.selectTrack(it.tag as Media3Track)
                }
                show()
            }
        }
    }

    private fun showSpeedSelector() {
        playerView?.internalPlayer?.let { player ->
            val list = ArrayList<TrackSelectEntity>().apply {
                add(TrackSelectEntity(0.25f, "0.25x"))
                add(TrackSelectEntity(0.5f, "0.5x"))
                add(TrackSelectEntity(1.0f, "1.0x"))
                add(TrackSelectEntity(1.25f, "1.25x"))
                add(TrackSelectEntity(1.5f, "1.5x"))
                add(TrackSelectEntity(2.0f, "2.0x"))
                add(TrackSelectEntity(3.0f, "3.0x"))
                add(TrackSelectEntity(4.0f, "4.0x"))
                add(TrackSelectEntity(8.0f, "8.0x"))
            }

            val selectedPosition = list.indexOfFirst {
                it.tag as Float == player.playbackParameters.speed
            }

            TrackSelectDialog(context).apply {
                setTitle("倍速播放")
                setOnShowListener { playerView?.pause() }
                setOnDismissListener { playerView?.start() }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    player.playbackParameters = PlaybackParameters(it.tag as Float)
                }
                show()
            }
        }
    }

    private fun showRatioSelector() {
        playerView?.let { view ->
            val list = ArrayList<TrackSelectEntity>().apply {
                add(TrackSelectEntity(Media3VideoScaleMode.FIT, "自适应"))
                add(TrackSelectEntity(Media3VideoScaleMode.ZOOM, "居中裁剪"))
                add(TrackSelectEntity(Media3VideoScaleMode.FILL, "填充屏幕"))
                add(TrackSelectEntity(Media3VideoScaleMode.FIXED_WIDTH, "宽度固定"))
                add(TrackSelectEntity(Media3VideoScaleMode.FIXED_HEIGHT, "高度固定"))
            }

            val selectedPosition = list.indexOfFirst {
                it.tag == view.getScaleMode()
            }

            TrackSelectDialog(context).apply {
                setTitle("画面缩放")
                setOnShowListener { view.pause() }
                setOnDismissListener { view.start() }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    view.setScaleMode(it.tag as Media3VideoScaleMode)
                }
                show()
            }
        }
    }

    private fun showEpisodeSelector() {
        playerView?.internalPlayer?.let { player ->
            var selectedPosition = 0
            val list = ArrayList<TrackSelectEntity>().apply {
                for (i in 0 until player.mediaItemCount) {
                    val tag = player.getMediaItemAt(i).localConfiguration?.tag
                    if (tag is Media3Item) {
                        val name = if (tag.subtitle.isBlank()) tag.title else {
                            tag.title + " / " + tag.subtitle
                        }
                        if (tag.url == player.getCurrentMedia3Item()?.url) {
                            selectedPosition = i
                        }
                        add(TrackSelectEntity(i, name))
                    }
                }
            }

            TrackSelectDialog(context).apply {
                setTitle("选择剧集")
                setOnShowListener { playerView!!.pause() }
                setOnDismissListener { playerView!!.start() }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    playerView?.seekToItem(it.tag as Int, 0)
                    playerView?.prepare()
                    playerView?.start()
                }
                show()
            }
        }
    }
}