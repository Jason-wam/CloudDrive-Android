package com.jason.cloud.drive.database.downloader

import com.drake.net.NetConfig
import com.jason.cloud.drive.utils.ItemSelector
import com.jason.cloud.drive.utils.TaskQueue
import com.jason.cloud.extension.toFileSizeString
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.appendingSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection

class DownloadTask(val name: String, val url: String, val hash: String, val dir: File) :
    ItemSelector.SelectableItem,
    TaskQueue.Task() {
    val file = File(dir, name)

    var status = Status.QUEUE
    var progress: Int = 0
    var totalBytes: Long = 0
    var downloadBytes: Long = 0
    var speedBytes: Long = 0

    enum class Status {
        QUEUE, CONNECTING, DOWNLOADING, PAUSED, FAILED, SUCCEED
    }

    private var call: Call? = null
    private var isDone = false

    override fun start(): DownloadTask {
        if (isRunning()) return this

        status = Status.CONNECTING

        if (dir.exists().not()) {
            dir.mkdirs()
        }
        if (file.exists().not()) {
            file.createNewFile()
        }

        val startPos = file.length()

        val request = if (startPos == 0L) {
            println("开始下载完整文件 ...")
            Request.Builder().url(url).header("Range", "bytes=0-").build()
        } else {
            println("尝试获取分块：$startPos- ...")
            Request.Builder().url(url).header("Range", "bytes=$startPos-").build()
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
                        try {
                            println("成功获得分块内容，开始断点续传...")
                            status = Status.DOWNLOADING
                            response.body?.use { body ->
                                file.appendingSink().buffer().writeBody(
                                    body,
                                    startPos
                                ) { downloadBytes: Long, totalBytes: Long, speedBytes: Long, progress: Int ->
                                    this@DownloadTask.totalBytes = totalBytes
                                    this@DownloadTask.downloadBytes = downloadBytes
                                    this@DownloadTask.speedBytes = speedBytes
                                    this@DownloadTask.progress = progress
                                }
                            }

                            status = Status.SUCCEED
                        } catch (e: Exception) {
                            if (status != Status.PAUSED) {
                                status = Status.FAILED
                                e.printStackTrace()
                            }
                        }
                    }

                    HttpURLConnection.HTTP_OK -> {
                        println("服务器返回完整文件内容，从头开始下载...")
                        try {
                            status = Status.DOWNLOADING
                            response.body?.use { body ->
                                file.sink().buffer().writeBody(body,
                                    block = { downloadBytes: Long, totalBytes: Long, speedBytes: Long, progress: Int ->
                                        this@DownloadTask.totalBytes = totalBytes
                                        this@DownloadTask.downloadBytes = downloadBytes
                                        this@DownloadTask.speedBytes = speedBytes
                                        this@DownloadTask.progress = progress
                                    })
                            }
                            println()
                            status = Status.SUCCEED
                        } catch (e: Exception) {
                            if (status != Status.PAUSED) {
                                status = Status.FAILED
                                e.printStackTrace()
                            }
                        }
                    }

                    416 -> { //Requested Range not satisfiable
                        println("获得分块内容失败,分块位置超出文件总长度...")
                        println(response.headers)
                        status = Status.SUCCEED
                    }

                    else -> {
                        println("文件下载失败：code = ${response.code}...")
                        status = Status.FAILED
                    }
                }

                isDone = true
            }
        })
        return this
    }

    override fun pause() {
        call?.cancel()
        call = null
        status = Status.PAUSED
    }

    override fun cancel() {
        call?.cancel()
        call = null
        if (file.exists()) {
            file.delete()
        }
    }

    fun cancelButSaveFile() {
        call?.cancel()
        call = null
    }

    override fun isPaused(): Boolean {
        return status == Status.PAUSED
    }

    override fun isDone(): Boolean {
        return status == Status.SUCCEED || status == Status.FAILED
    }

    override fun isRunning(): Boolean {
        return status == Status.DOWNLOADING || status == Status.CONNECTING
    }

    override fun getTaskId(): Any {
        return hash
    }

    private fun BufferedSink.writeBody(
        body: ResponseBody,
        startPos: Long = 0L,
        block: ((downloadBytes: Long, totalBytes: Long, speedBytes: Long, progress: Int) -> Unit)? = null
    ) {
        use {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var readByte: Int
            val totalBytes = startPos + body.contentLength()
            var downloadBytes: Long = startPos

            var startTime: Long = 0
            var lastBytes: Long = 0

            body.source().inputStream().use { stream ->
                while (stream.read(buffer).also { readByte = it } > 0) {
                    it.write(buffer, 0, readByte)
                    downloadBytes += readByte

                    val interval = System.currentTimeMillis() - startTime
                    val intervalBytes = downloadBytes - lastBytes
                    if (interval >= 500) {
                        lastBytes = downloadBytes
                        val speedBytes = (intervalBytes / (interval / 1000f)).toLong()
                        val progress = (downloadBytes / totalBytes.toFloat() * 100).toInt()
                        block?.invoke(downloadBytes, totalBytes, speedBytes, progress)
                        startTime = System.currentTimeMillis()
                        println("下载进度：$progress %, $downloadBytes/$totalBytes >> ${speedBytes.toFileSizeString()} /s")
                    }
                }
            }
        }
    }

    override fun primaryKey(): Any {
        return hash
    }
}