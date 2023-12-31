package com.jason.cloud.drive.database.uploader

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.drake.net.Net
import com.drake.net.NetConfig
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.utils.scopeNet
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.ItemSelector
import com.jason.cloud.drive.utils.TaskQueue
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.createSketchedMD5String
import com.jason.cloud.extension.toMd5String
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class UploadTask(val uri: Uri, val folderHash: String) : ItemSelector.SelectableItem,
    TaskQueue.Task() {
    var id: String = uri.toString().toMd5String()
    var totalBytes: Long = 0
    var uploadedBytes: Long = 0
    var speedBytes: Long = 0
    var progress: Int = 0
    var status = Status.QUEUE
    var fileName: String = ""
    var fileHash: String = ""

    override fun primaryKey(): Any {
        return id
    }

    enum class Status {
        QUEUE, CHECKING, UPLOADING, FAILED, SUCCEED, FLASH_UPLOADED, CONNECTING
    }

    init {
        val file = DocumentFile.fromSingleUri(NetConfig.app, uri)
        this.fileName = file?.name ?: ""
        this.totalBytes = file?.length() ?: 0
    }

    override fun isDone(): Boolean {
        return status == Status.SUCCEED || status == Status.FLASH_UPLOADED || status == Status.FAILED
    }

    override fun isRunning(): Boolean {
        return status == Status.UPLOADING || status == Status.CHECKING || status == Status.CONNECTING
    }

    fun isSucceed(): Boolean {
        return status == Status.SUCCEED || status == Status.FLASH_UPLOADED
    }

    override fun getTaskId(): Any {
        return id
    }

    override fun start(): UploadTask {
        status = Status.CONNECTING
        scopeNet {
            status = Status.CHECKING
            Log.e("Uploader", "$fileName >> 校验文件...")
            //开始校验文件...
            fileHash = createFileHash(uri)
            if (fileHash.isBlank()) { //校验文件失败.
                status = Status.FAILED
                Log.e("Uploader", "$fileName >> 校验文件失败...")
            } else {
                //校验文件完成，开始上传文件
                status = Status.UPLOADING
                //尝试闪传文件
                if (flashTransfer(fileHash)) {
                    status = Status.FLASH_UPLOADED
                    Log.e("Uploader", "$fileName >> 闪传成功！")
                } else {
                    //闪传失败，开始上传文件
                    Log.e("Uploader", "$fileName >> 正在上传文件...")
                    status = if (upload(uri, fileHash)) {
                        Log.e("Uploader", "$fileName >> 上传成功！")
                        Status.SUCCEED
                    } else {
                        Log.e("Uploader", "$fileName >> 上传失败！")
                        Status.FAILED
                    }
                }
            }
        }.catch {
            it.printStackTrace()
            status = Status.FAILED
            Log.e("Uploader", "$fileName >> 上传失败！")
        }
        return this
    }

    override fun pause() {
    }

    override fun cancel() {
        Net.cancelId(id)
    }

    override fun isPaused(): Boolean {
        return false
    }

    private suspend fun createFileHash(uri: Uri): String = withContext(Dispatchers.IO) {
        NetConfig.app.contentResolver.openInputStream(uri)?.use {
            it.createSketchedMD5String(totalBytes)
        }.orEmpty()
    }

    /**
     * 尝试闪传
     */
    private suspend fun flashTransfer(fileHash: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Net.get("${Configure.host}/flashTransfer") {
                setId(id)
                param("hash", folderHash)
                param("fileHash", fileHash)
                param("fileName", fileName)
                setHeader("password", Configure.password)
                setClient {
                    callTimeout(1800, TimeUnit.SECONDS)
                    readTimeout(1800, TimeUnit.SECONDS)
                    writeTimeout(1800, TimeUnit.SECONDS)
                    connectTimeout(1800, TimeUnit.SECONDS)
                }
            }.execute<String>().asJSONObject().optInt("code") == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun upload(uri: Uri, fileHash: String): Boolean = withContext(Dispatchers.IO) {
        Net.post("${Configure.host}/upload") {
            setId(id)
            param("file", uri)
            addQuery("hash", folderHash)
            addQuery("fileHash", fileHash)
            setHeader("password", Configure.password)
            setClient {
                callTimeout(1800, TimeUnit.SECONDS)
                readTimeout(1800, TimeUnit.SECONDS)
                writeTimeout(1800, TimeUnit.SECONDS)
                connectTimeout(1800, TimeUnit.SECONDS)
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