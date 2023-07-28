package com.jason.cloud.drive.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jason.cloud.drive.R
import com.jason.cloud.drive.model.AudioEntity
import com.jason.cloud.extension.dp
import com.jason.cloud.extension.squared
import com.jason.cloud.extension.toast
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.model.VideoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.doikki.videoplayer.player.BaseVideoView
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class AudioService : Service() {
    private val name = "媒体播放器"
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val channelId = "audio_service"
    private val notificationId: Int = 20003
    private val channel by lazy {
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(name).build()
    }


    private lateinit var videoView: VideoView
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    companion object {
        private const val MEDIA_STYLE_ACTION_PLAY = "MEDIA_STYLE_ACTION_PLAY"
        private const val MEDIA_STYLE_ACTION_PAUSE = "MEDIA_STYLE_ACTION_PAUSE"
        private const val MEDIA_STYLE_ACTION_PREVIOUS = "MEDIA_STYLE_ACTION_PREVIOUS"
        private const val MEDIA_STYLE_ACTION_NEXT = "MEDIA_STYLE_ACTION_NEXT"
        private const val MEDIA_STYLE_ACTION_STOP = "MEDIA_STYLE_ACTION_STOP"

        fun launchWith(
            context: Context,
            list: List<AudioEntity>,
            position: Int = 0,
            block: Intent.(MediaDataController) -> Unit
        ) {
            fun start() {
                val service = Intent(context, AudioService::class.java).apply {
                    putExtra("position", position)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service)
                    block.invoke(service, MediaDataController.with("AudioService"))
                } else {
                    context.startService(service)
                    block.invoke(service, MediaDataController.with("AudioService"))
                }
            }

            XXPermissions.with(context).permission(Permission.NOTIFICATION_SERVICE)
                .request { _, allGranted ->
                    if (allGranted.not()) {
                        context.toast("请先赋予通知权限")
                    } else {
                        MediaDataController.with("AudioService").setData(list)
                        start()
                    }
                }
        }

        fun stopService(context: Context?) {
            context?.let {
                Intent(context, AudioService::class.java).also {
                    context.stopService(it)
                }
            }
        }
    }

    class AudioBinder(val videoView: VideoView) : Binder() {

    }

    override fun onBind(intent: Intent?): IBinder {
        return AudioBinder(videoView)
    }

    override fun onCreate() {
        super.onCreate()
        videoView = VideoView(this)
        videoView.addOnStateChangeListener(onStateChangeListener)
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, channelId)

        notificationManager.createNotificationChannel(channel)
        notificationBuilder.setChannelId(channelId)
        notificationBuilder.setContentTitle(name)
        notificationBuilder.setContentText("WAITING ....")
        notificationBuilder.setSmallIcon(R.drawable.ic_music_two_24)
        notificationBuilder.setLargeIcon(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_music_two_200
            )?.toBitmap()
        )
        notificationBuilder.addAction(btnPrevious())
        notificationBuilder.addAction(btnPause())
        notificationBuilder.addAction(btnNext())
        notificationBuilder.addAction(btnStop())
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notificationBuilder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2)
        )
        notificationBuilder.setOngoing(true)
        startForeground(notificationId, notificationBuilder.build())

        MediaDataController.with("AudioService")
            .addOnCompleteListener(object : MediaDataController.OnCompleteListener {
                override fun onCompletion() {
                    videoView.release()
                    stopSelf()
                }
            })

        MediaDataController.with("AudioService")
            .addOnPlayListener(object : MediaDataController.OnPlayListener {
                override fun onPlay(position: Int, videoData: VideoData) {
                    if (videoData is AudioEntity) {
                        videoView.release()
                        videoView.setUrl(videoData.url)
                        videoView.start()
                        loadMediaInfo(videoData)
                    }
                }
            })

        MediaDataController.with("AudioService").addOnCallTimedStopListener(object :
            MediaDataController.OnCallTimedStopListener {
            override fun onTimeStop() {
                videoView.pause()
                updateNow()
                Log.i("AudioService", "timed stop.")
            }

            override fun updateTime(position: Long, duration: Long, smart: Boolean) {
                Log.i(
                    "AudioService",
                    "smart = $smart stop after ${
                        PlayerUtils.stringForTime(
                            (duration - position).toInt()
                        )
                    }"
                )
            }
        })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            MEDIA_STYLE_ACTION_PLAY -> videoView.resume()
            MEDIA_STYLE_ACTION_PAUSE -> videoView.pause()
            MEDIA_STYLE_ACTION_PREVIOUS -> previous()
            MEDIA_STYLE_ACTION_NEXT -> next()
            MEDIA_STYLE_ACTION_STOP -> { //如果已经bindService，stopSelf会在UnBind后自动结束
                videoView.release()
                stopSelf()
            }

            else -> {
                val position = intent.getIntExtra("position", 0)
                MediaDataController.with("AudioService").setVideoView(videoView)
                MediaDataController.with("AudioService").start(position)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private val onStateChangeListener = object : BaseVideoView.SimpleOnStateChangeListener() {
        override fun onPlayStateChanged(playState: Int) {
            updateActions()
            when (playState) {
                VideoView.STATE_PREPARING -> {
                    progressJob?.cancel()
                    notificationBuilder.setContentText("正在加载媒体...")
                    updateIfInteractive()
                }

                VideoView.STATE_ERROR -> {
                    progressJob?.cancel()
                    notificationBuilder.setContentText("媒体播放错误：STATE_ERROR")
                    updateNow()
                }

                VideoView.STATE_START_ABORT -> {
                    progressJob?.cancel()
                    notificationBuilder.setContentText("媒体播放错误：STATE_START_ABORT")
                    updateNow()
                }

                VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED -> {
                    progressJob?.cancel()
                }

                VideoView.STATE_BUFFERING, VideoView.STATE_PLAYING -> {
                    progressJob?.cancel()
                    progressJob = scope.launch {
                        while (isActive && videoView.isPlaying) {
                            notificationBuilder.setSubText(
                                PlayerUtils.stringForTime(
                                    (videoView.duration - videoView.currentPosition).toInt()
                                )
                            )
                            updateIfInteractive()
                            delay(1000)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadMediaInfo(file: AudioEntity) = scope.launch(Dispatchers.IO) {
        try {
            val bitmap = withContext(Dispatchers.IO) {
                Glide.with(this@AudioService).asBitmap().load(file.image).override(300.dp)
                    .submit().get()
            }
            withContext(Dispatchers.Main) {
                notificationBuilder.setLargeIcon(bitmap?.squared)
                updateIfInteractive()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                notificationBuilder.setLargeIcon(
                    ContextCompat.getDrawable(
                        this@AudioService,
                        R.drawable.ic_music_two_200
                    )?.toBitmap()
                )
                updateIfInteractive()
            }
        }

        var title = ""
        var album = ""
        var artist = ""
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.url, HashMap<String, String>())
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
            withContext(Dispatchers.Main) {
                notificationBuilder.setContentTitle(title.ifBlank { file.name })
                if (artist.isBlank()) {
                    notificationBuilder.setContentText("未知艺术家")
                } else {
                    if (album.isBlank()) {
                        notificationBuilder.setContentText(artist)
                    } else {
                        notificationBuilder.setContentText("$artist - $album")
                    }
                }
                updateIfInteractive()
            }
        }
    }

    private fun btnPrevious(): NotificationCompat.Action {
        val intent = Intent(this, AudioService::class.java)
        intent.action = MEDIA_STYLE_ACTION_PREVIOUS
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val text = "Previous"
        val image = IconCompat.createWithResource(this, R.drawable.ic_round_skip_previous_24)
        return NotificationCompat.Action.Builder(image, text, pendingIntent).build()
    }

    private fun btnPause(): NotificationCompat.Action {
        val intent = Intent(this, AudioService::class.java)
        intent.action =
            if (videoView.isPlaying) MEDIA_STYLE_ACTION_PAUSE else MEDIA_STYLE_ACTION_PLAY
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val text = if (videoView.isPlaying) "PAUSE" else "PLAY"
        val image =
            if (videoView.isPlaying) R.drawable.ic_round_pause_24 else R.drawable.ic_round_play_arrow_24
        val action = IconCompat.createWithResource(this, image)
        return NotificationCompat.Action.Builder(action, text, pendingIntent).build()
    }

    private fun btnNext(): NotificationCompat.Action {
        val intent = Intent(this, AudioService::class.java)
        intent.action = MEDIA_STYLE_ACTION_NEXT
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val text = "Next"
        val image = R.drawable.ic_round_skip_next_24
        val action = IconCompat.createWithResource(this, image)
        return NotificationCompat.Action.Builder(action, text, pendingIntent).build()
    }

    private fun btnStop(): NotificationCompat.Action {
        val intent = Intent(this, AudioService::class.java)
        intent.action = MEDIA_STYLE_ACTION_STOP
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val text = "Stop"
        val image = R.drawable.ic_round_stop_24
        val action = IconCompat.createWithResource(this, image)
        return NotificationCompat.Action.Builder(action, text, pendingIntent).build()
    }

    private fun previous() {
        if (MediaDataController.with("AudioService").hasPrevious()) {
            MediaDataController.with("AudioService").previous()
        } else {
            toast("已经是第一个啦")
        }
    }

    private fun next() {
        if (MediaDataController.with("AudioService").hasNext()) {
            MediaDataController.with("AudioService").next()
        } else {
            toast("已经是最后一个啦")
        }
    }

    private fun updateActions() {
        notificationBuilder.clearActions()
        notificationBuilder.addAction(btnPrevious())
        notificationBuilder.addAction(btnPause())
        notificationBuilder.addAction(btnNext())
        notificationBuilder.addAction(btnStop())
        notificationBuilder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2)
        )
        updateIfInteractive()
    }

    private fun updateIfInteractive() {
        if (isInteractive()) {
            updateNow()
        }
    }

    private fun updateNow() {
        val hasPermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true else {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        if (hasPermission) {
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    private fun isInteractive(): Boolean {
        return getSystemService<PowerManager>()?.isInteractive ?: true
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.release()
        MediaDataController.with("AudioService").release()
    }
}