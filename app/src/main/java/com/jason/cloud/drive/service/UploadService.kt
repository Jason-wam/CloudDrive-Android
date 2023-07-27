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
    private val channelId = "file_upload_service"
    private val notificationId: Int = 20000
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

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
            val service = Intent(context, UploadService::class.java).apply {
                putExtra("hash", hash)
                putParcelableArrayListExtra("uriList", ArrayList(list))
            }
            XXPermissions.with(context).permission(Permission.POST_NOTIFICATIONS)
                .request { _, allGranted ->
                    if (allGranted.not()) {
                        context.toast("请赋予软件通知权限")
                    } else {
                        block?.invoke()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(service)
                        } else {
                            context.startService(service)
                        }
                    }
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hash = intent?.getStringExtra("hash")
        val uriList = intent?.getParcelableArrayListEx("uriList", Uri::class.java) ?: emptyList()
        if (!hash.isNullOrBlank() && uriList.isNotEmpty()) {
            startUploads(uriList, hash)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun showNotification() {
        val notificationChannel = NotificationChannelCompat.Builder(
            channelId,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
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

    private fun startUploads(uriList: List<Uri>, hash: String) {
        UploadQueue.instance.onTaskStart {
            startObserveTask(it)
        }
        UploadQueue.instance.onTaskListDone {
            taskObserver?.cancel()
            toast("全部上传任务完成！")
            stopSelf()
        }
        UploadQueue.instance.addTask(ArrayList<UploadTask>().apply {
            uriList.forEach { uri ->
                add(UploadTask(uri, hash))
            }
        })
        UploadQueue.instance.start()
    }

    private fun startObserveTask(task: UploadTask) {
        notificationBuilder.setContentTitle(task.childName)
        notificationBuilder.setContentText(task.getStatusText())
        notificationBuilder.setProgress(100, task.progress, false)
        update()

        taskObserver?.cancel()
        taskObserver = scope.launch {
            while (isActive) {
                delay(1000)
                if (isActive) {
                    notificationBuilder.setContentTitle(task.childName)
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