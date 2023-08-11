package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jason.cloud.media3.interfaces.OnMediaItemTransitionListener
import com.jason.cloud.media3.interfaces.OnPlayCompleteListener
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.utils.FfmpegRenderersFactory
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.utils.Media3SourceHelper

@SuppressLint("UnsafeOptInUsageError")
class Media3AudioPlayer(context: Context) {
    private val mediaSourceHelper by lazy {
        Media3SourceHelper.newInstance(context.applicationContext)
    }
    internal val internalPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setRenderersFactory(FfmpegRenderersFactory(context))
            .build()
    }

    var currentPlayState = Media3PlayState.STATE_IDLE
    private var speedPlaybackParameters: PlaybackParameters? = null
    private var onPlayStateListeners = ArrayList<OnStateChangeListener>()
    private var onMediaItemTransitionListeners = ArrayList<OnMediaItemTransitionListener>()
    private var onPlayCompleteListeners = ArrayList<OnPlayCompleteListener>()

    private val playerListener: Player.Listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            //相当于暂停继续
            if (isPlaying) {
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

        @SuppressLint("SetTextI18n")
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            onPlayStateListeners.forEach {
                it.onStateChanged(playbackState)
            }
            if (playbackState == Player.STATE_ENDED) {
                onPlayCompleteListeners.forEach { it.onCompletion() }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            Log.e("AudioPlayer", "transition = ${internalPlayer.getCurrentMedia3Item()?.title}")
            Log.e("AudioPlayer", "transition = $reason")
            onMediaItemTransitionListeners.forEach {
                it.onTransition(
                    internalPlayer.currentMediaItemIndex,
                    internalPlayer.getCurrentMedia3Item()!!
                )
            }
        }
    }

    init {
        internalPlayer.addListener(playerListener)
    }

    fun setDataSource(item: Media3Item) {
        internalPlayer.setMediaSource(mediaSourceHelper.getMediaSource(item))
    }

    fun setDataSource(path: String) {
        internalPlayer.setMediaSource(mediaSourceHelper.getMediaSource(path, false))
    }

    fun setDataSource(path: String, headers: Map<String, String>? = null) {
        internalPlayer.setMediaSource(mediaSourceHelper.getMediaSource(path, headers, false))
    }

    fun addDataSource(itemList: List<Media3Item>) {
        internalPlayer.addMediaSource(mediaSourceHelper.getMediaSource(itemList))
    }

    fun setDataSource(itemList: List<Media3Item>) {
        internalPlayer.setMediaSource(mediaSourceHelper.getMediaSource(itemList))
    }

    fun start() {
        internalPlayer.playWhenReady = true
    }

    fun pause() {
        internalPlayer.playWhenReady = false
        currentPlayState = Media3PlayState.STATE_PAUSED
        onPlayStateListeners.forEach {
            it.onStateChanged(Media3PlayState.STATE_PAUSED)
        }
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

    fun seekTo(time: Long) {
        internalPlayer.seekTo(time)
    }

    fun seekToDefaultPosition(mediaItemIndex: Int) {
        internalPlayer.seekToDefaultPosition(mediaItemIndex)
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
        internalPlayer.removeListener(playerListener)
        internalPlayer.release()
    }

    fun getCurrentMediaItemIndex(): Int {
        return internalPlayer.currentMediaItemIndex
    }

    fun getMediaItemCount(): Int {
        return internalPlayer.mediaItemCount
    }

    fun getCurrentMedia3Item(): Media3Item? {
        return internalPlayer.getCurrentMedia3Item()
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

    fun clearOnPlayCompleteListener() {
        this.onPlayCompleteListeners.clear()
    }

    fun removeOnPlayCompleteListener(listener: OnPlayCompleteListener) {
        this.onPlayCompleteListeners.remove(listener)
    }

    fun addOnPlayCompleteListener(listener: OnPlayCompleteListener) {
        this.onPlayCompleteListeners.add(listener)
    }
}