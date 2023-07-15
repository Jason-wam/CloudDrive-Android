package com.jason.cloud.drive.views.dialog

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import com.drake.net.utils.TipUtils.toast
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jason.cloud.drive.extension.runOnMainAtFrontOfQueue
import com.jason.cloud.drive.service.UploadService

class UploadDialog(context: Context) : ProgressDialog(context) {
    private var uri: Uri? = null
    private var hash: String = ""
    private var uploadServiceConnection: ServiceConnection? = null

    init {
        setCanceledOnTouchOutside(false)
    }

    fun setData(uri: Uri, hash: String): UploadDialog {
        this.uri = uri
        this.hash = hash
        return this
    }

    private fun checkPermission(block: () -> Unit) {
        XXPermissions.with(context).permission(Permission.NOTIFICATION_SERVICE)
            .request { _, allGranted ->
                if (allGranted.not()) {
                    toast("请先赋予通知权限")
                } else {
                    block.invoke()
                }
            }
    }

    fun startNow() {
        if (uri == null || hash.isBlank()) {
            toast("数据错误Uri或Hash为空！")
            return
        }
        checkPermission {
            val service = Intent(context, UploadService::class.java).apply {
                putExtra("uri", uri)
                putExtra("hash", hash)
            }
            setOnDismissListener {
                context.unbindService(uploadServiceConnection!!)
                uploadServiceConnection = null
                context.stopService(service)
            }

            uploadServiceConnection = createUploadServiceConnection()
            context.startService(service)
            context.bindService(
                service,
                uploadServiceConnection!!,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    private fun createUploadServiceConnection(): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder is UploadService.UploadBinder) {
                    binder.onFileCheckListener = {
                        runOnMainAtFrontOfQueue {
                            setIsIndeterminate(true)
                            setMessage("正在校验文件...")
                            if (isShowing.not()) {
                                show()
                            }
                        }
                    }
                    binder.onProgressListener = { progress, speed ->
                        runOnMainAtFrontOfQueue {
                            update(progress)
                            updateSpeed(speed)
                            setMessage("正在上传文件：$progress%，请稍候..")
                            setIsIndeterminate(false)
                            if (isShowing.not()) {
                                show()
                            }
                        }
                    }
                    binder.onUploadSucceedListener = {
                        runOnMainAtFrontOfQueue {
                            dismiss()
                            if (it) {
                                toast("文件闪传成功！")
                            } else {
                                toast("文件上传成功！")
                            }
                        }
                    }
                    binder.onErrorListener = {
                        runOnMainAtFrontOfQueue {
                            dismiss()
                            TextDialog(context).setTitle("错误").setText(it)
                                .onPositive("确定").dismiss()
                        }
                    }
                    binder.onUploadDoneListener = {
                        runOnMainAtFrontOfQueue {
                            dismiss()
                        }
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {

            }
        }
    }
}