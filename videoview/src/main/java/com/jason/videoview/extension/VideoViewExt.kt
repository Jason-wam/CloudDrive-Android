package com.jason.videoview.extension

import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.jason.videoview.view.ExVideoView
import xyz.doikki.videoplayer.player.BaseVideoView
import xyz.doikki.videoplayer.player.BaseVideoView.SimpleOnStateChangeListener
import xyz.doikki.videoplayer.player.VideoView

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-13 20:14
 * @Description: TODO
 */
fun BaseVideoView<*>.bindLifecycle(owner: LifecycleOwner) {
    bindLifecycle(owner.lifecycle)
}

fun BaseVideoView<*>.bindLifecycleWithPIPMode(
    activity: AppCompatActivity,
    backgroundEnabled: Boolean = false
) {
    var isPausedByUser = true
    addOnStateChangeListener(object : BaseVideoView.SimpleOnStateChangeListener() {
        override fun onPlayStateChanged(playState: Int) {
            super.onPlayStateChanged(playState)
            when (playState) {
                VideoView.STATE_PLAYING -> {
                    isPausedByUser = true
                }
            }
        }
    })

    activity.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (isPlaying) {
                    isPausedByUser = false
                }
                if (currentPlayState == VideoView.STATE_PREPARING) {
                    release()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (activity.isInPictureInPictureMode) { //如果处于画中画模式则返回
                        return
                    }
                }
                if (backgroundEnabled.not()) {
                    pause()
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (activity.isInPictureInPictureMode) { //如果处于画中画模式则返回
                        release()
                    }
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (isPausedByUser.not() && currentPlayState == VideoView.STATE_PAUSED) {
                    resume()
                }
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                release()
            }
        }
    })
}

fun BaseVideoView<*>.bindLifecycle(lifecycle: Lifecycle, backgroundEnabled: Boolean = false) {
    var isPausedByUser = true
    addOnStateChangeListener(object : BaseVideoView.SimpleOnStateChangeListener() {
        override fun onPlayStateChanged(playState: Int) {
            super.onPlayStateChanged(playState)
            when (playState) {
                VideoView.STATE_PLAYING -> {
                    isPausedByUser = true
                }
            }
        }
    })

    lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (isPlaying) {
                    isPausedByUser = false
                }
                if (currentPlayState == VideoView.STATE_PREPARING) {
                    release()
                }
                if (backgroundEnabled.not()) {
                    pause()
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (isPausedByUser.not() && currentPlayState == VideoView.STATE_PAUSED) {
                    resume()
                }
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                release()
            }
        }
    })
}

fun ExVideoView.enableAutoSize(videoLayout: View, enabled: Boolean) {
    setOnSizeChangedListener { width, height ->
        if (enabled && !isFullScreen) {
            val scale = width / height.toFloat()
            val minScale = 1920 / 1080f
            if (width > 0 && height > 0 && scale < minScale) {
                videoLayout.scaleByWidth(videoSize[0], videoSize[1])
            } else {
                videoLayout.scaleByWidth(1920, 1080)
            }
        }
    }

    setOnStateChangedListener(object : BaseVideoView.SimpleOnStateChangeListener() {
        override fun onPlayStateChanged(playState: Int) {
            super.onPlayStateChanged(playState)
            if (enabled && !isFullScreen) {
                when (playState) {
                    VideoView.STATE_PREPARING, VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED -> {
                        videoLayout.scaleByWidth(1920, 1080)
                    }
                }
            }
        }
    })
}

fun BaseVideoView<*>.onPlayStateChanged(block: (Int) -> Unit) {
    addOnStateChangeListener(object : SimpleOnStateChangeListener() {
        override fun onPlayStateChanged(playState: Int) {
            super.onPlayStateChanged(playState)
            block.invoke(playState)
        }
    })
}

private fun View.scaleByWidth(wScale: Int, hScale: Int, block: (View.() -> Unit)? = null) {
    post {
        val params = layoutParams
        val scaleSize = (width.toFloat() * hScale / wScale).toInt()
        if (scaleSize != 0) {
            params.height = scaleSize
            layoutParams = params
        }
        block?.invoke(this)
    }
}