package com.jason.cloud.drive.database.downloader

import com.jason.cloud.extension.toFileSizeString


fun DownloadTask.getStatusText(): String {
    return when (status) {
        DownloadTask.Status.QUEUE -> "排队等待..."

        DownloadTask.Status.CONNECTING -> "正在连接服务器..."

        DownloadTask.Status.DOWNLOADING -> "正在取回文件(${speedBytes.toFileSizeString()}/s)..."

        DownloadTask.Status.SUCCEED -> "文件取回成功！"

        DownloadTask.Status.FAILED -> "文件取回失败！"
    }
}

fun DownloadTaskEntity.getStatusText(): String {
    return when (status) {
        DownloadTask.Status.QUEUE -> "排队等待..."

        DownloadTask.Status.CONNECTING -> "正在连接服务器..."

        DownloadTask.Status.DOWNLOADING -> "正在取回文件(?/s)..."

        DownloadTask.Status.SUCCEED -> "文件取回成功！"

        DownloadTask.Status.FAILED -> "文件取回失败！"
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
        this.progress =
            if (status == DownloadTask.Status.SUCCEED) 100 else this@toTaskEntity.progress
    }
}