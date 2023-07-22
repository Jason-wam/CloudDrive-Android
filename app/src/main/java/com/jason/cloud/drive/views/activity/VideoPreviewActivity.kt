package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jason.cloud.extension.getSerializableExtraEx
import com.jason.cloud.extension.putSerializableListExtra
import com.jason.cloud.extension.startActivity
import com.jason.cloud.extension.toast
import com.jason.exo.extension.ExoMediaPlayerFactory
import com.jason.videoview.StandardVideoController
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.extension.bindLifecycle
import com.jason.videoview.model.VideoData
import com.jason.videoview.util.VideoProgressManager
import xyz.doikki.videoplayer.player.VideoView

class VideoPreviewActivity : AppCompatActivity(), MediaDataController.OnPlayListener {
    private var proxy: Boolean = false

    companion object {
        fun open(context: Context?, title: String, url: String) {
            context?.startActivity(VideoPreviewActivity::class) {
                putExtra("url", url)
                putExtra("title", title)
            }
        }

        fun open(context: Context?, position: Int = 0, videoData: List<VideoData>) {
            context.startActivity(VideoPreviewActivity::class) {
                putExtra("position", position)
                putSerializableListExtra("videoData", videoData)
            }
        }
    }

    private val videoView by lazy {
        VideoView(this)
    }

    private val controller: StandardVideoController by lazy {
        StandardVideoController(this).apply {
            setTimedTextViewEnabled(false)
            setDanmakuTimedTextEnabled(true) //            setFullScreenButtonEnabled(false)
            setFastForwardRate(3.0f)
            setAutoToggleScreenDirectionInFullscreen(true)
            setEnableOrientation(false)
            setDataController(MediaDataController.with("VideoPreviewActivity"))
            takeoverFullScreenLogic {
                toggleFullScreen(this@VideoPreviewActivity, true)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        setContentView(videoView)

        val url = intent.getStringExtra("url").orEmpty()
        val title = intent.getStringExtra("title").orEmpty()

        val isLive = intent.getBooleanExtra("isLive", false)
        val history = intent.getBooleanExtra("history", true)
        val position = intent.getIntExtra("position", 0)

        proxy = intent.getBooleanExtra("proxy", false)

        if (url.isNotBlank()) {
            MediaDataController.with("VideoPreviewActivity").setData(title, url)
        }

        intent.getSerializableExtraEx("videoData", ArrayList::class.java)?.let {
            MediaDataController.with("VideoPreviewActivity")
                .setData(it as ArrayList<VideoData>)
            MediaDataController.with("VideoPreviewActivity").setIndex(position)
        }

        enterFullScreen()
        adaptCutoutAboveAndroidP()

        controller.setIsLive(isLive)
        controller.setOnBackListener {
            MediaDataController.with("VideoPreviewActivity").release()
            finish()
        }

        if (history) {
            videoView.setProgressManager(VideoProgressManager())
        }

        videoView.startFullScreen()
        videoView.setPlayerFactory(ExoMediaPlayerFactory.create(false))
        videoView.bindLifecycle(this)
        videoView.setVideoController(controller)
        videoView.clearOnStateChangeListeners()

        MediaDataController.with("VideoPreviewActivity").setVideoView(videoView)
        MediaDataController.with("VideoPreviewActivity").addOnPlayListener(this)
        MediaDataController.with("VideoPreviewActivity").start()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (controller.isLocked) {
                    toast("请先解锁屏幕！")
                } else {
                    finish()
                }
            }
        })
    }

    private fun enterFullScreen() {
        WindowInsetsControllerCompat(window, window.decorView).also {
            it.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun adaptCutoutAboveAndroidP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaDataController.with("VideoPreviewActivity").release()
    }

    override fun onPlay(position: Int, videoData: VideoData) {
        videoView.release()
        videoView.setUrl(videoData.url)
        videoView.start()
    }
}