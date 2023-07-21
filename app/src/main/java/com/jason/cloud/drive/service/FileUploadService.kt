package com.jason.cloud.drive.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.drake.net.utils.fileName
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jason.cloud.drive.R
import com.jason.cloud.drive.utils.extension.getParcelableArrayListEx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FileUploadService : Service() {
    private val name = "文件上传服务"
    private val channelId = "file_upload_service"
    private val notificationId: Int = 2000
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        fun upload(context: Context, hash: String, uriList: List<Uri>) {
            Log.e("FileUploadService", "launch...")
            fun start() {
                context.startService(Intent(context, FileUploadService::class.java).apply {
                    putExtra("hash", hash)
                    putParcelableArrayListExtra("list", ArrayList(uriList))
                })
            }

            if (uriList.isNotEmpty()) {
                if (XXPermissions.isGranted(context, Permission.POST_NOTIFICATIONS)) {
                    start()
                } else {
                    XXPermissions.with(context).permission(Permission.POST_NOTIFICATIONS)
                        .request { _, _ ->
                            start()
                        }
                }
            }
        }
    }

    private fun showNotification() {
        notificationManager.createNotificationChannel(createChannel())

        notificationBuilder = NotificationCompat.Builder(this, channelId)
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

        startForeground(notificationId, notificationBuilder.build())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("FileUploadService", "dealUriList...")
        dealUriList(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun dealUriList(intent: Intent?) {
        val hash = intent?.getStringExtra("hash").orEmpty()
        val uriList = intent?.getParcelableArrayListEx("list", Uri::class.java).orEmpty()
        if (hash.isNotBlank() && uriList.isNotEmpty()) {
            uploadNow(hash, uriList)
        }
    }

    private fun uploadNow(hash: String, list: List<Uri>) {
        test(list)
        list.forEach {
            Log.e("FileUploadService", "$hash >> $it")

        }
    }

    private fun test(list: List<Uri>) {
        CoroutineScope(Dispatchers.Main).launch {
            list.forEach {
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.checkSelfPermission(
                        this@FileUploadService,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } else {
                    PackageManager.PERMISSION_GRANTED
                }
                if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(notificationId, setNotification(it).build())
                    delay(5000)
                }
            }
        }
    }

    private fun createChannel(): NotificationChannelCompat {
        return NotificationChannelCompat.Builder(
            channelId,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        ).setName(name).build()
    }

    private fun setNotification(uri: Uri): NotificationCompat.Builder {
        notificationBuilder.setContentTitle("正在上传文件")
        notificationBuilder.setContentText(uri.fileName() ?: uri.toString())
        notificationBuilder.setProgress(100, 0, true)
        notificationBuilder.setSubText("23.2 MB/s")
        return notificationBuilder
    }
}