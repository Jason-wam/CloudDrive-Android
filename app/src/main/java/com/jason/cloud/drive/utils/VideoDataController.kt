package com.jason.cloud.drive.utils

import android.util.Log
import com.jason.cloud.media3.interfaces.OnMediaItemTransitionListener
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3VideoItem
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.widget.Media3AudioPlayer

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-26 20:34
 * @Description: TODO
 */
open class VideoDataController : OnMediaItemTransitionListener, OnStateChangeListener {
    private var index: Int = 0
    private var player: Media3AudioPlayer? = null
    private val videoData: ArrayList<Media3VideoItem> = arrayListOf()
    private var onPlayListeners: ArrayList<OnPlayListener> = arrayListOf()
    private var onCompleteListeners: ArrayList<OnCompleteListener> = arrayListOf()
    private var videoName: String = ""

    override fun onStateChanged(state: Int) {
        if (state == Media3PlayState.STATE_ENDED) {
            if (hasNext().not()) {
//                onCompleteListeners.forEach {
//                    it.onCompletion()
//                }
            }
        }
    }

    override fun onTransition(index: Int) {
        onPlayListeners.forEach {
            it.onPlay(index, videoData[index])
        }
    }

    companion object {
        private val hashMap = HashMap<String, VideoDataController>()

        fun release() {
            hashMap.forEach { it.value.release() }
            hashMap.clear()
        }

        fun with(id: String): VideoDataController {
            return if (hashMap[id] == null) {
                hashMap[id] = VideoDataController()
                hashMap[id]!!
            } else {
                hashMap[id]!!
            }
        }
    }

    fun attachPlayer(player: Media3AudioPlayer): VideoDataController {
        this.player = player
        this.player?.addOnStateChangeListener(this)
        this.player?.addOnMediaItemTransitionListener(this)
        return this
    }

    fun getData(): List<Media3VideoItem> {
        return videoData
    }

    fun currentData(): Media3VideoItem {
        return videoData[index]
    }

    fun currentIndex(): Int {
        return index
    }

    fun getVideoName(): String {
        return videoName
    }

    fun setVideoName(name: String): VideoDataController {
        this.videoName = name
        return this
    }

    fun setData(url: String): VideoDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.add(Media3VideoItem.create(url, url))
        this.player?.setDataSource(Media3VideoItem.create(url, url))
        return this
    }

    fun setData(name: String, url: String): VideoDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.add(Media3VideoItem.create(name, url))
        this.player?.setDataSource(Media3VideoItem.create(name, url))
        return this
    }

    fun setData(video: Media3VideoItem): VideoDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.add(video)
        this.player?.setDataSource(video)
        return this
    }

    fun setData(list: List<Media3VideoItem>): VideoDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.addAll(list)
        this.player?.setDataSource(list)
        Log.e("VideoDataController", "${this.player?.getMediaItemCount()}")
        return this
    }

    interface OnPlayListener {
        fun onPlay(position: Int, videoData: Media3VideoItem)
    }

    fun addOnPlayListener(listener: OnPlayListener): VideoDataController {
        this.onPlayListeners.add(listener)
        return this
    }

    fun removeOnPlayListener(listener: OnPlayListener? = null): VideoDataController {
        listener?.let {
            this.onPlayListeners.remove(it)
        }
        return this
    }

    interface OnCompleteListener {
        fun onCompletion()
    }

    fun addOnCompleteListener(listener: OnCompleteListener): VideoDataController {
        this.onCompleteListeners.add(listener)
        return this
    }

    fun removeOnCompleteListener(listener: OnCompleteListener): VideoDataController {
        this.onCompleteListeners.remove(listener)
        return this
    }

    fun hasNext(): Boolean {
        return player?.hasNextMediaItem() == true
    }

    fun hasPrevious(): Boolean {
        return player?.hasPreviousMediaItem() == true
    }

    fun next() {
        if (player?.hasNextMediaItem() == true) {
            player?.seekToNext()
            player?.prepare()
            player?.start()
        }
    }

    fun previous() {
        if (player?.hasPreviousMediaItem() == true) {
            player?.seekToNext()
            player?.prepare()
            player?.start()
        }
    }

    fun setIndex(index: Int) {
        this.index = index
    }

    fun start() {
        if (index !in videoData.indices) {
            Log.e("VideoDataController", "index out of video list!")
        } else {
            println("VideoDataController.invoke.onPlayListeners = ${onPlayListeners.size}")
            player?.seekToItem(index, 0)
            player?.prepare()
            player?.start()
            onPlayListeners.forEach {
                it.onPlay(index, videoData[index])
            }
        }
    }

    fun start(index: Int) {
        this.index = index
        this.start()
    }

    fun release() {
        this.index = 0
        this.videoData.clear()
        this.onPlayListeners.clear()
        this.onCompleteListeners.clear()
        this.player?.clearOnStateChangeListener()
        this.player = null
        this.videoName = ""
    }
}