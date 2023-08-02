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
import com.jason.cloud.drive.utils.VideoDataController
import com.jason.cloud.extension.dp
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.squared
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3VideoItem
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.utils.Media3PlayerUtils
import com.jason.cloud.media3.widget.Media3AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable

class AudioService : Service(), OnStateChangeListener {
    private val name = "媒体播放器"
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val channelId = "audio_service"
    private val notificationId: Int = 20003
    private val channel by lazy {
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(name).build()
    }


    private lateinit var audioPlayer: Media3AudioPlayer
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
            list: List<Media3VideoItem>,
            position: Int = 0,
            block: Intent.() -> Unit
        ) {
            fun start() {
                val service = Intent(context, AudioService::class.java).apply {
                    putExtra("list", list as Serializable)
                    putExtra("position", position)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service)
                    block.invoke(service)
                } else {
                    context.startService(service)
                    block.invoke(service)
                }
            }

            XXPermissions.with(context).permission(Permission.NOTIFICATION_SERVICE)
                .request { _, allGranted ->
                    if (allGranted.not()) {
                        context.toast("请先赋予通知权限")
                    } else {
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

    class AudioBinder(val videoView: Media3AudioPlayer) : Binder() {

    }

    override fun onBind(intent: Intent?): IBinder {
        return AudioBinder(audioPlayer)
    }

    override fun onStateChanged(state: Int) {
        updateActions()
        when (state) {
            Media3PlayState.STATE_BUFFERING -> {
                progressJob?.cancel()
                notificationBuilder.setContentText("正在加载媒体...")
                updateIfInteractive()
            }

            Media3PlayState.STATE_ERROR -> {
                progressJob?.cancel()
                notificationBuilder.setContentText("媒体播放错误：STATE_ERROR")
                updateNow()
            }

            Media3PlayState.STATE_IDLE, Media3PlayState.STATE_ENDED -> {
                progressJob?.cancel()
            }

            Media3PlayState.STATE_PLAYING -> {
                progressJob?.cancel()
                progressJob = scope.launch {
                    while (isActive && audioPlayer.isPlaying()) {
                        notificationBuilder.setSubText(
                            Media3PlayerUtils.stringForTime(
                                audioPlayer.getDuration() - audioPlayer.getCurrentPosition()
                            )
                        )
                        updateIfInteractive()
                        delay(1000)
                    }
                }
            }

            else -> {
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioPlayer = Media3AudioPlayer(this)
        audioPlayer.addOnStateChangeListener(this)
        VideoDataController.with("AudioService").attachPlayer(audioPlayer)

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

        VideoDataController.with("AudioService")
            .addOnCompleteListener(object : VideoDataController.OnCompleteListener {
                override fun onCompletion() {
                    audioPlayer.release()
                    stopSelf()
                }
            })

        VideoDataController.with("AudioService")
            .addOnPlayListener(object : VideoDataController.OnPlayListener {
                override fun onPlay(position: Int, videoData: Media3VideoItem) {
                    loadMediaInfo(videoData)
                }
            })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            MEDIA_STYLE_ACTION_PLAY -> audioPlayer.start()
            MEDIA_STYLE_ACTION_PAUSE -> audioPlayer.pause()
            MEDIA_STYLE_ACTION_PREVIOUS -> previous()
            MEDIA_STYLE_ACTION_NEXT -> next()
            MEDIA_STYLE_ACTION_STOP -> { //如果已经bindService，stopSelf会在UnBind后自动结束
                audioPlayer.release()
                stopSelf()
            }

            else -> {
                VideoDataController.with("AudioService").setData(
                    intent.getSerializableListExtraEx("list")
                )
                VideoDataController.with("AudioService").start(
                    intent.getIntExtra(
                        "position", 0
                    )
                )
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("SetTextI18n")
    private fun loadMediaInfo(file: Media3VideoItem) = scope.launch(Dispatchers.IO) {
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
                notificationBuilder.setContentTitle(title.ifBlank { file.title })
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
            if (audioPlayer.isPlaying()) MEDIA_STYLE_ACTION_PAUSE else MEDIA_STYLE_ACTION_PLAY
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val text = if (audioPlayer.isPlaying()) "PAUSE" else "PLAY"
        val image =
            if (audioPlayer.isPlaying()) R.drawable.ic_round_pause_24 else R.drawable.ic_round_play_arrow_24
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
        if (VideoDataController.with("AudioService").hasPrevious()) {
            VideoDataController.with("AudioService").previous()
        } else {
            toast("已经是第一个啦")
        }
    }

    private fun next() {
        if (VideoDataController.with("AudioService").hasNext()) {
            VideoDataController.with("AudioService").next()
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
        audioPlayer.release()
        VideoDataController.with("AudioService").release()
    }

}