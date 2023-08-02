package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTrackNameProvider
import com.jason.cloud.media3.R
import com.jason.cloud.media3.dialog.TrackSelectDialog
import com.jason.cloud.media3.model.AudioTrack
import com.jason.cloud.media3.model.Media3VideoItem
import com.jason.cloud.media3.model.SubtitleTrack
import com.jason.cloud.media3.model.TrackSelectEntity
import com.jason.cloud.media3.utils.Media3PlayerUtils
import com.jason.cloud.media3.utils.Media3VideoScaleModel
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
                onBackPressedListener?.invoke()
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
        ibRatio = findViewById(R.id.media3_ib_ratio)
        ibRotation = findViewById(R.id.media3_ib_rotation)
        bottomBar = findViewById(R.id.media3_bottom_bar)
        ibLargerLock = findViewById(R.id.media3_ib_larger_lock)
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
                statusView.layoutParams.height =
                    Media3PlayerUtils.getStatusBarHeight(context).toInt()
                tvVideoSize.isVisible = false
                titleBar.setPadding(0, 0, 0, 0)
                bottomBar.setPadding(0, 0, 0, 0)
            }
        } else {
            ibRotation.isEnabled = false
            ibRotation.animate().rotationBy(360f).setDuration(250).withEndAction {
                ibRotation.isEnabled = true
                playerView?.startFullScreen()
                titleBar.visibility = View.VISIBLE
                bottomTitle.visibility = View.GONE
                statusView.layoutParams.height = 0
                playerView?.internalPlayer?.videoSize?.let { size ->
                    tvVideoSize.text = "${size.width} × ${size.height}"
                    tvVideoSize.isVisible = true
                }
                titleBar.setPadding(
                    Media3PlayerUtils.getStatusBarHeight(context).toInt(), 0, 0, 0
                )
                bottomBar.setPadding(
                    Media3PlayerUtils.getStatusBarHeight(context).toInt(), 0, 0, 0
                )
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
        Log.e("Media3PlayerControlView", "onMediaItemTransition: $mediaItem")
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
            playerView?.internalPlayer?.playWhenReady = !isPlaying
        }

        var isDragging = false
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
                            var newPosition = duration * seekBar.progress / seekBar.max
                            if (newPosition == duration) {
                                newPosition = duration - 1
                            }
                            seekTo(newPosition)
                            isDragging = false
                        }
                    })
                }

                videoPositionJob?.cancel()
                videoPositionJob = scope.launch {
                    var progress: Int
                    while (isActive) {
                        delay(500)
                        if (isDragging) {
                            continue
                        }
                        progress = (currentPosition / duration.toFloat() * 100).toInt()
                        videoSeekBar.max = 100
                        videoSeekBar.progress = progress
                        videoSeekBar.secondaryProgress = bufferedPercentage
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
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.playWhenReady = true
                }
            }

            val tag = player.currentMediaItem?.localConfiguration?.tag
            if (tag is Media3VideoItem) {
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
                    ibSubtitle.isVisible = getSubtitlesList().isNotEmpty()
                    ibAudioTrack.isVisible = getAudioTracks().isNotEmpty()
                }

                Player.STATE_ENDED -> {}

                Player.STATE_IDLE -> {

                }

                Player.STATE_BUFFERING -> {

                }
            }
        }
    }

    fun getAudioTracks(): List<AudioTrack> {
        return ArrayList<AudioTrack>().apply {
            playerView?.internalPlayer?.let { player ->
                val trackNameProvider = DefaultTrackNameProvider(resources)
                player.currentTracks.groups.filter {
                    it.type == C.TRACK_TYPE_AUDIO
                }.forEach { group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.mediaTrackGroup.length) {
                            val format = group.getTrackFormat(i)
                            if (format.id != null) {
                                Log.i(
                                    "ControlView",
                                    "audioTrack: ${format.label} >> ${group.isSelected}"
                                )
                                if (format.label == null) {
                                    add(
                                        AudioTrack(
                                            format.id!!,
                                            trackNameProvider.getTrackName(format),
                                            group.isSelected
                                        )
                                    )
                                } else {
                                    add(
                                        AudioTrack(
                                            format.id!!,
                                            trackNameProvider.getTrackName(format) + "(${format.label})",
                                            group.isSelected
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectAudioTrack(track: AudioTrack) {
        playerView?.internalPlayer?.let { player ->
            Log.i("TrackSelector", "selectAudioTrack > ${track.name}")
            player.currentTracks.groups.filter {
                it.type == C.TRACK_TYPE_AUDIO
            }.forEach { group ->
                for (i in 0 until group.mediaTrackGroup.length) {
                    val id = group.getTrackFormat(i).id
                    if (id == track.id) {
                        val parameters = player.trackSelectionParameters
                        val selection = TrackSelectionOverride(group.mediaTrackGroup, 0)
                        player.trackSelectionParameters =
                            parameters.buildUpon().setOverrideForType(selection).build()
                        break
                    }
                }
            }
        }
    }

    fun getSubtitlesList(): List<SubtitleTrack> {
        return ArrayList<SubtitleTrack>().apply {
            playerView?.internalPlayer?.let { player ->
                val trackNameProvider = DefaultTrackNameProvider(resources)
                player.currentTracks.groups.filter {
                    it.type == C.TRACK_TYPE_TEXT
                }.forEach { group ->
                    for (i in 0 until group.mediaTrackGroup.length) {
                        val format = group.getTrackFormat(i)
                        if (format.id != null) {
                            Log.i(
                                "ControlView", "subtitle: ${format.label} >> ${group.isSelected}"
                            )
                            if (format.label == null) {
                                add(
                                    SubtitleTrack(
                                        format.id!!,
                                        trackNameProvider.getTrackName(format),
                                        group.isSelected
                                    )
                                )
                            } else {
                                add(
                                    SubtitleTrack(
                                        format.id!!,
                                        trackNameProvider.getTrackName(format) + "(${format.label})",
                                        group.isSelected
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectSubtitle(track: SubtitleTrack) {
        playerView?.internalPlayer?.let { player ->
            Log.i("TrackSelector", "selectSubtitle > ${track.name}")
            player.currentTracks.groups.filter {
                it.type == C.TRACK_TYPE_TEXT
            }.forEach { group ->
                for (i in 0 until group.mediaTrackGroup.length) {
                    val id = group.getTrackFormat(i).id
                    if (id == track.id) {
                        val parameters = player.trackSelectionParameters
                        val selection = TrackSelectionOverride(group.mediaTrackGroup, 0)
                        player.trackSelectionParameters =
                            parameters.buildUpon().setOverrideForType(selection).build()
                        break
                    }
                }
            }
        }
    }

    private fun showSubtitleSelector() {
        playerView?.internalPlayer?.let { player ->
            val subtitles = getSubtitlesList()
            val selectedPosition = subtitles.indexOfFirst { it.isSelected }
            val list = subtitles.map { TrackSelectEntity(it, it.name) }
            TrackSelectDialog(context).apply {
                setTitle("字幕选择")
                setOnShowListener { player.playWhenReady = false }
                setOnDismissListener { player.playWhenReady = true }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    selectSubtitle(it.tag as SubtitleTrack)
                }
                show()
            }
        }
    }

    private fun showAudioTrackSelector() {
        playerView?.internalPlayer?.let { player ->
            val audioTracks = getAudioTracks()
            val selectedPosition = audioTracks.indexOfFirst { it.isSelected }
            val list = audioTracks.map { TrackSelectEntity(it, it.name) }
            TrackSelectDialog(context).apply {
                setTitle("音轨选择")
                setOnShowListener { player.playWhenReady = false }
                setOnDismissListener { player.playWhenReady = true }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    selectAudioTrack(it.tag as AudioTrack)
                }
                show()
            }
        }
    }

    private fun showRatioSelector() {
        playerView?.let { view ->
            val list = ArrayList<TrackSelectEntity>().apply {
                add(TrackSelectEntity(Media3VideoScaleModel.FIT, "自适应"))
                add(TrackSelectEntity(Media3VideoScaleModel.ZOOM, "居中裁剪"))
                add(TrackSelectEntity(Media3VideoScaleModel.FILL, "填充屏幕"))
                add(TrackSelectEntity(Media3VideoScaleModel.FIXED_WIDTH, "宽度自适应"))
                add(TrackSelectEntity(Media3VideoScaleModel.FIXED_HEIGHT, "高度自适应"))
            }
            val selectedPosition = list.indexOfFirst {
                it.tag == view.getScaleModel()
            }

            TrackSelectDialog(context).apply {
                setTitle("画面缩放")
                setOnShowListener { view.internalPlayer.playWhenReady = false }
                setOnDismissListener { view.internalPlayer.playWhenReady = true }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    view.setScaleModel(it.tag as Media3VideoScaleModel)
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
                    if (tag is Media3VideoItem) {
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
                setOnShowListener { player.playWhenReady = false }
                setOnDismissListener { player.playWhenReady = true }
                setSelectedPosition(selectedPosition)
                setSelectionData(list)
                onNegative("取消")
                onPositive("确定") {
                    player.seekTo(it.tag as Int, 0)
                    player.prepare()
                    player.playWhenReady = true
                }
                show()
            }
        }
    }

    private fun ExoPlayer.getCurrentMedia3Item(): Media3VideoItem? {
        val tag = currentMediaItem?.localConfiguration?.tag ?: return null
        return tag as Media3VideoItem
    }
}