package com.jason.cloud.drive.database.uploader

import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.utils.TaskQueue
import kotlin.concurrent.thread

class UploadQueue : TaskQueue<UploadTask>() {
    companion object {
        val instance by lazy { UploadQueue() }
    }

    init {
        onTaskDone(object : OnTaskDoneListener<UploadTask> {
            override fun onTaskDone(task: UploadTask) {
                thread {
                    TaskDatabase.instance.getUploadDao().put(UploadTaskEntity().apply {
                        this.id = task.id
                        this.uri = task.uri.toString()
                        this.hash = task.folderHash
                        this.fileName = task.fileName
                        this.fileHash = task.fileHash
                        this.progress = task.progress
                        this.totalBytes = task.totalBytes
                        this.uploadedBytes = task.uploadedBytes
                        this.status = task.status
                        this.timestamp = System.currentTimeMillis()
                        this.progress = if (task.isSucceed()) 100 else task.progress
                    })
                }
            }
        })
    }
}