package com.jason.cloud.drive.utils.uploader

import com.jason.cloud.drive.utils.extension.toFileSizeString

fun Uploader.getStatusText(): String {
    return when (status) {
        Uploader.Status.QUEUE -> "排队等待..."

        Uploader.Status.CHECKING -> "正在校验文件，请稍候..."

        Uploader.Status.UPLOADING -> "正在上传文件(${speedBytes.toFileSizeString()}/s)..."

        Uploader.Status.FAILED -> "文件上传失败！"

        Uploader.Status.SUCCEED -> "文件上传成功！"

        Uploader.Status.FLASH_UPLOADED -> "文件闪传成功！"

        Uploader.Status.PREPARING -> "文件上传准备..."
    }
}