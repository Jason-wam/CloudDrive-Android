package com.jason.cloud.drive.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.jason.cloud.drive.database.uploader.UploadQueue
import com.jason.cloud.drive.database.uploader.UploadTask
import com.jason.cloud.drive.database.uploader.getStatusText
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.extension.getParcelableArrayListEx
import com.jason.cloud.extension.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UploadService : Service() {
    private val name = "文件上传服务"
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val channelId = "file_upload_service"
    private val notificationId: Int = 20000
    private val channel by lazy {
        NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(name).build()
    }

    private val binder = UploadBinder(this)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var taskObserver: Job? = null

    companion object {
        /**
         * 上传文件到指定文件夹
         * @param hash 要上传到的目标文件夹
         * @param list 本地文件URI列表
         */
        fun launchWith(
            context: Context,
            hash: String,
            list: List<Uri>,
            block: (() -> Unit)? = null
        ) {
            fun start() {
                val service = Intent(context, UploadService::class.java).apply {
                    putExtra("hash", hash)
                    putParcelableArrayListExtra("uriList", ArrayList(list))
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
                    .setText("后台上传文件需要获取通知权限，请赋予相关权限后继续执行取回！")
                    .onNegative("取消")
                    .onPositive("继续执行", ::continueRun)
                    .show()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    class UploadBinder(val service: UploadService) : Binder()

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, channelId)
        showNotification()
        UploadQueue.instance.threadSize(3)
        UploadQueue.instance.onTaskStart {
            startObserveTask(it)
        }
        UploadQueue.instance.onTaskListDone {
            taskObserver?.cancel()
            toast("全部上传任务完成！")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hash = intent?.getStringExtra("hash")
        val uriList = intent?.getParcelableArrayListEx("uriList", Uri::class.java) ?: emptyList()
        if (!hash.isNullOrBlank() && uriList.isNotEmpty()) {
            UploadQueue.instance.addTaskAndStart(ArrayList<UploadTask>().apply {
                uriList.forEach { uri ->
                    add(UploadTask(uri, hash))
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
        notificationBuilder.setContentText("文件上传服务正在启动..")
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

    private fun startObserveTask(task: UploadTask) {
        notificationBuilder.setContentTitle(task.fileName)
        notificationBuilder.setContentText(task.getStatusText())
        notificationBuilder.setProgress(100, task.progress, false)
        update()

        taskObserver?.cancel()
        taskObserver = scope.launch {
            while (isActive) {
                delay(1000)
                if (isActive) {
                    notificationBuilder.setContentTitle(task.fileName)
                    notificationBuilder.setContentText(task.getStatusText())
                    notificationBuilder.setProgress(100, task.progress, false)
                    notificationBuilder.setOngoing(task.isDone().not())

                    val taskList = UploadQueue.instance.taskList
                    val doneSize = taskList.count { it.isDone() }
                    notificationBuilder.setSubText("${doneSize}/${taskList.size}")
                    update()
                }
            }
        }
    }
}