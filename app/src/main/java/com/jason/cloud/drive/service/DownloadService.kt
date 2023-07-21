package com.jason.cloud.drive.service

import TaskQueue
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jason.cloud.drive.R
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.downloader.getStatusText
import com.jason.cloud.drive.database.downloader.toTaskEntity
import com.jason.cloud.drive.utils.extension.getSerializableListExtraEx
import com.jason.cloud.drive.utils.extension.putSerializableListExtra
import com.jason.cloud.drive.utils.extension.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable

class DownloadService : Service() {
    private val name = "文件上传服务"
    private val channelId = "file_upload_service"
    private val notificationId: Int = 20001
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val binder = DownloadBinder(this)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var taskObserver: Job? = null

    val downloadQueue = TaskQueue<DownloadTask>()

    class DownloadParam(val name: String, val url: String, val hash: String, val dir: File) :
        Serializable

    companion object {
        /**
         * 上传文件到指定文件夹
         * @param dir 要下载到的目标文件夹
         * @param list 本地文件URI列表
         */
        fun launchWith(context: Context, params: List<DownloadParam>) {
            val service = Intent(context, DownloadService::class.java).apply {
                putSerializableListExtra("params", params)
            }
            XXPermissions.with(context).permission(Permission.POST_NOTIFICATIONS)
                .request { _, allGranted ->
                    if (allGranted.not()) {
                        context.toast("请赋予软件通知权限")
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(service)
                        } else {
                            context.startService(service)
                        }
                    }
                }
        }
    }

    init {
        downloadQueue
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    class DownloadBinder(val service: DownloadService) : Binder()

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, channelId)
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val params = intent?.getSerializableListExtraEx<DownloadParam>("params") ?: emptyList()
        if (params.isNotEmpty()) {
            startDownloads(params)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification() {
        val notificationChannel = NotificationChannelCompat.Builder(
            channelId,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName(name).build()

        notificationManager.createNotificationChannel(notificationChannel)
        notificationBuilder.setChannelId(channelId)
        notificationBuilder.setSmallIcon(R.drawable.ic_cloud_six_24)
        notificationBuilder.setLargeIcon(
            BitmapFactory.decodeResource(
                resources,
                R.drawable.ic_cloud_six_24
            )
        )

        notificationBuilder.setContentTitle(name)
        notificationBuilder.setContentText("文件上传服务正在启动..")
        notificationBuilder.setOngoing(true) //不能被清除
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.foregroundServiceBehavior =
                Notification.FOREGROUND_SERVICE_IMMEDIATE
        }
        startForeground(notificationId, notificationBuilder.build())
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

    private fun startDownloads(params: List<DownloadParam>) {
        downloadQueue.onTaskStart {
            TaskDatabase.INSTANCE.getDownloadDao().put(it.toTaskEntity())
            startObserveTask(it)
        }
        downloadQueue.onTaskDone {
            TaskDatabase.INSTANCE.getDownloadDao().put(it.toTaskEntity())
        }
        downloadQueue.onTaskListDone {
            toast("全部取回任务完成！")
            taskObserver?.cancel()
            downloadQueue.release()
            stopSelf()
        }
        downloadQueue.addTask(ArrayList<DownloadTask>().apply {
            params.forEach { param ->
                add(DownloadTask(param.name, param.url, param.hash, param.dir))
            }
        })
        downloadQueue.start()
    }

    private fun startObserveTask(task: DownloadTask) {
        notificationBuilder.setContentTitle(task.name)
        notificationBuilder.setContentText(task.getStatusText())
        notificationBuilder.setProgress(100, task.progress, false)
        update()

        taskObserver?.cancel()
        taskObserver = scope.launch(Dispatchers.IO) {
            var status = DownloadTask.Status.QUEUE
            var progress = 0
            var downloadBytes = 0L

            while (isActive) {
                if (task.isRunning()) {
                    if (task.progress != progress) {
                        progress = task.progress
                        TaskDatabase.INSTANCE.getDownloadDao()
                            .updateProgress(task.hash, task.progress)
                    }
                    if (task.downloadBytes != downloadBytes) {
                        downloadBytes = task.downloadBytes
                        TaskDatabase.INSTANCE.getDownloadDao()
                            .updateDownloadedBytes(task.hash, task.downloadBytes)
                    }
                    if (task.status != status) {
                        status = task.status
                        TaskDatabase.INSTANCE.getDownloadDao().updateStatus(task.hash, task.status)
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationBuilder.setContentTitle(task.name)
                    notificationBuilder.setContentText(task.getStatusText())
                    notificationBuilder.setProgress(100, task.progress, false)

                    val taskList = downloadQueue.getTaskList()
                    val doneSize = taskList.count { it.isDone() }
                    notificationBuilder.setSubText("$doneSize/${taskList.size}")
                    update()
                }
                delay(1000)
            }
        }
    }
}