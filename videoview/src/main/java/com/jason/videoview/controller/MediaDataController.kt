package com.jason.videoview.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jason.videoview.model.VideoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.doikki.videoplayer.player.BaseVideoView
import xyz.doikki.videoplayer.player.VideoView
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.math.abs

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-26 20:34
 * @Description: TODO
 */
open class MediaDataController : SensorEventListener {
    private var videoView: BaseVideoView<*>? = null
    private val videoData: ArrayList<VideoData> = arrayListOf()
    private var onPlayListeners: ArrayList<OnPlayListener> = arrayListOf()
    private var onCompleteListeners: ArrayList<OnCompleteListener> = arrayListOf()
    private var onFullScreenListeners: ArrayList<OnFullScreenListener> = arrayListOf()
    private var onDataChangedListeners: ArrayList<OnDataChangedListener> = arrayListOf()
    private val onCallTimedStopListeners: ArrayList<OnCallTimedStopListener> = arrayListOf()

    private var index: Int = 0
    private var videoName: String = ""
    private var orderMode: OrderMode = OrderMode.Sequential
    private var timedStopDuration: Long = -1
    private var timedStopJob: Job? = null

    private var sensorManager: SensorManager? = null

    enum class OrderMode {
        Random, ListLoop, Sequential, Loop
    }

