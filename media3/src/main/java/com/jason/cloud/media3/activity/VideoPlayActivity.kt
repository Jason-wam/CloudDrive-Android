package com.jason.cloud.media3.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.jason.cloud.media3.R
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.utils.MediaPositionStore
import com.jason.cloud.media3.utils.PlayerUtils
import com.jason.cloud.media3.widget.Media3PlayerView
import java.io.Serializable

class VideoPlayActivity : AppCompatActivity() {
    private var pausedByUser = false
    private val playerView: Media3PlayerView by lazy {
        findViewById(R.id.player_view)
    }

    companion object {
        var positionStore: MediaPositionStore? = null

        /**
         * 如果需要记忆播放则需要每次open前设置MediaPositionStore
         */
        fun open(context: Context?, title: String, url: String, useCache: Boolean = false) {
            context?.startActivity(Intent(context, VideoPlayActivity::class.java).apply {
                putExtra("url", url)
                putExtra("title", title)
                putExtra("useCache", useCache)
            })
        }

        /**
         * 如果需要记忆播放则需要每次open前设置MediaPositionStore
         */
        fun open(context: Context?, item: Media3Item) {
            context?.startActivity(Intent(context, VideoPlayActivity::class.java).apply {
                putExtra("item", item)
            })
        }

        /**
         * 如果需要记忆播放则需要每次open前设置MediaPositionStore
         */
        fun open(
            context: Context?, videoData: List<Media3Item>, position: Int = 0
        ) {
            context?.startActivity(Intent(context, VideoPlayActivity::class.java).apply {
                putExtra("videoData", videoData as Serializable)
                putExtra("position", position)
            })
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

    private var rememberOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rememberOrientation = requestedOrientation
        adaptCutoutAboveAndroidP()
        setContentView(R.layout.activity_video_play)

        playerView.setPositionStore(positionStore)
        playerView.getStatusView().layoutParams.height =
            PlayerUtils.getStatusBarHeight(this).toInt()
        playerView.addOnStateChangeListener(object : OnStateChangeListener {
            override fun onStateChanged(state: Int) {
                Log.i("VideoPlayActivity", "onStateChanged > $state")
                when (state) {
                    Media3PlayState.STATE_PLAYING -> pausedByUser = false
                    Media3PlayState.STATE_PAUSED -> pausedByUser = true
                }
            }
        })

        playerView.onRequestScreenOrientationListener {
            if (it) {
                if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            } else {
                if (requestedOrientation != rememberOrientation) {
                    requestedOrientation = rememberOrientation
                }
            }
        }

        val url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")
        val useCache = intent.getBooleanExtra("useCache", false)
        if (url?.isNotBlank() == true && title?.isNotBlank() == true) {
            playerView.setDataSource(Media3Item.create(title, url, useCache))
            playerView.prepare()
            playerView.start()
            return
        }

        val item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("item", Media3Item::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("item")?.let {
                it as Media3Item
            }
        }
        if (item != null) {
            playerView.setDataSource(item)
            playerView.prepare()
            playerView.start()
            return
        }

        val position = intent.getIntExtra("position", 0)
        val videoData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("UNCHECKED_CAST")
            intent.getSerializableExtra("videoData", Serializable::class.java)
                ?.let { it as List<Media3Item> } ?: emptyList()
        } else {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            intent.getSerializableExtra("videoData")?.let {
                it as List<Media3Item>
            }
        }
        if (videoData?.isNotEmpty() == true) {
            playerView.setDataSource(videoData)
            playerView.prepare()
            playerView.seekToDefaultPosition(position)
            playerView.start()
        }
    }

    override fun onStart() {
        super.onStart()
        if (pausedByUser.not()) {
            playerView.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (playerView.isPlaying()) {
            playerView.pause()
            pausedByUser = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.release()
        positionStore = null
    }
}