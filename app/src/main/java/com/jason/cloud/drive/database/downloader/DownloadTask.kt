package com.jason.cloud.drive.database.downloader

import com.drake.net.NetConfig
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.extension.toFileSizeString
import com.jason.cloud.drive.utils.extension.toMd5String
import okhttp3.*
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection

class DownloadTask(val name: String, val url: String, val hash: String, val dir: File) :
    TaskQueue.Task() {
    val file = File(dir, name)

    val id = hash.toMd5String()
    var totalBytes: Long = 0
    var downloadBytes: Long = 0
    var speedBytes: Long = 0
    var progress: Int = 0
    var status = Status.QUEUE

    enum class Status {
        QUEUE, CONNECTING, DOWNLOADING, FAILED, SUCCEED
    }

    private var call: Call? = null
    private var isDone = false
    private var isRunning = false

    override fun start() {
        if (call != null || isRunning) return
        isRunning = true
        status = Status.CONNECTING

        if (file.exists().not()) {
            file.createNewFile()
        }

        val startPos = file.length()

        val request = if (startPos == 0L) {
            println("开始下载完整文件 ...")
            Request.Builder().url(url).build()
        } else {
            println("尝试获取分块：$startPos- ...")
            Request.Builder().url(url)
                .header("Range", "bytes=$startPos-")
                .build()
        }

        call = NetConfig.okHttpClient.newCall(request)
        call?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                isDone = true
                status = Status.FAILED
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    HttpURLConnection.HTTP_PARTIAL -> {
                        println("成功获得分块内容，开始断点续传...")
                        status = Status.DOWNLOADING
                        response.body?.use { body ->
                            FileOutputStream(file, true).writeBody(
                                body,
                                startPos
                            ) { downloadBytes: Long, totalBytes: Long, speedBytes: Long, progress: Int ->
                                this@DownloadTask.totalBytes = totalBytes
                                this@DownloadTask.downloadBytes = downloadBytes
                                this@DownloadTask.speedBytes = speedBytes
                                this@DownloadTask.progress = progress
                            }
                        }
                        println()
                        status = Status.SUCCEED
                    }

                    HttpURLConnection.HTTP_OK -> {
                        println("服务器返回完整文件内容，从头开始下载...")
                        status = Status.DOWNLOADING
                        response.body?.use { body ->
                            FileOutputStream(file, false).writeBody(
                                body,
                                block = { downloadBytes: Long, totalBytes: Long, speedBytes: Long, progress: Int ->
                                    this@DownloadTask.totalBytes = totalBytes
                                    this@DownloadTask.downloadBytes = downloadBytes
                                    this@DownloadTask.speedBytes = speedBytes
                                    this@DownloadTask.progress = progress
                                }
                            )
                        }
                        println()
                        status = Status.SUCCEED
                    }

                    416 -> { //Requested Range not satisfiable
                        println("获得分块内容失败,分块位置超出文件总长度...")
                        status = Status.FAILED
                    }

                    else -> {
                        println("文件下载失败：code = ${response.code}...")
                        status = Status.FAILED
                    }
                }

                isDone = true
            }
        })
    }

    override fun pause() {
        call?.cancel()
        call = null
    }

    override fun stop() {
        call?.cancel()
        call = null
        if (file.exists()) {
            file.delete()
        }
    }

    override fun isDone(): Boolean {
        return status == Status.SUCCEED || status == Status.FAILED
    }

    override fun isRunning(): Boolean {
        return status == Status.DOWNLOADING || status == Status.CONNECTING
    }

    override fun getTaskId(): Any {
        return id
    }

    private fun FileOutputStream.writeBody(
        body: ResponseBody,
        startPos: Long = 0L,
        block: ((downloadBytes: Long, totalBytes: Long, speedBytes: Long, progress: Int) -> Unit)? = null
    ) {
        sink().buffer().use { sink ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var readByte: Int
            val totalBytes = startPos + body.contentLength()
            var downloadBytes: Long = startPos

            var startTime: Long = 0
            var lastBytes: Long = 0

            body.source().inputStream().use { stream ->
                while (stream.read(buffer).also { readByte = it } > 0) {
                    sink.write(buffer, 0, readByte)
                    downloadBytes += readByte

                    val interval = System.currentTimeMillis() - startTime
                    val intervalBytes = downloadBytes - lastBytes
                    if (interval >= 1000) {
                        lastBytes = downloadBytes
                        val speedBytes = (intervalBytes / (interval / 1000f)).toLong()
                        val progress = (downloadBytes / totalBytes.toFloat() * 100).toInt()
                        block?.invoke(downloadBytes, totalBytes, speedBytes, progress)
                        startTime = System.currentTimeMillis()
                        print("\r下载进度：$progress %, $downloadBytes/$totalBytes >> ${speedBytes.toFileSizeString()} /s")
                    }
                }
            }
        }
    }
}