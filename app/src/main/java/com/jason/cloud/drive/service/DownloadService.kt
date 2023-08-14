package com.jason.cloud.drive.service

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
import com.jason.cloud.drive.database.downloader.DownloadQueue
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.downloader.getStatusText
import com.jason.cloud.drive.database.downloader.toTaskEntity
import com.jason.cloud.drive.utils.TaskQueue
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.putSerializableListExtra
import com.jason.cloud.extension.toast
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
    private val name = "文件取回服务"
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val notificationId: Int = 20001
    private val channelId = "file_download_service"
    private val channel by lazy {
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(name).build()
    }

    private val binder = DownloadBinder(this)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var taskObserver: Job? = null

    class DownloadParam(val name: String, val url: String, val hash: String, val dir: File) :
        Serializable

    companion object {
        fun launchWith(context: Context, params: List<DownloadParam>, block: (() -> Unit)? = null) {
            if (params.isEmpty()) return
            fun start() {
                val service = Intent(context, DownloadService::class.java).apply {
                    putSerializableListExtra("params", params)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(service)
                    block?.invoke()
                } else {
                    context.startService(service)
                    block?.invoke()
                }
            }

            fun continueRun() {
                XXPermissions.with(context).permission(Permission.POST_NOTIFICATIONS)
                    .request { _, allGranted ->
                        if (allGranted) {
                            start()
                        } else {
                            context.toast("请赋予软件通知权限！")
                        }
                    }
            }

            val isGranted = XXPermissions.isGranted(context, Permission.POST_NOTIFICATIONS)
            if (isGranted) {
                start()
            } else {
                TextDialog(context)
                    .setTitle("权限提醒")
                    .setText("后台取回文件需要获取通知权限，请赋予相关权限后继续执行取回！")
                    .onNegative("取消")
                    .onPositive("继续执行", ::continueRun)
                    .show()
            }
        }
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

        DownloadQueue.instance.onTaskStart(object : TaskQueue.OnTaskStartListener<DownloadTask> {
            override fun onTaskStart(task: DownloadTask) {
                scope.launch(Dispatchers.IO) {
                    TaskDatabase.instance.getDownloadDao().put(task.toTaskEntity())
                    startObserveTask(task)
                }
            }
        })
        DownloadQueue.instance.onTaskDone(object : TaskQueue.OnTaskDoneListener<DownloadTask> {
            override fun onTaskDone(task: DownloadTask) {
                scope.launch(Dispatchers.IO) {
                    TaskDatabase.instance.getDownloadDao().put(task.toTaskEntity())
                }
            }
        })
        DownloadQueue.instance.onTaskListDone(object : TaskQueue.OnTaskListDoneListener {
            override fun onTaskListDone() {
                taskObserver?.cancel()
                toast("全部取回任务完成！")
                stopSelf()
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val params = intent?.getSerializableListExtraEx<DownloadParam>("params") ?: emptyList()
        if (params.isNotEmpty()) {
            DownloadQueue.instance.addTaskAndStart(ArrayList<DownloadTask>().apply {
                params.forEach { param ->
                    add(DownloadTask(param.name, param.url, param.hash, param.dir))
                }
            })
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification() {
        notificationManager.createNotificationChannel(channel)
        notificationBuilder.setChannelId(channelId)
        notificationBuilder.setSmallIcon(R.drawable.ic_cloud_six_24)
        notificationBuilder.setLargeIcon(
            BitmapFactory.decodeResource(resources, R.drawable.ic_cloud_six_24)
        )

        notificationBuilder.setContentTitle(name)
        notificationBuilder.setContentText("文件取回服务正在启动..")
        notificationBuilder.setOngoing(true) //不能被清除
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.foregroundServiceBehavior =
                Notification.FOREGROUND_SERVICE_IMMEDIATE
        }
        startForeground(notificationId, notificationBuilder.build())
    }

    private fun update() {
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

    private fun startObserveTask(task: DownloadTask) {
        notificationBuilder.setContentTitle(task.name)
        notificationBuilder.setContentText(task.getStatusText())
        notificationBuilder.setProgress(100, task.progress, false)
        update()

        taskObserver?.cancel()
        taskObserver = scope.launch {
            var status = DownloadTask.Status.QUEUE
            var progress = 0
            var downloadBytes = 0L

            while (isActive) {
                delay(1000)

                if (isActive) {
                    withContext(Dispatchers.IO) {
                        if (task.isRunning()) {
                            if (task.progress != progress) {
                                progress = task.progress
                                TaskDatabase.instance.getDownloadDao()
                                    .updateProgress(task.hash, task.progress)
                            }
                            if (task.downloadBytes != downloadBytes) {
                                downloadBytes = task.downloadBytes
                                TaskDatabase.instance.getDownloadDao()
                                    .updateDownloadedBytes(task.hash, task.downloadBytes)
                            }
                            if (task.status != status) {
                                status = task.status
                                TaskDatabase.instance.getDownloadDao()
                                    .updateStatus(task.hash, task.status)
                            }
                        }
                    }

                    notificationBuilder.setContentTitle(task.name)
                    notificationBuilder.setContentText(task.getStatusText())
                    notificationBuilder.setProgress(100, task.progress, false)

                    val taskList = DownloadQueue.instance.taskList
                    val doneSize = taskList.count { it.isDone() }
                    notificationBuilder.setSubText("$doneSize/${taskList.size}")
                    update()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DownloadQueue.instance.release()
    }
}