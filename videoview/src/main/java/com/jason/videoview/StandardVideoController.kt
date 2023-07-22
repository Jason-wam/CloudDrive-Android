package com.jason.videoview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import com.jason.cloud.extension.toFileSizeString
import com.jason.videoview.component.*
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.model.DanmakuEntity
import com.jason.videoview.util.VibratorUtil
import com.jason.videoview.view.TimedTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import master.flame.danmaku.danmaku.model.BaseDanmaku
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.GestureVideoController
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class StandardVideoController(context: Context) : GestureVideoController(context) {
    private var lastSpeed: Float = 1.0f
    private var isInFastForward = false
    private var fastForwardRate: Float = 3.0f
    private var autoToggleScreenDirectionInFullscreen = false

    private lateinit var pipContainer: LinearLayout
    private lateinit var lockContainer: LinearLayout
    private lateinit var positionContainer: LinearLayout

    private lateinit var ibLock: ImageView
    private lateinit var ibPictureInPicture: ImageView

    private lateinit var tvPosition: TextView
    private lateinit var tvFastForward: TextView
    private lateinit var timedTextView: TimedTextView
    private lateinit var tvDownloadSpeed: TextView

    private lateinit var danmakuView: MyDanmakuView
    private lateinit var timedTextDanmakuView: MyTimedTextDanmakuView

    private lateinit var dataController: MediaDataController

    private var isFullScreenButtonEnabled = true
    private var takeoverRetryLogic: (() -> Unit)? = null
    private var takeoverFullScreenLogic: (ControlWrapper.() -> Unit)? = null

    fun getDataController(): MediaDataController {
        return dataController
    }

    fun setDataController(controller: MediaDataController) {
        dataController = controller
        updateDataController()
    }

    private fun updateDataController() {
        mControlComponents.keys.forEach {
            it.setDataController(dataController)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.layout_player_standard_controller
    }

    override fun initView() {
        super.initView()
        tvPosition = findViewById(R.id.tvPosition)
        pipContainer = findViewById(R.id.pipContainer)
        lockContainer = findViewById(R.id.lockContainer)
        positionContainer = findViewById(R.id.positionContainer)
        ibPictureInPicture = findViewById(R.id.ibPictureInPicture)

        timedTextView = findViewById(R.id.timedTextView)
        tvDownloadSpeed = findViewById(R.id.tvDownloadSpeed)
        tvFastForward = findViewById<TextView>(R.id.tvFastForward).also {
            it.text = String.format(context.getString(R.string.player_fast_forward), 3.0)
        }

        ibLock = findViewById<ImageView?>(R.id.ibLock).also {
            it.setOnClickListener {
                controlWrapper.toggleLockState()
            }
        }

        setDataController(MediaDataController.with(System.currentTimeMillis().toString()))
        addDefaultComponents()
    }

    override fun onLockStateChanged(isLocked: Boolean) {
        super.onLockStateChanged(isLocked)
        ibLock.isSelected = isLocked
        tvPosition.isVisible = isLocked && controlWrapper.isFullScreen
        tvDownloadSpeed.isVisible = false
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        super.onVisibilityChanged(isVisible, anim)
        if (isVisible.not()) {
            tvDownloadSpeed.isVisible = false
        }
        if (controlWrapper.isFullScreen) {
            if (isLocked && isVisible) {
                if (tvPosition.isVisible.not()) {
                    tvPosition.isVisible = true
                    if (anim != null) {
                        tvPosition.startAnimation(anim);
                    }
                }
            } else {
                if (tvPosition.isVisible) {
                    tvPosition.isVisible = false
                    if (anim != null) {
                        tvPosition.startAnimation(anim);
                    }
                }
            }

            if (isVisible) {
                if (ibLock.isVisible.not()) {
                    ibLock.isVisible = true
                    if (anim != null) {
                        ibLock.startAnimation(anim);
                    }
                }
            } else {
                if (ibLock.isVisible) {
                    ibLock.isVisible = false
                    if (anim != null) {
                        ibLock.startAnimation(anim);
                    }
                }
            }
        }

        if (isVisible) {
            if (ibPictureInPicture.isVisible.not()) {
                ibPictureInPicture.isVisible = true
                if (anim != null) {
                    ibPictureInPicture.startAnimation(anim);
                }
            }
            applyCutout()
        } else {
            if (ibPictureInPicture.isVisible) {
                ibPictureInPicture.isVisible = false
                if (anim != null) {
                    ibPictureInPicture.startAnimation(anim);
                }
            }
        }
    }

    private var job: Job? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onPlayStateChanged(playState: Int) {
        super.onPlayStateChanged(playState)
        timedTextView.updateTimeText(null)
        when (playState) {
            VideoView.STATE_IDLE -> {
                ibLock.isVisible = false
                ibPictureInPicture.isVisible = false
            }

            VideoView.STATE_PLAYBACK_COMPLETED -> {
                ibLock.isVisible = false
                ibLock.isSelected = false
                ibPictureInPicture.isVisible = false
            }

            VideoView.STATE_PLAYING -> { //切换剧集后重新根据视频宽高切换横竖屏
                if (autoToggleScreenDirectionInFullscreen) {
                    job?.cancel()
                    job = CoroutineScope(Dispatchers.Main).launch {
                        while (isAttachedToWindow && controlWrapper.isPlaying) {
                            delay(500)
                            val size = controlWrapper.videoSize
                            val width = size[0]
                            val height = size[1]
                            if (width > 0 && height > 0) {
                                Log.i(
                                    "StandardVideoController",
                                    "isInPortrait = ${PlayerUtils.isInPortrait(context)}"
                                )
                                Log.i(
                                    "StandardVideoController",
                                    "controlWrapper.isFullScreen = ${controlWrapper.isFullScreen}"
                                )
                                Log.i("StandardVideoController", "videoSize = $width,$height")
                                if (controlWrapper.isFullScreen && width != height) {
                                    //如果是在竖屏全屏模式且视频是宽的则切换横屏
                                    if (width > height) {
                                        if (PlayerUtils.isNotInLandscape(context)) {
                                            Log.i(
                                                "StandardVideoController",
                                                "set SCREEN_ORIENTATION_LANDSCAPE"
                                            )
                                            PlayerUtils.scanForActivity(context)?.requestedOrientation =
                                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }
                                    } else {
                                        if (PlayerUtils.isNotInLandscape(context).not()) {
                                            Log.i(
                                                "StandardVideoController",
                                                "set SCREEN_ORIENTATION_PORTRAIT"
                                            )
                                            PlayerUtils.scanForActivity(context)?.requestedOrientation =
                                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        }
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        super.onPlayerStateChanged(playerState)
        when (playerState) {
            VideoView.PLAYER_NORMAL -> {
                ibLock.isVisible = false
                tvPosition.isVisible = false
            }

            VideoView.PLAYER_FULL_SCREEN -> {
                ibLock.isVisible = isShowing
                tvPosition.isVisible = isLocked && isShowing
            }
        }

        applyCutout()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCutout()
    }

    private fun applyCutout() {
        val activity = PlayerUtils.scanForActivity(context)
        if (activity != null && controlWrapper.hasCutout()) {
            val orientation = activity.requestedOrientation
            if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                pipContainer.setPadding(0, 0, 0, 0)
                lockContainer.setPadding(0, 0, 0, 0)
                positionContainer.setPadding(0, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                pipContainer.setPadding(0, 0, 0, 0)
                lockContainer.setPadding(controlWrapper.cutoutHeight, 0, 0, 0)
                positionContainer.setPadding(controlWrapper.cutoutHeight, 0, 0, 0)
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                pipContainer.setPadding(0, 0, controlWrapper.cutoutHeight, 0)
                lockContainer.setPadding(0, 0, 0, 0)
                positionContainer.setPadding(0, 0, controlWrapper.cutoutHeight, 0)
            }
        }

        danmakuView.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                if (PlayerUtils.isInPortrait(context) && controlWrapper.isFullScreen) {
                    setMargins(0, controlWrapper.cutoutHeight, 0, 0)
                } else {
                    setMargins(0, 0, 0, 0)
                }
            }
    }

    fun setCover(url: String) {
        mControlComponents.keys.forEach {
            if (it is CoverView) {
                it.setCover(url)
            }
        }
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        if (isLocked) {
            return
        }
        if (controlWrapper.isPlaying && controlWrapper.duration > 0) {
            if (controlWrapper.isShowing) {
                controlWrapper.hide()
            }
            isInFastForward = true
            tvFastForward.isVisible = true
            tvDownloadSpeed.isVisible = false
            lastSpeed = controlWrapper.speed
            controlWrapper.speed = fastForwardRate

            VibratorUtil(context).vibrateTo(60, 255)
        }
    }

    @SuppressLint("SetTextI18n")
    fun setDownloadSpeed(speed: Long) {
        if (speed <= 0) {
            tvDownloadSpeed.isVisible = false
            return
        }
        if (isInFastForward.not() && controlWrapper.isLocked.not() && controlWrapper.isShowing) {
            tvDownloadSpeed.isVisible = true
            tvDownloadSpeed.text = speed.toFileSizeString() + "/s"
        }
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        if (isInFastForward) {
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isInFastForward = false
                tvFastForward.isVisible = false
                controlWrapper.speed = lastSpeed
                return true
            }
        }
        return super.onTouch(v, event)
    }

    @SuppressLint("SetTextI18n")
    override fun setProgress(duration: Int, position: Int) {
        super.setProgress(duration, position)
        tvPosition.text = PlayerUtils.stringForTime(duration - position)
    }

    override fun onBackPressed(): Boolean {
        if (isLocked) {
            show()
            Toast.makeText(context, "请先解锁屏幕", Toast.LENGTH_SHORT).show()
            return true
        }
        if (controlWrapper.isFullScreen) {
            return stopFullScreen()
        }
        return super.onBackPressed()
    }

    override fun onTimedText(timedText: TimedText?) {
        super.onTimedText(timedText)
        timedTextView.updateTimeText(timedText)
    }

    fun setOnBackListener(listener: () -> Unit) {
        mControlComponents.keys.forEach {
            if (it is TitleView) {
                it.getIbBack().setOnClickListener {
                    listener.invoke()
                }
            }
        }
    }

    fun setPictureInPictureEnabled(onPipListener: () -> Unit) {
        pipContainer.isVisible = true
        ibPictureInPicture.setOnClickListener {
            onPipListener.invoke()
        }
    }

    fun setFullScreenButtonEnabled(enabled: Boolean) {
        this.isFullScreenButtonEnabled = enabled
        updateFullScreenButtonStatus()
    }

    private fun updateFullScreenButtonStatus() {
        mControlComponents.keys.filterIsInstance<ControlView>().forEach { view ->
            if (isFullScreenButtonEnabled) {
                view.getIbFullScreen().isEnabled = true
                view.getIbFullScreen().alpha = 1f
            } else {
                view.getIbFullScreen().isEnabled = false
                view.getIbFullScreen().alpha = 0.5f
            }
        }
        mControlComponents.keys.filterIsInstance<LiveControlView>().forEach { view ->
            if (isFullScreenButtonEnabled) {
                view.getIbFullScreen().isEnabled = true
                view.getIbFullScreen().alpha = 1f
            } else {
                view.getIbFullScreen().isEnabled = false
                view.getIbFullScreen().alpha = 0.5f
            }
        }
        mControlComponents.keys.filterIsInstance<LandscapeControlView>().forEach { view ->
            if (isFullScreenButtonEnabled) {
                view.getIbFullScreen().isEnabled = true
                view.getIbFullScreen().alpha = 1f
            } else {
                view.getIbFullScreen().isEnabled = false
                view.getIbFullScreen().alpha = 0.5f
            }
        }
    }

    fun startFullScreen(activity: Activity) {
        controlWrapper.toggleFullScreen(activity)
    }

    private fun addDefaultComponents() {
        removeAllControlComponent()
        addControlComponent(CoverView(context))
        addControlComponent(LoadView(context))
        addControlComponent(GestureView(context))
        addControlComponent(ErrorView(context))
        addControlComponent(CompleteView(context))
        addControlComponent(ScaleView(context))
        addControlComponent(SpeedView(context))
        addControlComponent(SelectView(context))
        addControlComponent(SubtitleView(context))
        addControlComponent(TrackView(context))

        addControlComponent(TitleView(context).apply {
            getIbSubtitle().setOnClickListener { anchor ->
                hide()
                stopFadeOut()
                mControlComponents.keys.forEach {
                    if (it is SubtitleView) {
                        it.show(anchor)
                    }
                }
            }
            getIbTrack().setOnClickListener { anchor ->
                hide()
                stopFadeOut()
                mControlComponents.keys.forEach {
                    if (it is TrackView) {
                        it.show(anchor)
                    }
                }
            }
        })

        danmakuView = MyDanmakuView(context)
        timedTextDanmakuView = MyTimedTextDanmakuView(context)

        addControlComponent(danmakuView)
        addControlComponent(timedTextDanmakuView)
        addControlComponent(ControlView(context))
        addControlComponent(createLandscapeControlView())
        updateDataController()
    }

    fun setIsLive(isLive: Boolean) {
        mControlComponents.keys.filter {
            it is LiveControlView || it is ControlView || it is LandscapeControlView
        }.forEach {
            removeControlComponent(it)
        }

        if (isLive) {
            addControlComponent(createLiveControlView())
            updateDataController()
            takeoverFullScreenLogic()
            updateFullScreenButtonStatus()
        } else {
            addControlComponent(ControlView(context))
            addControlComponent(createLandscapeControlView())
            updateDataController()
            takeoverFullScreenLogic()
            updateFullScreenButtonStatus()
        }
    }


    private fun createLandscapeControlView(): LandscapeControlView {
        return LandscapeControlView(context).also {
            it.getIbScale().setOnClickListener { anchor ->
                mControlComponents.keys.filterIsInstance<ScaleView>().forEach { component ->
                    component.show(anchor)
                }
            }
            it.getIbSpeed().setOnClickListener { anchor ->
                mControlComponents.keys.filterIsInstance<SpeedView>().forEach { component ->
                    component.show(anchor)
                }
            }
            it.getIbSelect().setOnClickListener { anchor ->
                mControlComponents.keys.filterIsInstance<SelectView>().forEach { component ->
                    component.show(anchor)
                }
            }
        }
    }

    private fun createLiveControlView(): LiveControlView {
        return LiveControlView(context).also {
            it.getIbScale().setOnClickListener { anchor ->
                mControlComponents.keys.filterIsInstance<ScaleView>().forEach { component ->
                    component.show(anchor)
                }
            }
            it.getIbSelect().setOnClickListener { anchor ->
                mControlComponents.keys.filterIsInstance<SelectView>().forEach { component ->
                    component.show(anchor)
                }
            }
        }
    }

    fun setLoadingText(text: String) {
        mControlComponents.keys.forEach {
            if (it is LoadView) {
                it.setLoadingText(text)
            }
        }
    }

    fun setErrorText(text: String, retry: () -> Unit) {
        mControlComponents.keys.forEach {
            if (it is ErrorView) {
                it.setErrorText(text, retry)
            }
        }
    }

    fun takeoverRetryLogic(block: () -> Unit) {
        takeoverRetryLogic = block
        takeoverRetryLogic()
    }

    private fun takeoverRetryLogic() {
        if (takeoverRetryLogic != null) {
            mControlComponents.keys.filterIsInstance<ErrorView>().forEach {
                it.takeoverRetryLogic(takeoverRetryLogic!!)
            }
        }
    }

    fun takeoverFullScreenLogic(block: ControlWrapper.() -> Unit) {
        takeoverFullScreenLogic = block
        takeoverFullScreenLogic()
    }

    private fun takeoverFullScreenLogic() {
        if (takeoverFullScreenLogic != null) {
            mControlComponents.keys.filterIsInstance<ControlView>().forEach {
                it.getIbFullScreen().setOnClickListener { button ->
                    if (controlWrapper.isFullScreen) {
                        button.animate().rotationBy(-360f).setDuration(300).withEndAction {
                            takeoverFullScreenLogic?.invoke(controlWrapper)
                        }
                    } else {
                        button.animate().rotationBy(360f).setDuration(300).withEndAction {
                            takeoverFullScreenLogic?.invoke(controlWrapper)
                        }
                    }
                }
            }
            mControlComponents.keys.filterIsInstance<LiveControlView>().forEach {
                it.getIbFullScreen().setOnClickListener { button ->
                    if (controlWrapper.isFullScreen) {
                        button.animate().rotationBy(-360f).setDuration(300).withEndAction {
                            takeoverFullScreenLogic?.invoke(controlWrapper)
                        }
                    } else {
                        button.animate().rotationBy(360f).setDuration(300).withEndAction {
                            takeoverFullScreenLogic?.invoke(controlWrapper)
                        }
                    }
                }
            }
            mControlComponents.keys.filterIsInstance<LandscapeControlView>().forEach {
                it.getIbFullScreen().setOnClickListener { button ->
                    if (controlWrapper.isFullScreen) {
                        button.animate().rotationBy(-360f).setDuration(300).withEndAction {
                            takeoverFullScreenLogic?.invoke(controlWrapper)
                        }
                    } else {
                        button.animate().rotationBy(360f).setDuration(300).withEndAction {
                            takeoverFullScreenLogic?.invoke(controlWrapper)
                        }
                    }
                }
            }
        }
    }

    fun setFastForwardRate(rate: Float) {
        this.fastForwardRate = rate
    }

    fun setAutoToggleScreenDirectionInFullscreen(enabled: Boolean) {
        this.autoToggleScreenDirectionInFullscreen = enabled
    }

    fun setTimedTextViewEnabled(enabled: Boolean) {
        this.timedTextView.isVisible = enabled
    }

    fun setDanmakuTimedTextEnabled(enabled: Boolean) {
        this.danmakuView.setTimedTextEnabled(enabled)
        this.timedTextDanmakuView.setTimedTextEnabled(enabled)
    }

    fun addDanmaku(danmaku: List<DanmakuEntity>) {
        danmaku.forEach {
            danmakuView.addDanmaku(it.text, it.size, it.color, it.type, it.time)
        }
    }

    fun addDanmaku(vararg danmakus: DanmakuEntity) {
        danmakus.forEach {
            danmakuView.addDanmaku(it.text, it.size, it.color, it.type, it.time)
        }
    }

    fun addDanmaku(text: CharSequence, isSelf: Boolean) {
        this.danmakuView.addDanmaku(text, isSelf)
    }

    fun addDanmaku(text: CharSequence, color: String) {
        this.danmakuView.addDanmaku(text, color)
    }

    fun addDanmaku(text: CharSequence, color: String, time: Long) {
        this.danmakuView.addDanmaku(text, color, time)
    }

    fun addDanmaku(
        text: CharSequence,
        color: String,
        type: Int = BaseDanmaku.TYPE_SCROLL_RL,
        time: Long
    ) {
        this.danmakuView.addDanmaku(text, color, type, time)
    }

    fun addDanmaku(
        text: CharSequence,
        size: Int,
        @ColorInt color: Int,
        type: Int = BaseDanmaku.TYPE_SCROLL_RL,
        time: Long
    ) {
        this.danmakuView.addDanmaku(text, size, color, type, time)
    }

    fun addDanmaku(text: CharSequence, @ColorRes colorRes: Int, time: Long) {
        this.danmakuView.addDanmaku(text, colorRes, time)
    }

    fun clearDanmaku() {
        this.danmakuView.clearDanmaku()
    }

    fun setDanmakuAlpha(alpha: Float) {
        this.danmakuView.alpha = alpha
    }

    fun setDanmakuEnabled(enabled: Boolean) {
        this.danmakuView.isVisible = enabled
    }

    fun getDanmakuView(): MyDanmakuView {
        return danmakuView
    }
}