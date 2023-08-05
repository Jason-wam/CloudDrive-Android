package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.OnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.jason.cloud.media3.utils.PlayerUtils
import com.jason.cloud.media3.utils.VibratorUtil
import kotlin.math.abs

class Media3GestureView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
    OnGestureListener, GestureDetector.OnDoubleTapListener, View.OnTouchListener {

    private lateinit var playerView: Media3PlayerView
    private var gestureDetector: GestureDetector
    private val audioManager: AudioManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(AudioManager::class.java)
    } else {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var brightness = 0f
    private var streamVolume = 0
    private var seekPosition = -1L

    private var isFirstTouch = false
    private var isChangePosition = false
    private var isChangeBrightness = false
    private var isChangeVolume = false
    private var inDoubleSpeedPlaying = false
    private var rememberSpeed = 1.0f
    private val doubleSpeedValue = 3.0f

    init {
        gestureDetector = GestureDetector(context, this)
        gestureDetector.setOnDoubleTapListener(this)
        setOnTouchListener(this)
    }

    fun attachPlayerView(view: Media3PlayerView) {
        this.playerView = view
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        isFirstTouch = true
        isChangePosition = false
        isChangeBrightness = false
        isChangeVolume = false
        streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        brightness =
            PlayerUtils.scanForActivity(context)?.window?.attributes?.screenBrightness ?: 0f
        return true
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, dx: Float, dy: Float): Boolean {
        if (playerView.isPlaying().not()) return true
        if (PlayerUtils.isEdge(context, e1)) return true
        if (playerView.isLocked) return true

        if (isFirstTouch) {
            isChangePosition = abs(dx) >= abs(dy)
            if (isChangePosition.not()) {
                //半屏宽度
                val halfScreen: Float = measuredWidth / 2f
                if (e2.x > halfScreen) {
                    isChangeVolume = true
                } else {
                    isChangeBrightness = true
                }
            }
            if (isChangePosition) {
                //根据用户设置是否可以滑动调节进度来决定最终是否可以滑动调节进度
                isChangePosition = true
            }
            playerView.onStartSlide()
            isFirstTouch = false
        }

        val deltaX = e1.x - e2.x
        val deltaY = e1.y - e2.y
        if (isChangePosition) {
            slideToChangePosition(deltaX)
        } else if (isChangeBrightness) {
            slideToChangeBrightness(deltaY)
        } else if (isChangeVolume) {
            slideToChangeVolume(deltaY)
        }
        return true
    }

    private fun slideToChangePosition(deltaX: Float) {
        val width = measuredWidth.toFloat()
        val duration = playerView.getDuration()
        val currentPosition = playerView.getCurrentPosition()
        var position = (-deltaX / width * 120000 + currentPosition).toLong()
        if (position > duration) {
            position = duration
        }
        if (position < 0) {
            position = 0
        }
        seekPosition = position
        val percent = (seekPosition / duration.toFloat() * 100).toInt()
        playerView.onSlidePosition(percent, position, duration)
    }

    private fun slideToChangeVolume(deltaY: Float) {
        val streamMaxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val height = measuredHeight
        val deltaV = deltaY * 2 / height * streamMaxVolume
        var index: Float = streamVolume + deltaV
        if (index > streamMaxVolume) {
            index = streamMaxVolume.toFloat()
        }
        if (index < 0) {
            index = 0f
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index.toInt(), 0)
        val percent = (index / streamMaxVolume * 100).toInt()
        playerView.onSlideVolume(percent)
    }

    private fun slideToChangeBrightness(deltaY: Float) {
        val activity = PlayerUtils.scanForActivity(context) ?: return
        val window = activity.window
        val attributes = window.attributes
        val height = measuredHeight
        if (brightness == -1.0f) brightness = 0.5f
        var brightness: Float = deltaY * 2 / height + brightness
        if (brightness < 0) {
            brightness = 0f
        }
        if (brightness > 1.0f) {
            brightness = 1.0f
        }
        attributes.screenBrightness = brightness
        window.attributes = attributes
        val percent = (brightness * 100).toInt()
        playerView.onSlideBrightness(percent)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (inDoubleSpeedPlaying) {
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                inDoubleSpeedPlaying = false
                playerView.setSpeed(rememberSpeed)
                playerView.onStopSlide()
                //取消倍速
                return true
            }
        }
        //滑动结束时事件处理
        if (!gestureDetector.onTouchEvent(event)) {
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    onStopSlide()
                    if (seekPosition >= 0) {
                        playerView.seekTo(seekPosition)
                        seekPosition = -1
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    onStopSlide()
                    seekPosition = -1
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun onStopSlide() {
        playerView.onStopSlide()
    }

    override fun onLongPress(e: MotionEvent) {
        if (playerView.isLocked) return
        if (playerView.isPlaying().not()) return
        if (playerView.getDuration() > 0) {
            rememberSpeed = playerView.getSpeed()
            inDoubleSpeedPlaying = true
            playerView.setSpeed(doubleSpeedValue)
            playerView.onDoubleSpeedPlaying(doubleSpeedValue)
            VibratorUtil(context).vibrateTo(60, 255)
        }
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, vX: Float, vY: Float): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        if (playerView.isControlViewVisible()) {
            playerView.hideControlView()
        } else {
            playerView.showControlView()
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (e.x < playerView.measuredWidth * 0.33f) {
            if (playerView.isPlaying().not()) return true
            playerView.onSeekBackward(10 * 1000)
            VibratorUtil(context).vibrateTo(60, 255)
        }
        if (e.x > playerView.measuredWidth * 0.33f * 2) {
            if (playerView.isPlaying().not()) return true
            playerView.onSeekForward(10 * 1000)
            VibratorUtil(context).vibrateTo(60, 255)
        }
        if (e.x in (playerView.measuredWidth * 0.33f..playerView.measuredWidth * 0.33f * 2)) {
            VibratorUtil(context).vibrateTo(60, 255)
            if (playerView.isPlaying()) {
                playerView.pause()
            } else {
                playerView.start()
            }
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }
}