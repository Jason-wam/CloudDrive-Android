package com.jason.cloud.drive.database.uploader

import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.utils.extension.toFileSizeString

fun UploadTask.getStatusText(): String {
    return when (status) {
        UploadTask.Status.QUEUE -> "排队等待..."

        UploadTask.Status.CHECKING -> "正在校验文件，请稍候..."

        UploadTask.Status.CONNECTING -> "正在连接服务器..."

        UploadTask.Status.FLASH_UPLOADED -> "文件闪传成功！"

        UploadTask.Status.UPLOADING -> "正在上传文件(${speedBytes.toFileSizeString()}/s)..."

        UploadTask.Status.SUCCEED -> "文件上传成功！"

        UploadTask.Status.FAILED -> "文件上传失败！"
    }
}