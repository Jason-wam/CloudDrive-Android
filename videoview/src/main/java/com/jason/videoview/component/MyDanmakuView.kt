package com.jason.videoview.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.view.animation.Animation
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.util.CenteredImageSpan
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class MyDanmakuView(context: Context) : DanmakuView(context), IControlComponent {
    private var mParser: BaseDanmakuParser? = null
    private val mContext: DanmakuContext = DanmakuContext.create()
    private var showTimedText: Boolean = false
    private lateinit var mControlWrapper: ControlWrapper

    init {
        val activity = PlayerUtils.scanForActivity(context)
        if (activity != null) {
            val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.refreshRate ?: activity.windowManager.defaultDisplay.refreshRate
            } else {
                activity.windowManager.defaultDisplay.refreshRate
            }
            val rate = (1000 / refreshRate).toInt()
            mContext.frameUpdateRate = rate.toLong()
            Log.e(TAG, "FrameUpdateRate : $refreshRate >> $rate")
        } else {
            mContext.frameUpdateRate = 8
        }

        alpha = 0.8f
        mContext.setDanmakuBold(true)
        mContext.setDanmakuStyle(
            IDisplayer.DANMAKU_STYLE_STROKEN,
            PlayerUtils.dp2px(context, 1.2f).toFloat()
        )

        mContext.isDuplicateMergingEnabled = false
        mContext.setScrollSpeedFactor(1.0f)
        mContext.setScaleTextSize(1.0f)
        // 设置最大显示行数
//        mContext.setMaximumLines(HashMap<Int, Int>().apply {
//            this[BaseDanmaku.TYPE_SCROLL_RL] = 5 // 滚动弹幕最大显示5行
//        })
        mContext.setMaximumLines(null)
        mContext.setMaximumVisibleSizeInScreen(-1)
        // 设置是否禁止重叠
        mContext.preventOverlapping(HashMap<Int, Boolean>().apply {
            this[BaseDanmaku.TYPE_SCROLL_RL] = false
            this[BaseDanmaku.TYPE_FIX_TOP] = true
            this[BaseDanmaku.TYPE_FIX_BOTTOM] = true
        })
        mContext.setDanmakuMargin(40)
        mParser = object : BaseDanmakuParser() {
            override fun parse(): IDanmakus {
                return Danmakus()
            }
        }

        showFPS(false)
        enableDanmakuDrawingCache(true)
        val uiHandler = Handler(Looper.getMainLooper())
        setCallback(object : DrawHandler.Callback {
            override fun updateTimer(timer: DanmakuTimer) {
                uiHandler.postAtFrontOfQueue {
                    //解决闪烁问题，保持最新的任务
                    timer.update(mControlWrapper.currentPosition)
                }
            }

            override fun drawingFinished() {
                //弹幕绘制完成时回掉
            }

            override fun danmakuShown(danmaku: BaseDanmaku) {
                //弹幕展示的时候回掉
            }

            override fun prepared() {
                //弹幕准备好的时候回掉，这里启动弹幕
                start()
            }
        })
    }

    override fun attach(controlWrapper: ControlWrapper) {
        mControlWrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation) {

    }

    override fun onPlayStateChanged(playState: Int) {
        when (playState) {
            VideoView.STATE_PREPARED -> {
                if (isPaused) {
                    return
                }
                start()
            }

            VideoView.STATE_IDLE -> /*release()*/ clearDanmaku() //release会导致切换剧集卡死
            VideoView.STATE_PREPARING -> {
                if (isPrepared) {
                    restart()
                }
                prepare(mParser, mContext)
            }

            VideoView.STATE_PLAYING -> if (isPrepared) {
                if (isPaused) {
                    resume()
                    Log.i("MyDanmakuView", "resume")
                } else {
                    mControlWrapper.currentPosition.let {
                        start(it)
                        Log.i("MyDanmakuView", "start $it")
                    }
                }
            }

            VideoView.STATE_PAUSED, VideoView.STATE_BUFFERING -> if (isPrepared) {
                pause()
            }

            VideoView.STATE_BUFFERED -> if (isPrepared) {
                mControlWrapper.let {
                    seekTo(it.currentPosition)
                    if (it.isPlaying) {
                        resume()
                    }
                }
            }

            VideoView.STATE_PLAYBACK_COMPLETED -> {
                clear()
                clearDanmakusOnScreen()
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {

    }


    override fun setProgress(duration: Int, position: Int) {

    }

    override fun onLockStateChanged(isLocked: Boolean) {}

    override fun onTimedText(timedText: TimedText) {
        //addTimedTextDanmaku(timedText)
    }

    fun addDanmaku(text: CharSequence, isSelf: Boolean) {
        mContext.setCacheStuffer(SpannedCacheStuffer(), null)
        val danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL) ?: return
        danmaku.text = text
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false
        danmaku.time = currentTime
        danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
        danmaku.textColor = if (isSelf) Color.YELLOW else Color.WHITE
        danmaku.textShadowColor = Color.BLACK
        //danmaku.underlineColor = Color.TRANSPARENT;
        danmaku.borderColor = Color.TRANSPARENT
        danmaku.padding = PlayerUtils.dp2px(context, 2f)
        addDanmaku(danmaku)
        clearDanmaku()
    }

    fun addDanmaku(text: CharSequence, color: String) {
        mContext.setCacheStuffer(SpannedCacheStuffer(), null)
        val danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL) ?: return
        danmaku.text = text
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false
        danmaku.time = currentTime
        danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
        danmaku.textColor = Color.parseColor(color)
        danmaku.textShadowColor = Color.BLACK
        //danmaku.underlineColor = Color.TRANSPARENT;
        danmaku.borderColor = Color.TRANSPARENT
        danmaku.padding = PlayerUtils.dp2px(context, 2f)
        addDanmaku(danmaku)
    }

    fun addDanmaku(text: CharSequence, color: String, time: Long) {
        mContext.setCacheStuffer(SpannedCacheStuffer(), null)
        val danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_TOP) ?: return
        danmaku.text = text
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false
        danmaku.time = time
        danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
        danmaku.textColor = Color.parseColor(color)
        danmaku.textShadowColor = Color.BLACK
        //danmaku.underlineColor = Color.TRANSPARENT;
        danmaku.borderColor = Color.TRANSPARENT
        danmaku.padding = PlayerUtils.dp2px(context, 2f)
        danmaku.alpha
        addDanmaku(danmaku)
    }

    fun addDanmaku(
        text: CharSequence,
        color: String,
        type: Int = BaseDanmaku.TYPE_SCROLL_RL,
        time: Long
    ) {
        mContext.setCacheStuffer(SpannedCacheStuffer(), null)
        val danmaku = mContext.mDanmakuFactory.createDanmaku(type) ?: return
        danmaku.text = text
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false
        danmaku.time = time
        danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
        danmaku.textColor = Color.parseColor(color)
        danmaku.textShadowColor = Color.BLACK
        //danmaku.underlineColor = Color.TRANSPARENT;
        danmaku.borderColor = Color.TRANSPARENT
        danmaku.padding = PlayerUtils.dp2px(context, 2f)
        addDanmaku(danmaku)
    }

    fun addDanmaku(
        text: CharSequence,
        @ColorInt color: Int,
        type: Int = BaseDanmaku.TYPE_SCROLL_RL,
        time: Long
    ) {
        mContext.setCacheStuffer(SpannedCacheStuffer(), null)
        val danmaku = mContext.mDanmakuFactory.createDanmaku(type) ?: return
        danmaku.text = text
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false
        danmaku.time = time
        danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
        danmaku.textColor = color
        danmaku.textShadowColor = Color.BLACK
        //danmaku.underlineColor = Color.TRANSPARENT;
        danmaku.borderColor = Color.TRANSPARENT
        danmaku.padding = PlayerUtils.dp2px(context, 2f)
        addDanmaku(danmaku)
    }

    fun addDanmaku(
        text: CharSequence,
        size: Int = 13,
        @ColorInt color: Int,
        type: Int = BaseDanmaku.TYPE_SCROLL_RL,
        time: Long
    ) {
        mContext.setCacheStuffer(SpannedCacheStuffer(), null)
        val danmaku = mContext.mDanmakuFactory.createDanmaku(type) ?: return
        danmaku.text = text
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false
        danmaku.time = time
        danmaku.textSize = PlayerUtils.sp2px(context, size.toFloat()).toFloat()
        danmaku.textColor = color
        danmaku.textShadowColor = Color.BLACK
        //danmaku.underlineColor = Color.TRANSPARENT;
        danmaku.borderColor = Color.TRANSPARENT
        danmaku.padding = PlayerUtils.dp2px(context, 2f)
        addDanmaku(danmaku)
    }

    fun addDanmaku(text: CharSequence, @ColorRes colorRes: Int, time: Long) {
        mContext.setCacheStuffer(SpannedCacheStuffer(), null)
        val danmaku = mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_TOP) ?: return
        danmaku.text = text
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = false
        danmaku.time = time
        danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
        danmaku.textColor = ContextCompat.getColor(context, colorRes)
        danmaku.textShadowColor = Color.BLACK
        //danmaku.underlineColor = Color.TRANSPARENT;
        danmaku.borderColor = Color.TRANSPARENT
        danmaku.padding = PlayerUtils.dp2px(context, 2f)
        addDanmaku(danmaku)
    }

    fun clearDanmaku() {
        if (handler != null) {
            handler.removeAllDanmakus(true)
        } else {
            Log.e(TAG, "clearDanmaku： ignore")
        }
    }

    private fun addTimedTextDanmaku(timedText: TimedText) {
        if (showTimedText.not()) {
            return
        }
        if (timedText.bitmap != null) {
            mContext.setCacheStuffer(SpannedCacheStuffer(), null)
            val danmaku =
                mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_BOTTOM) ?: return
            val drawable = BitmapDrawable(resources, timedText.bitmap)
            if (timedText.bitmapHeight != -Float.MAX_VALUE) {
                val bitmapHeight = timedText.bitmapHeight * height
                val scale = drawable.intrinsicWidth * (bitmapHeight / drawable.intrinsicHeight)
                drawable.setBounds(0, 0, scale.toInt(), bitmapHeight.toInt())
            } else {
                drawable.setBounds(0, 0, timedText.bitmap.width, timedText.bitmap.height)
            }

            danmaku.text = createSpannable(drawable)
            danmaku.priority = 100 // 可能会被各种过滤器过滤并隐藏显示
            danmaku.isLive = false
            danmaku.time = currentTime
            danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
            danmaku.textColor = Color.WHITE
            danmaku.textShadowColor = Color.BLACK
            //danmaku.underlineColor = Color.TRANSPARENT;
            danmaku.borderColor = Color.TRANSPARENT
            danmaku.padding = PlayerUtils.dp2px(context, 2f)
            addDanmaku(danmaku)
        }

        if (timedText.text?.isNotBlank() == true) {
            mContext.setCacheStuffer(SpannedCacheStuffer(), null)
            val danmaku =
                mContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_FIX_BOTTOM) ?: return
            danmaku.text = timedText.text.toString()
            danmaku.priority = 100 // 可能会被各种过滤器过滤并隐藏显示
            danmaku.isLive = false
            danmaku.time = currentTime
            danmaku.textSize = PlayerUtils.sp2px(context, 14f).toFloat()
            danmaku.textColor = Color.WHITE
            danmaku.textShadowColor = Color.BLACK
            //danmaku.underlineColor = Color.TRANSPARENT;
            danmaku.borderColor = Color.TRANSPARENT
            danmaku.padding = PlayerUtils.dp2px(context, 2f)
            addDanmaku(danmaku)
            removeAllLiveDanmakus()
        }
    }

    private fun createSpannable(drawable: Drawable): SpannableStringBuilder {
        val text = "bitmap"
        val spannableStringBuilder = SpannableStringBuilder(text)
        val span = CenteredImageSpan(drawable) //ImageSpan.ALIGN_BOTTOM);
        spannableStringBuilder.setSpan(span, 0, text.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannableStringBuilder
    }


    fun setTimedTextEnabled(showTimedText: Boolean) {
        this.showTimedText = showTimedText
    }

    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
    }
}