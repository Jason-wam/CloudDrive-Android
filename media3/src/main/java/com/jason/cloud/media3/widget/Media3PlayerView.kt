package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.jason.cloud.media3.R
import com.jason.cloud.media3.interfaces.OnControlViewVisibleListener
import com.jason.cloud.media3.interfaces.OnMediaItemTransitionListener
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3VideoItem
import com.jason.cloud.media3.utils.FfmpegRenderersFactory
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.utils.Media3PlayerUtils
import com.jason.cloud.media3.utils.Media3SourceHelper
import com.jason.cloud.media3.utils.Media3VideoScaleModel
import com.jason.cloud.media3.utils.Media3VideoScaleModel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@SuppressLint("UnsafeOptInUsageError")
class Media3PlayerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private lateinit var ivHolderBackground: ImageView
    private lateinit var subtitleView: SubtitleView
    private lateinit var ratioContentFrame: AspectRatioFrameLayout
    private lateinit var gestureView: Media3GestureView
    private lateinit var controlView: Media3PlayerControlView
    private lateinit var errorMessage: TextView
    private lateinit var errorStatusBtn: TextView
    private lateinit var errorLayout: LinearLayout
    private lateinit var bufferingView: CircularProgressIndicator
    private lateinit var bufferingMessage: TextView
    private lateinit var bufferingLayout: LinearLayout
    private lateinit var indicatorSlideVolume: CircularProgressIndicator
    private lateinit var tvSlideVolume: TextView
    private lateinit var slideVolume: LinearLayout
    private lateinit var indicatorSlideBrightness: CircularProgressIndicator
    private lateinit var tvSlideBrightness: TextView
    private lateinit var slideBrightness: LinearLayout
    private lateinit var indicatorSlidePosition: CircularProgressIndicator
    private lateinit var tvSlidePosition: TextView
    private lateinit var slidePosition: LinearLayout
    private lateinit var doubleSpeedPlaying: TextView
    private lateinit var rootContainer: FrameLayout
    private lateinit var rootView: FrameLayout
    private lateinit var surfaceView: View

    private val mediaSourceHelper by lazy { Media3SourceHelper.getInstance(context) }
    private var speedPlaybackParameters: PlaybackParameters? = null

    private var currentPlayState = Media3PlayState.STATE_IDLE
    internal val internalPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setRenderersFactory(FfmpegRenderersFactory(context))
            .build()
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var hideControlViewJob: Job? = null

    internal var isLocked = false
    internal var isInFullscreen = false
    private var isInSliding = false

    private lateinit var playerListener: Player.Listener
    private var onControlViewVisibleListener: OnControlViewVisibleListener? = null
    private var onPlayStateListeners = ArrayList<OnStateChangeListener>()
    private var onMediaItemTransitionListeners = ArrayList<OnMediaItemTransitionListener>()
    private var onBackPressedListener: (() -> Unit)? = null
    private var onRequestScreenOrientationListener: ((isFullScreen: Boolean) -> Unit)? = null

    private fun getUserCaptionStyle(): CaptionStyleCompat {
        return CaptionStyleCompat(
            Color.WHITE,
            Color.TRANSPARENT,
            Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
            Color.DKGRAY,
            Typeface.DEFAULT
        )
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.media3_player_view, this)
        if (isInEditMode.not()) {
            bindViews()
            initPlayListener()
            subtitleView.setUserDefaultTextSize()
            subtitleView.setStyle(getUserCaptionStyle())

            ratioContentFrame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            ratioContentFrame.addView(
                surfaceView, 0, LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            )

            internalPlayer.addAnalyticsListener(EventLogger("ExoPlayer"))
            internalPlayer.setVideoSurfaceView(surfaceView as SurfaceView)
            internalPlayer.setWakeMode(C.WAKE_MODE_LOCAL)
            internalPlayer.addListener(playerListener)
            controlView.attachPlayerView(this)
            gestureView.attachPlayerView(this)

            controlView.onBackPressed {
                if (isInFullscreen) {
                    cancelFullScreen()
                } else {
                    onBackPressedListener?.invoke()
                }
            }
        }
    }

    private fun bindViews() {
        ivHolderBackground = findViewById(R.id.media3_iv_holder_background)
        subtitleView = findViewById(R.id.media3_subtitles)
        ratioContentFrame = findViewById(R.id.media3_content_frame)
        gestureView = findViewById(R.id.gesture_view)
        controlView = findViewById(R.id.media3_control_view)
        errorMessage = findViewById(R.id.media3_error_message)
        errorStatusBtn = findViewById(R.id.media3_error_status_btn)
        errorLayout = findViewById(R.id.media3_error_layout)
        bufferingView = findViewById(R.id.media3_buffering)
        bufferingMessage = findViewById(R.id.media3_buffering_message)
        bufferingLayout = findViewById(R.id.media3_buffering_layout)
        indicatorSlideVolume = findViewById(R.id.media3_indicator_slide_volume)
        tvSlideVolume = findViewById(R.id.media3_tv_slide_volume)
        slideVolume = findViewById(R.id.media3_slide_volume)
        indicatorSlideBrightness = findViewById(R.id.media3_indicator_slide_brightness)
        tvSlideBrightness = findViewById(R.id.media3_tv_slide_brightness)
        slideBrightness = findViewById(R.id.media3_slide_brightness)
        indicatorSlidePosition = findViewById(R.id.media3_indicator_slide_position)
        tvSlidePosition = findViewById(R.id.media3_tv_slide_position)
        slidePosition = findViewById(R.id.media3_slide_position)
        doubleSpeedPlaying = findViewById(R.id.media3_double_speed_playing)
        rootContainer = findViewById(R.id.root_container)
        rootView = findViewById(R.id.root_view)

        surfaceView = SurfaceView(context)
    }

    private fun initPlayListener() {
        playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                //相当于暂停继续
                surfaceView.keepScreenOn = isPlaying
                if (isPlaying) {
                    val videoSize = internalPlayer.videoSize
                    ratioContentFrame.setAspectRatio(
                        videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    )

                    startHideControlViewJob()
                    currentPlayState = Media3PlayState.STATE_PLAYING
                    onPlayStateListeners.forEach {
                        it.onStateChanged(currentPlayState)
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                if (playWhenReady.not()) {
                    currentPlayState = Media3PlayState.STATE_PAUSED
                    onPlayStateListeners.forEach {
                        it.onStateChanged(currentPlayState)
                    }
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                super.onCues(cueGroup)
                subtitleView.setCues(cueGroup.cues)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                if (videoSize.width > 0 && videoSize.height > 0) {
                    ratioContentFrame.setAspectRatio(
                        videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    )
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                onPlayStateListeners.forEach {
                    it.onStateChanged(playbackState)
                }
                bufferingLayout.isVisible = false
                Log.e("PlayerView", "playbackState = $playbackState")
                when (playbackState) {
                    Player.STATE_READY -> {
                        currentPlayState = Media3PlayState.STATE_PREPARED
                        startHideControlViewJob()
                    }

                    Player.STATE_ENDED -> {
                        currentPlayState = Media3PlayState.STATE_ENDED
                    }

                    Player.STATE_IDLE -> {
                        currentPlayState = Media3PlayState.STATE_IDLE
                        val error = internalPlayer.playerError
                        if (error != null) {
                            currentPlayState = Media3PlayState.STATE_ERROR
                            errorLayout.isVisible = true
                            errorMessage.text = "${error.errorCode} : ${error.errorCodeName}"
                            errorStatusBtn.setOnClickListener {
                                retryPlayback()
                            }
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        currentPlayState = Media3PlayState.STATE_BUFFERING
                        errorLayout.isVisible = false
                        bufferingMessage.text = context.getString(R.string.media3_on_buffing_media)
                        bufferingLayout.isVisible = true
                    }
                }
            }
        }
    }

    fun getStatusView(): View {
        return controlView.statusView
    }

    fun getScaleModel(): Media3VideoScaleModel {
        return when (ratioContentFrame.resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> FIT
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> FILL
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> FIXED_WIDTH
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> FIXED_HEIGHT
            else -> FIT
        }
    }

    fun setScaleModel(scaleModel: Media3VideoScaleModel) {
        when (scaleModel) {
            FIT -> ratioContentFrame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            FILL -> ratioContentFrame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            ZOOM -> ratioContentFrame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

            FIXED_WIDTH -> {
                ratioContentFrame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            }

            FIXED_HEIGHT -> {
                ratioContentFrame.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            }
        }
    }


    fun setDataSource(item: Media3VideoItem) {
        internalPlayer.setMediaSource(mediaSourceHelper.getMediaSource(item))
    }

    fun setDataSource(path: String) {
        internalPlayer.setMediaSource(mediaSourceHelper.getMediaSource(path, false))
    }

    fun setDataSource(path: String, headers: Map<String, String>? = null) {
        internalPlayer.setMediaSource(mediaSourceHelper.getMediaSource(path, headers, false))
    }

    fun addDataSource(itemList: List<Media3VideoItem>) {
        internalPlayer.addMediaSources(mediaSourceHelper.getMediaSource(itemList))
    }

    fun setDataSource(itemList: List<Media3VideoItem>) {
        internalPlayer.setMediaSources(mediaSourceHelper.getMediaSource(itemList))
    }

    fun start() {
        internalPlayer.playWhenReady = true
    }

    fun pause() {
        internalPlayer.playWhenReady = false
    }

    fun stop() {
        internalPlayer.stop()
    }

    fun retryPlayback() {
        prepare()
        internalPlayer.playWhenReady = true
    }

    fun prepare() {
        internalPlayer.prepare()
        bufferingMessage.text = context.getString(R.string.media3_on_opening_media)
        bufferingView.isIndeterminate = true
        bufferingLayout.isVisible = true
    }

    fun reset() {
        internalPlayer.stop()
        internalPlayer.clearMediaItems()
        internalPlayer.setVideoSurface(null)
    }

    fun isPlaying(): Boolean {
        return when (internalPlayer.playbackState) {
            Player.STATE_BUFFERING, Player.STATE_READY -> internalPlayer.playWhenReady
            Player.STATE_IDLE, Player.STATE_ENDED -> false
            else -> false
        }
    }

    fun currentPlayState(): Int {
        return currentPlayState
    }

    fun seekTo(time: Long) {
        internalPlayer.seekTo(time)
    }

    fun seekToItem(mediaItemIndex: Int, positionMs: Long) {
        internalPlayer.seekTo(mediaItemIndex, positionMs)
    }

    fun getCurrentPosition(): Long {
        return internalPlayer.currentPosition
    }

    fun getDuration(): Long {
        return internalPlayer.duration
    }

    fun getBufferedPercentage(): Int {
        return internalPlayer.bufferedPercentage
    }

    fun setVolume(volume: Float) {
        internalPlayer.volume = volume
    }

    fun setLooping(isLooping: Boolean) {
        internalPlayer.repeatMode = if (isLooping)
            Player.REPEAT_MODE_ALL
        else
            Player.REPEAT_MODE_OFF
    }

    fun setSpeed(speed: Float) {
        internalPlayer.playbackParameters = PlaybackParameters(speed).also {
            speedPlaybackParameters = it
        }
    }

    fun getSpeed(): Float {
        return speedPlaybackParameters?.speed ?: 1f
    }

    fun release() {
        hideControlViewJob?.cancel()
        surfaceView.keepScreenOn = false
        internalPlayer.removeListener(playerListener)
        internalPlayer.release()
    }

    fun getCurrentMediaItemIndex(): Int {
        return internalPlayer.currentMediaItemIndex
    }

    fun hasNextMediaItem(): Boolean {
        return internalPlayer.hasNextMediaItem()
    }

    fun hasPreviousMediaItem(): Boolean {
        return internalPlayer.hasPreviousMediaItem()
    }

    fun seekToNext() {
        internalPlayer.seekToNext()
    }

    fun seekToPrevious() {
        internalPlayer.seekToPrevious()
    }

    fun addOnStateChangeListener(listener: OnStateChangeListener) {
        this.onPlayStateListeners.add(listener)
    }

    fun removeOnStateChangeListener(listener: OnStateChangeListener) {
        this.onPlayStateListeners.remove(listener)
    }

    fun clearOnStateChangeListener() {
        this.onPlayStateListeners.clear()
    }

    fun removeOnMediaItemTransitionListener(listener: OnMediaItemTransitionListener) {
        this.onMediaItemTransitionListeners.remove(listener)
    }

    fun addOnMediaItemTransitionListener(listener: OnMediaItemTransitionListener) {
        this.onMediaItemTransitionListeners.add(listener)
    }

    fun clearOnMediaItemTransitionListener() {
        this.onMediaItemTransitionListeners.clear()
    }

    /**********************ControlView*********************************/

    fun onControlViewVisibleListener(listener: OnControlViewVisibleListener) {
        this.onControlViewVisibleListener = listener
    }

    fun showControlView() {
        if (isInFullscreen.not()) {
            showBars()
        }
        controlView.show()
        onControlViewVisibleListener?.onVisibleChanged(true, isInFullscreen)
        startHideControlViewJob()
    }

    fun hideControlView() {
        hideBars()
        controlView.hide()
        onControlViewVisibleListener?.onVisibleChanged(false, isInFullscreen)
    }

    fun isControlViewVisible(): Boolean {
        return controlView.isVisible
    }

    fun onStartSlide() {
        isInSliding = true
    }

    fun onStopSlide() {
        isInSliding = false
        slideVolume.isVisible = false
        slideBrightness.isVisible = false
        slidePosition.isVisible = false
        doubleSpeedPlaying.isVisible = false
    }

    fun onSlideVolume(percent: Int) {
        slideVolume.isVisible = true
        indicatorSlideVolume.progress = percent
        tvSlideVolume.text = context.getString(R.string.media3_slide_volume, percent)
    }

    fun onSlideBrightness(percent: Int) {
        slideBrightness.isVisible = true
        indicatorSlideBrightness.progress = percent
        tvSlideBrightness.text = context.getString(R.string.media3_slide_brightness, percent)
    }

    fun onSlidePosition(percent: Int, position: Long, duration: Long) {
        slidePosition.isVisible = true
        indicatorSlidePosition.progress = percent
        tvSlidePosition.text = context.getString(
            R.string.media3_slide_position,
            Media3PlayerUtils.stringForTime(position),
            Media3PlayerUtils.stringForTime(duration)
        )
    }

    fun onDoubleSpeedPlaying(speed: Float) {
        doubleSpeedPlaying.isVisible = true
        doubleSpeedPlaying.text = context.getString(
            R.string.media3_double_speed_playing,
            speed
        )
    }

    fun onBackPressed(listener: () -> Unit) {
        this.onBackPressedListener = listener
    }

    fun onRequestScreenOrientationListener(listener: (fullScreen: Boolean) -> Unit) {
        this.onRequestScreenOrientationListener = listener
    }

    private fun startHideControlViewJob() {
        hideControlViewJob?.cancel()
        hideControlViewJob = scope.launch {
            var timeCount = 0
            while (isActive) {
                delay(1000)
                timeCount += 1
                if (timeCount > 4) {
                    if (internalPlayer.isPlaying) {
                        if (controlView.isVisible) {
                            hideBars()
                            controlView.hide()
                            onControlViewVisibleListener?.onVisibleChanged(
                                false,
                                isInFullscreen
                            )
                        }
                        break
                    } else {
                        if (controlView.isVisible.not()) {
                            if (isInFullscreen.not()) {
                                showBars()
                            }
                            controlView.show()
                            onControlViewVisibleListener?.onVisibleChanged(
                                true,
                                isInFullscreen
                            )
                        }
                        break
                    }
                }
            }
        }
    }

    fun startFullScreen() {
        hideBars()
        Media3PlayerUtils.scanForActivity(context)?.let { activity ->
            onRequestScreenOrientationListener?.invoke(true)
            rootView.removeView(rootContainer)
            val decorView = activity.window.decorView as ViewGroup
            decorView.addView(rootContainer)
            isInFullscreen = true
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    fun cancelFullScreen() {
        if (controlView.isVisible) {
            showBars()
        }
        Media3PlayerUtils.scanForActivity(context)?.let { activity ->
            onRequestScreenOrientationListener?.invoke(false)
            val decorView = activity.window.decorView as ViewGroup
            decorView.removeView(rootContainer)
            rootView.addView(rootContainer)
            isInFullscreen = false
        }
    }

    private fun hideBars() {
        Media3PlayerUtils.scanForActivity(context)?.let { activity ->
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).also {
                it.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun showBars() {
        Media3PlayerUtils.scanForActivity(context)?.let { activity ->
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).also {
                it.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                it.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}