package com.jason.cloud.drive.database.backup

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.drake.net.Net
import com.drake.net.NetConfig
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.utils.scopeNet
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.TaskQueue
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.toMd5String
import com.jason.cloud.utils.MMKVStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BackupTask(val uri: Uri, val fileHash: String, private val folderHash: String) :
    TaskQueue.Task() {
    var id: String = ""
    var totalBytes: Long = 0
    var uploadedBytes: Long = 0
    var speedBytes: Long = 0
    var progress: Int = 0
    var status = Status.QUEUE
    var fileName: String = ""

    enum class Status {
        QUEUE, UPLOADING, FAILED, SUCCEED, FLASH_UPLOADED, CONNECTING
    }

    init {
        this.id = uri.toString().toMd5String()
        if (MMKVStore.with("BackupTask").isExists(id)) {
            val obj = MMKVStore.with("BackupTask").getString(id).asJSONObject()
            this.fileName = obj.getString("name")
            this.totalBytes = obj.getLong("size")
        } else {
            val file = DocumentFile.fromSingleUri(NetConfig.app, uri)
            this.fileName = file?.name ?: ""
            this.totalBytes = file?.length() ?: 0
            MMKVStore.with("BackupTask").put(id, JSONObject().apply {
                put("name", fileName)
                put("size", totalBytes)
            })
        }
    }

    override fun isDone(): Boolean {
        return status == Status.SUCCEED || status == Status.FLASH_UPLOADED || status == Status.FAILED
    }

    override fun isRunning(): Boolean {
        return status == Status.UPLOADING || status == Status.CONNECTING
    }

    fun isSucceed(): Boolean {
        return status == Status.SUCCEED || status == Status.FLASH_UPLOADED
    }

    override fun getTaskId(): Any {
        return uri.toString().toMd5String()
    }

    override fun start(): BackupTask {
        if (isDone()) return this
        if (isRunning()) return this
        status = Status.CONNECTING
        scopeNet {
            //校验文件完成，开始上传文件
            status = Status.UPLOADING
            status = if (flashTransfer()) {
                Status.FLASH_UPLOADED
            } else {
                //闪传失败，开始上传文件
                if (upload()) {
                    Log.e("Uploader", "$fileName >> 上传成功！")
                    Status.SUCCEED
                } else {
                    Log.e("Uploader", "$fileName >> 上传失败！")
                    Status.FAILED
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

    /**
     * 尝试闪传
     */
    private suspend fun flashTransfer(): Boolean = withContext(Dispatchers.IO) {
        try {
            Net.get("${Configure.host}/flashBackup") {
                setId(id)
                param("fileHash", fileHash)
                param("fileName", fileName)
                param("folderHash", folderHash)
                param("deviceName", Configure.deviceName)
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

    private suspend fun upload(): Boolean = withContext(Dispatchers.IO) {
        Net.post("${Configure.host}/backup") {
            setId(id)
            param("file", uri)
            addQuery("fileName", fileName)
            addQuery("fileHash", fileHash)
            addQuery("folderHash", folderHash)
            addQuery("deviceName", Configure.deviceName)
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