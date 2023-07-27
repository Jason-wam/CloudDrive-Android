package com.jason.cloud.drive.database.downloader

import com.jason.cloud.drive.database.downloader.DownloadTask.Status.CONNECTING
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.DOWNLOADING
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.FAILED
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.PAUSED
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.QUEUE
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.SUCCEED
import com.jason.cloud.extension.toFileSizeString


fun DownloadTask.getStatusText(): String {
    return when (status) {
        QUEUE -> "排队等待..."
        CONNECTING -> "正在连接服务器..."
        DOWNLOADING -> "正在取回文件(${speedBytes.toFileSizeString()}/s)..."
        PAUSED -> "任务已暂停"
        SUCCEED -> "文件取回成功！"
        FAILED -> "文件取回失败！"
    }
}

fun DownloadTaskEntity.getStatusText(): String {
    return when (status) {
        QUEUE -> "排队等待..."
        CONNECTING -> "正在连接服务器..."
        DOWNLOADING -> "正在取回文件(?/s)..."
        PAUSED -> "任务已暂停"
        SUCCEED -> "文件取回成功！"
        FAILED -> "文件取回失败！"
    }
}

fun DownloadTask.toTaskEntity(): DownloadTaskEntity {
    return DownloadTaskEntity().apply {
        this.url = this@toTaskEntity.url
        this.dir = this@toTaskEntity.dir.absolutePath
        this.name = this@toTaskEntity.name
        this.hash = this@toTaskEntity.hash
        this.path = this@toTaskEntity.file.absolutePath
        this.downloadedBytes = this@toTaskEntity.downloadBytes
        this.totalBytes = this@toTaskEntity.totalBytes
        this.status = this@toTaskEntity.status
        this.timestamp = System.currentTimeMillis()
        this.progress = if (status == SUCCEED) 100 else this@toTaskEntity.progress
    }
}