package com.jason.cloud.drive.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
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
import com.drake.net.Get
import com.drake.net.NetConfig
import com.drake.net.Post
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.scope.NetCoroutineScope
import com.drake.net.utils.fileName
import com.drake.net.utils.scopeNet
import com.jason.cloud.drive.R
import com.jason.cloud.drive.utils.extension.asJSONObject
import com.jason.cloud.drive.utils.extension.createMD5String
import com.jason.cloud.drive.utils.extension.getParcelableExtraEx
import com.jason.cloud.drive.utils.extension.runOnMainAtFrontOfQueue
import com.jason.cloud.drive.utils.extension.toFileSizeString
import com.jason.cloud.drive.utils.Configure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class UploadService : Service() {
    private val name = "文件上传服务"
    private val channelId = "file_upload_service"
    private val notificationId: Int = 2000
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat
    private var uploadJob: NetCoroutineScope? = null
    private val binder = UploadBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    class UploadBinder : Binder() {
        var onProgressListener: ((progress: Int, speed: Long) -> Unit)? = null
        var onFileCheckListener: (() -> Unit)? = null
        var onUploadDoneListener: (() -> Unit)? = null
        var onUploadSucceedListener: ((isFlash: Boolean) -> Unit)? = null
        var onErrorListener: ((String) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, channelId)
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hash = intent?.getStringExtra("hash")
        val uri = intent?.getParcelableExtraEx("uri", Uri::class.java)
        if (uri != null && hash != null) {
            startUploadURI(uri, hash)
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

        startForeground(notificationId, notificationBuilder.build())
    }

    private fun startUploadURI(uri: Uri, hash: String) {
        uploadJob = scopeNet {
            notificationBuilder.setStyle(null)
            notificationBuilder.setContentTitle(name)
            notificationBuilder.setContentText("正在校验文件...")
            notificationBuilder.setProgress(0, 0, true)
            update()
            delay(100)

            if (binder.isBinderAlive) {
                binder.onFileCheckListener?.invoke()
            }
            val fileHash = withContext(Dispatchers.IO) { //创建文件HASH
                NetConfig.app.contentResolver.openInputStream(uri)?.use {
                    it.createMD5String()
                }
            }

            if (fileHash == null) {
                if (binder.isBinderAlive) {
                    binder.onErrorListener?.invoke("校验文件HASH失败！")
                }
            } else {
                //先尝试判断服务器是否存在相同文件
                val flashed = Get<String>("${Configure.hostURL}/flash") {
                    addQuery("hash", hash)
                    addQuery("fileHash", fileHash)
                    addQuery("fileName", uri.fileName())
                }.await().let {
                    it.asJSONObject().optInt("code") == 200
                }

                if (flashed) {
                    if (binder.isBinderAlive) {
                        binder.onUploadSucceedListener?.invoke(true)
                    }
                } else {
                    notificationBuilder.setStyle(null)
                    notificationBuilder.setOngoing(true)
                    notificationBuilder.setContentTitle(name)
                    notificationBuilder.setContentText("正在上传文件，请稍候..")
                    notificationBuilder.setProgress(0, 0, true)
                    update()

                    Post<String>("${Configure.hostURL}/upload") {
                        addQuery("hash", hash)
                        addQuery("fileHash", fileHash)
                        param("file", uri)
                        setClient {
                            readTimeout(1, TimeUnit.HOURS)
                            writeTimeout(1, TimeUnit.HOURS)
                        }
                        addUploadListener(object : ProgressListener() {
                            var lastNotify = 0L
                            var lastProgress = 0

                            override fun onProgress(p: Progress) {
                                val progress = p.progress()
                                if (binder.isBinderAlive) {
                                    binder.onProgressListener?.invoke(progress, p.speedBytes)
                                }

                                runOnMainAtFrontOfQueue {
                                    if (System.currentTimeMillis() - lastNotify >= 1000 || progress != lastProgress) {
                                        lastNotify = System.currentTimeMillis()
                                        lastProgress = p.progress()

                                        notificationBuilder.setStyle(null)
                                        notificationBuilder.setSubText(p.speedBytes.toFileSizeString() + "/s")
                                        notificationBuilder.setContentText("正在上传文件：$progress%，请稍候..")
                                        notificationBuilder.setProgress(100, progress, false)
                                        update()
                                    }
                                }
                            }
                        })
                    }.await().asJSONObject().also {
                        if (it.optInt("code") == 200) {
                            if (binder.isBinderAlive) {
                                binder.onUploadSucceedListener?.invoke(false)
                            }
                        } else {
                            if (binder.isBinderAlive) {
                                binder.onErrorListener?.invoke(it.optString("message"))
                            }
                        }
                    }
                }
            }
        }.apply {
            catch {
                if (binder.isBinderAlive) {
                    binder.onErrorListener?.invoke(it.stackTraceToString())
                }
            }
            finally {
                if (binder.isBinderAlive) {
                    binder.onUploadDoneListener?.invoke()
                }
                stopSelf()
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        uploadJob?.cancel()
    }
}