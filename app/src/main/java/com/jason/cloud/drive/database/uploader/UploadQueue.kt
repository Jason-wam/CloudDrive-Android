package com.jason.cloud.drive.database.uploader

import TaskQueue
import com.jason.cloud.drive.database.TaskDatabase

object UploadQueue {
    val instance by lazy {
        TaskQueue<UploadTask>().onTaskDone {
            TaskDatabase.INSTANCE.getUploadDao().put(UploadTaskEntity().apply {
                this.id = it.id
                this.uri = it.uri.toString()
                this.hash = it.hash
                this.name = it.name
                this.fileHash = it.fileHash
                this.progress = it.progress
                this.totalBytes = it.totalBytes
                this.uploadedBytes = it.uploadedBytes
                this.succeed = it.status == UploadTask.Status.SUCCEED ||
                        it.status == UploadTask.Status.FLASH_UPLOADED
                this.timestamp = System.currentTimeMillis()
            })
        }
    }
}