package com.jason.cloud.drive.database.uploader

import TaskQueue
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.drake.net.Net
import com.drake.net.NetConfig
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.utils.scopeNet
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.extension.asJSONObject
import com.jason.cloud.drive.utils.extension.createSketchedMD5String
import com.jason.cloud.drive.utils.extension.toMd5String
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class UploadTask(val uri: Uri, val hash: String) : TaskQueue.Task() {
    var id: String = ""
    var name: String = ""
    var totalBytes: Long = 0
    var uploadedBytes: Long = 0
    var speedBytes: Long = 0
    var progress: Int = 0
    var status = Status.QUEUE
    var fileHash: String = ""

    enum class Status {
        QUEUE, CHECKING, UPLOADING, FAILED, SUCCEED, FLASH_UPLOADED, CONNECTING
    }

    init {
        this.id = uri.toString().toMd5String()
        val file = DocumentFile.fromSingleUri(NetConfig.app, uri)
        this.name = file?.name ?: ""
        this.totalBytes = file?.length() ?: 0
    }

    override fun isDone(): Boolean {
        return status == Status.SUCCEED || status == Status.FLASH_UPLOADED || status == Status.FAILED
    }

    override fun isRunning(): Boolean {
        return status == Status.UPLOADING || status == Status.CHECKING || status == Status.CONNECTING
    }

    override fun getTaskId(): Any {
        return uri.toString().toMd5String()
    }

    override fun start() {
        status = Status.CONNECTING
        scopeNet {
            status = Status.CHECKING
            Log.e("Uploader", "$name >> 校验文件...")
            //开始校验文件...
            fileHash = createFileHash(uri)
            if (fileHash.isBlank()) { //校验文件失败.
                status = Status.FAILED
                Log.e("Uploader", "$name >> 校验文件失败...")
            } else { //校验文件完成，尝试闪传文件
                if (tryFlashUpload(fileHash)) {
                    status = Status.FLASH_UPLOADED
                    Log.e("Uploader", "$name >> 闪传成功！")
                } else {
                    //闪传失败，开始上传文件
                    status = Status.UPLOADING
                    Log.e("Uploader", "$name >> 正在上传文件...")
                    status = if (upload(uri, fileHash)) {
                        Log.e("Uploader", "$name >> 上传成功！")
                        Status.SUCCEED
                    } else {
                        Log.e("Uploader", "$name >> 上传失败！")
                        Status.FAILED
                    }
                }
            }
        }.catch {
            it.printStackTrace()
            Log.e("Uploader", "$name >> 上传失败！")
            status = Status.FAILED
        }
    }

    override fun pause() {
    }

    override fun stop() {
        Net.cancelId(id)
    }

    private suspend fun createFileHash(uri: Uri): String = withContext(Dispatchers.IO) {
        if (totalBytes <= 0) return@withContext ""
        NetConfig.app.contentResolver.openInputStream(uri)?.use {
            it.createSketchedMD5String(totalBytes)
        }.orEmpty()
    }

    private suspend fun tryFlashUpload(fileHash: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Net.get("${Configure.hostURL}/flash") {
                setId(id)
                addQuery("hash", hash)
                addQuery("fileName", name)
                addQuery("fileHash", fileHash)
            }.execute<String>().asJSONObject().optInt("code") == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun upload(uri: Uri, fileHash: String): Boolean = withContext(Dispatchers.IO) {
        Net.post("${Configure.hostURL}/upload") {
            setId(id)
            param("file", uri)
            addQuery("hash", hash)
            addQuery("fileHash", fileHash)
            setClient {
                readTimeout(1, TimeUnit.HOURS)
                writeTimeout(1, TimeUnit.HOURS)
            }
            addUploadListener(object : ProgressListener() {
                override fun onProgress(p: Progress) {
                    progress = p.progress()
                    uploadedBytes = p.currentByteCount
                    totalBytes = p.totalByteCount
                    speedBytes = p.speedBytes
                }
            })
        }.execute<String>().asJSONObject().let {
            it.optInt("code") == 200
        }
    }
}