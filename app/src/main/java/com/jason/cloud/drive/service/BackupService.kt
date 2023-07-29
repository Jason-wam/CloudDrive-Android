package com.jason.cloud.drive.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import com.jason.cloud.drive.utils.TaskQueue
import com.jason.cloud.drive.utils.extension.MediaEntity
import com.jason.cloud.drive.utils.extension.scanAudios
import com.jason.cloud.drive.utils.extension.scanImages
import com.jason.cloud.drive.utils.extension.scanVideos
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.extension.createSketchedMD5String
import com.jason.cloud.extension.toast
import com.jason.cloud.utils.MMKVStore
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
    private val notificationId: Int = 20002
    private val channel by lazy {
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(name).build()
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        fun launchWith(context: Context, block: (() -> Unit)? = null) {
            fun start() {
                val service = Intent(context, BackupService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service)
                    block?.invoke()
                } else {
                    context.startService(service)
                    block?.invoke()
                }
            }

            fun continueRun() {
                XXPermissions.with(context).permission(
                    Permission.POST_NOTIFICATIONS,
                    Permission.READ_MEDIA_IMAGES,
                    Permission.READ_MEDIA_AUDIO,
                    Permission.READ_MEDIA_VIDEO
                ).request { _, allGranted ->
                    if (allGranted) {
                        start()
                    } else {
                        context.toast("请赋予软件读取媒体文件权限")
                    }
                }
            }

            val isGranted = XXPermissions.isGranted(
                context,
                Permission.READ_MEDIA_IMAGES,
                Permission.READ_MEDIA_AUDIO,
                Permission.READ_MEDIA_VIDEO
            )
            if (isGranted) {
                start()
            } else {
                TextDialog(context)
                    .setTitle("权限提醒")
                    .setText("后台备份本地文件到云端需要读取媒体文件和通知权限，请赋予相关权限后继续执行备份！")
                    .onNegative("取消")
                    .onPositive("继续执行", ::continueRun)
                    .show()
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
        notificationBuilder.setLargeIcon(
            BitmapFactory.decodeResource(resources, R.drawable.ic_cloud_six_24)
        )

        notificationBuilder.setContentTitle(name)
        notificationBuilder.setContentText("文件备份服务启动..")
        notificationBuilder.setOngoing(true) //不能被清除
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.foregroundServiceBehavior =
                Notification.FOREGROUND_SERVICE_IMMEDIATE
        }
        startForeground(notificationId, notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startBackup()
        return super.onStartCommand(intent, flags, startId)
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
            threadSize(3)
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
            stopSelf()
        }
        queue.start()
    }

    private suspend fun <T> T.listFiles(block: suspend T.(Int) -> Unit) =
        withContext(Dispatchers.IO) {
            ArrayList<Pair<MediaEntity, String>>().apply {
                var count = 0
                val files = ArrayList<MediaEntity>().apply {
                    addAll(scanImages().sortedByDescending { it.date })
                    addAll(scanAudios().sortedByDescending { it.date })
                    addAll(scanVideos().sortedByDescending { it.date })
                }
                for (file in files.sortedByDescending { it.date }) {
                    ensureActive()
                    val key = file.uri.toString()
                    val hash = MMKVStore.with("BackupHash").getString(key).ifBlank {
                        file.uri.hash().also {
                            if (it.isNotBlank()) {
                                MMKVStore.with("BackupHash").put(key, it)
                            }
                        }
                    }
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

    private var lastUpdate = 0L
    private fun update(important: Boolean = false) {
        val hasPermission =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true else {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        if (hasPermission) {
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
}