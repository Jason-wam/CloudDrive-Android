package com.jason.cloud.drive.database.uploader

import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.utils.TaskQueue
import kotlin.concurrent.thread

class UploadQueue : TaskQueue<UploadTask>() {
    companion object {
        val instance by lazy { UploadQueue() }
    }

    init {
        onTaskDone {
            thread {
                TaskDatabase.instance.getUploadDao().put(UploadTaskEntity().apply {
                    this.id = it.id
                    this.uri = it.uri.toString()
                    this.hash = it.folderHash
                    this.fileName = it.fileName
                    this.fileHash = it.fileHash
                    this.progress = it.progress
                    this.totalBytes = it.totalBytes
                    this.uploadedBytes = it.uploadedBytes
                    this.status = it.status
                    this.timestamp = System.currentTimeMillis()
                    this.progress = if (it.isSucceed()) 100 else it.progress
                })
            }
        }
    }
}