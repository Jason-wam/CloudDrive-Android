package com.jason.cloud.drive.database.downloader

import com.jason.cloud.drive.utils.extension.toFileSizeString


fun DownloadTask.getStatusText(): String {
    return when (status) {
        DownloadTask.Status.QUEUE -> "排队等待..."

        DownloadTask.Status.CONNECTING -> "正在连接服务器..."

        DownloadTask.Status.DOWNLOADING -> "正在取回文件(${speedBytes.toFileSizeString()}/s)..."

        DownloadTask.Status.SUCCEED -> "文件取回成功！"

        DownloadTask.Status.FAILED -> "文件取回失败！"
    }
}