    private val onStateChangeListener = object : BaseVideoView.SimpleOnStateChangeListener() {
        private var isPreparing = false

        override fun onPlayerStateChanged(playerState: Int) {
            super.onPlayerStateChanged(playerState)
            onFullScreenListeners.forEach {
                it.invoke(playerState == VideoView.PLAYER_FULL_SCREEN)
            }
        }

        override fun onPlayStateChanged(playState: Int) {
            super.onPlayStateChanged(playState)
            if (playState == VideoView.STATE_PLAYBACK_COMPLETED) {
                if (isPreparing.not()) {
                    isPreparing = true
                    Handler(Looper.getMainLooper()).postDelayed({ isPreparing = false }, 500)
                    when (orderMode) {
                        OrderMode.Loop -> start()

                        OrderMode.Random -> {
                            index = (0..videoData.size).random()
                            start()
                        }

                        OrderMode.ListLoop -> {
                            if (index + 1 in videoData.indices) {
                                index += 1
                                start()
                            } else {
                                index = 0
                                start()
                            }
                        }

                        OrderMode.Sequential -> {
                            if (index + 1 in videoData.indices) {
                                index += 1
                                start()
                            } else {
                                onCompleteListeners.forEach {
                                    it.onCompletion()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val hashMap = HashMap<String, MediaDataController>()

        fun release() {
            hashMap.forEach { it.value.release() }
            hashMap.clear()
        }

        fun with(id: String): MediaDataController {
            return if (hashMap[id] == null) {
                hashMap[id] = MediaDataController()
                hashMap[id]!!
            } else {
                hashMap[id]!!
            }
        }
    }

    fun setVideoView(videoView: BaseVideoView<*>): MediaDataController {
        this.videoView = videoView
        this.videoView?.addOnStateChangeListener(onStateChangeListener)
        return this
    }

    fun getData(): List<VideoData> {
        return videoData
    }

    fun getDataCursor(): Pair<Int, Int> {
        return Pair(index, videoData.size)
    }

    fun getDataCount(): Int {
        return videoData.size
    }

    fun currentData(): VideoData? {
        return if (index in videoData.indices) videoData[index] else null
    }

    fun currentIndex(): Int {
        return index
    }

    fun getVideoName(): String {
        return videoName
    }

    fun setVideoName(name: String): MediaDataController {
        this.videoName = name
        return this
    }

    fun setData(url: String): MediaDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.add(VideoData(url, url))
        this.onDataChangedListeners.forEach {
            it.onDataChanged(videoData)
        }
        return this
    }

    fun setData(name: String, url: String): MediaDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.add(VideoData(name, url))
        this.onDataChangedListeners.forEach {
            it.onDataChanged(videoData)
        }
        return this
    }

    fun setData(video: VideoData): MediaDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.add(video)
        this.onDataChangedListeners.forEach {
            it.onDataChanged(videoData)
        }
        return this
    }

    fun setData(list: List<VideoData>): MediaDataController {
        this.index = 0
        this.videoData.clear()
        this.videoData.addAll(list)
        this.onDataChangedListeners.forEach {
            it.onDataChanged(videoData)
        }
        return this
    }

    fun addOnPlayListener(listener: OnPlayListener): MediaDataController {
        this.onPlayListeners.add(listener)
        return this
    }

    fun removeOnPlayListener(listener: OnPlayListener): MediaDataController {
        this.onPlayListeners.remove(listener)
        return this
    }

    fun addOnCompleteListener(listener: OnCompleteListener): MediaDataController {
        this.onCompleteListeners.add(listener)
        return this
    }

    fun removeOnCompleteListener(listener: OnCompleteListener): MediaDataController {
        this.onCompleteListeners.remove(listener)
        return this
    }

    interface OnPlayListener {
        fun onPlay(position: Int, videoData: VideoData)
    }

    interface OnDataChangedListener {
        fun onDataChanged(data: List<VideoData>)
    }

    interface OnCompleteListener {
        fun onCompletion()
    }

    interface OnFullScreenListener {
        fun invoke(isFullScreen: Boolean)
    }


    interface OnCallTimedStopListener {
        /**
         * @param position 计时进度
         * @param duration 总计时时长
         * @param smart 是否智能停止
         */
        fun updateTime(position: Long, duration: Long, smart: Boolean)
        fun onTimeStop()
    }

    fun addOnCallTimedStopListener(listener: OnCallTimedStopListener): MediaDataController {
        this.onCallTimedStopListeners.add(listener)
        return this
    }

    fun removeOnCallTimedStopListener(listener: OnCallTimedStopListener): MediaDataController {
        this.onCallTimedStopListeners.remove(listener)
        return this
    }

    fun addOnFullScreenListener(listener: OnFullScreenListener): MediaDataController {
        this.onFullScreenListeners.add(listener)
        return this
    }

    fun removeOnFullScreenListener(listener: OnFullScreenListener): MediaDataController {
        this.onFullScreenListeners.remove(listener)
        return this
    }

    fun addOnDataChangedListener(listener: OnDataChangedListener): MediaDataController {
        this.onDataChangedListeners.add(listener)
        return this
    }

    fun removeOnDataChangedListener(listener: OnDataChangedListener): MediaDataController {
        this.onDataChangedListeners.remove(listener)
        return this
    }

    fun hasNext(): Boolean {
        return when (orderMode) {
            OrderMode.Loop -> true

            OrderMode.Random -> true

            OrderMode.ListLoop -> true

            OrderMode.Sequential -> index + 1 in videoData.indices
        }
    }

    fun hasPrevious(): Boolean {
        return when (orderMode) {
            OrderMode.Loop -> true

            OrderMode.Random -> true

            OrderMode.ListLoop -> true

            OrderMode.Sequential -> index - 1 in videoData.indices
        }
    }

    fun next() {
        if (hasNext()) {
            when (orderMode) {
                OrderMode.Loop -> start()

                OrderMode.Random -> {
                    index = (0..videoData.size).random()
                    start()
                }

                OrderMode.ListLoop -> {
                    if (index + 1 in videoData.indices) {
                        index += 1
                        start()
                    } else {
                        index = 0
                        start()
                    }
                }

                OrderMode.Sequential -> {
                    index += 1
                    start()
                }
            }
        }
    }

    fun previous() {
        if (hasPrevious()) {
            when (orderMode) {
                OrderMode.Loop -> start()

                OrderMode.Random -> {
                    index = (0..videoData.size).random()
                    start()
                }

                OrderMode.ListLoop -> {
                    if (index - 1 in videoData.indices) {
                        index -= 1
                        start()
                    } else {
                        index = videoData.lastIndex
                        start()
                    }
                }

                OrderMode.Sequential -> {
                    index -= 1
                    start()
                }
            }
        }
    }

    /**
     * 定制结束播放
     */
    private fun startTimedStop() {
        if (timedStopDuration <= 0) {
            smartStop = false
            sensorManager?.unregisterListener(this@MediaDataController)
            timedStopJob?.cancel()
        } else {
            val start = System.currentTimeMillis()
            timedStopJob?.cancel()
            timedStopJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    delay(1000)
                    val position = System.currentTimeMillis() - start
                    if (position > timedStopDuration) {
                        smartStop = false
                        sensorManager?.unregisterListener(this@MediaDataController)

                        timedStopDuration = -1
                        onCallTimedStopListeners.forEach {
                            it.onTimeStop()
                        }
                        break
                    } else {
                        onCallTimedStopListeners.forEach {
                            it.updateTime(position, timedStopDuration, smartStop)
                        }
                    }
                }
            }
        }
    }

    private var smartStop = false
    private var lastSensorValue = 0f
    private var lastSensorUpdateTime: Long = 0

    fun startSmartTimedStop(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        sensorManager?.let {
            val sensor = it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            it.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            smartStop = true
            setTimedStopDuration(15, MINUTES)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) { //加速度传感器
            val current = System.currentTimeMillis()
            val interval = current - lastSensorUpdateTime
            if (interval > 500) {
                val value = event.values.sum()//values >> x 左倾 | -x 右倾 | y 上倾 | -y 下倾
                val range = abs(value - lastSensorValue) / interval * 10000
                lastSensorValue = value
                lastSensorUpdateTime = current
                //经测试设定为1时比较准确
                //波动检测幅度设置过大的话灵敏度不足
                //检测到枕头波动幅度大于1，说明用户头动了，重新启动智能停止任务
                if (range > 1) {
                    smartStop = true
                    setTimedStopDuration(15, MINUTES)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    fun setTimedStopDuration(duration: Long, unit: TimeUnit = MINUTES) {
        timedStopDuration = unit.toMillis(duration)
        startTimedStop()
    }

    fun getTimedStopDuration(): Long {
        return timedStopDuration
    }

    fun setOrderMode(orderMode: OrderMode) {
        this.orderMode = orderMode
    }

    fun setIndex(index: Int) {
        this.index = index
    }

    fun start() {
        if (index !in videoData.indices) {
            Log.e("VideoDataController", "index out of video list!")
        } else {
            Log.i(
                "VideoDataController",
                "onPlayListeners >> ${onPlayListeners.size}"
            ) //toList防止ConcurrentModificationException
            onPlayListeners.toList().forEach {
                it.onPlay(index, videoData[index])
            }
        }
    }

    fun start(index: Int) {
        this.index = index
        this.start()
    }

    fun release() {
        index = 0
        videoData.clear()
        onPlayListeners.clear()
        onCompleteListeners.clear()
        onFullScreenListeners.clear()
        onCallTimedStopListeners.clear()
        timedStopJob?.cancel()
        videoView?.removeOnStateChangeListener(onStateChangeListener)
        videoView = null
        videoName = ""
    }
}