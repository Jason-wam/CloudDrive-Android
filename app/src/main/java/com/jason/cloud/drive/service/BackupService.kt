package com.jason.cloud.drive.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jason.cloud.drive.R
import com.jason.cloud.drive.database.backup.BackupTask
import com.jason.cloud.drive.utils.MediaEntity
import com.jason.cloud.drive.utils.TaskQueue
import com.jason.cloud.drive.utils.scanAudios
import com.jason.cloud.drive.utils.scanImages
import com.jason.cloud.drive.utils.scanVideos
import com.jason.cloud.extension.createSketchedMD5String
import com.jason.cloud.extension.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupService : Service() {
    private val name = "文件备份服务"
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val channelId = "backup_service"
    private val notificationId: Int = 20003
    private val channel by lazy {
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_MIN)
            .setName(name).build()
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        fun launchWith(context: Context, block: (() -> Unit)? = null) {
            XXPermissions.with(context).permission(
                Permission.READ_MEDIA_IMAGES,
                Permission.READ_MEDIA_AUDIO,
                Permission.READ_MEDIA_VIDEO
            ).request { _, allGranted ->
                if (allGranted.not()) {
                    context.toast("请赋予软件读取媒体文件权限")
                } else {
                    block?.invoke()
                    val service = Intent(context, BackupService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(service)
                    } else {
                        context.startService(service)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, channelId)
        notificationManager.createNotificationChannel(channel)
        notificationBuilder.setChannelId(channelId)
        notificationBuilder.setSmallIcon(R.drawable.ic_cloud_six_24)
        notificationBuilder.setContentTitle(name)
        notificationBuilder.setContentText("文件备份服务启动..")
        notificationBuilder.setOngoing(true) //不能被清除
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.foregroundServiceBehavior =
                Notification.FOREGROUND_SERVICE_IMMEDIATE
        }
        startForeground(notificationId, notificationBuilder.build())
        startBackup()
    }

    private fun startBackup() {
        scope.launch(Dispatchers.IO) {
            val files = listFiles {
                withContext(Dispatchers.Main) {
                    notificationBuilder.setContentText("正在枚举备份文件: $it ..")
                    notificationBuilder.setProgress(100, 0, true)
                    update()
                }
            }
            withContext(Dispatchers.Main) {
                notificationBuilder.setContentText("正在备份文件 ...")
                notificationBuilder.setProgress(files.size, 0, false)
                update()
            }
            startBackupQueue(files)
        }
    }

    private fun startBackupQueue(list: List<Pair<MediaEntity, String>>) {
        var count = 0
        val queue = TaskQueue<BackupTask>().apply {
            threadSize(20)
            list.distinctBy {
                it.second
            }.also {
                it.forEachIndexed { index, pair ->
                    addTask(BackupTask(pair.first.uri, pair.second))
                    notificationBuilder.setContentText("正在准备任务 ${index + 1}/${it.size}...")
                    notificationBuilder.setProgress(it.size, index + 1, false)
                    update()
                }
            }
        }
        queue.onTaskDone {
            count += 1
            notificationBuilder.setContentText("正在备份文件: $count/${list.size} ..")
            notificationBuilder.setProgress(list.size, count, false)
            update()
        }
        queue.onTaskListDone {
            toast("文件备份完毕！")
            notificationBuilder.setContentText("文件备份完毕！")
            notificationBuilder.setProgress(100, 100, false)
            notificationBuilder.setOngoing(false)
            update()
        }
        queue.start()
    }

    private suspend fun <T> T.listFiles(block: suspend T.(Int) -> Unit): List<Pair<MediaEntity, String>> =
        withContext(Dispatchers.IO) {
            var count = 0
            ArrayList<Pair<MediaEntity, String>>().apply {
                val files = ArrayList<MediaEntity>().apply {
                    addAll(scanImages())
                    addAll(scanAudios())
                    addAll(scanVideos())
                }
                for (file in files) {
                    ensureActive()
                    val hash = file.uri.hash()
                    if (hash.isNotBlank()) {
                        add(Pair(file, hash))
                        count += 1
                        ensureActive()
                        block.invoke(this@listFiles, count)
                    }
                }
            }
        }

    private suspend fun Uri.hash(): String = withContext(Dispatchers.IO) {
        val file = DocumentFile.fromSingleUri(this@BackupService, this@hash)
        val length = file?.length() ?: 0
        if (length <= 0) return@withContext ""

        contentResolver.openInputStream(this@hash)?.use {
            it.createSketchedMD5String(length)
        }.orEmpty()
    }

    private fun update() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
}