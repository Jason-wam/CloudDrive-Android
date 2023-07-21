package com.jason.cloud.drive.database.downloader

import TaskQueue
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.drake.net.utils.scopeNet
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.uploader.UploadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

object DownloadQueue {
    val instance by lazy {
        TaskQueue<DownloadTask>().onTaskStart {
            saveData(it) {
                cancelObserver()
                startTaskObserver(it)
            }
        }.onTaskDone {
            cancelObserver()
            saveData(it)
        }
    }

    private var taskObserver: Job? = null

    private fun cancelObserver() {
        taskObserver?.cancel()
    }

    fun loadTasks() {
        CoroutineScope(Dispatchers.IO).launch {
            TaskDatabase.INSTANCE.getDownloadDao().succeed(false).collectLatest {
                println("TaskList >> ${it.size}")
                if (it.isNotEmpty()) {
                    instance.addTask(ArrayList<DownloadTask>().apply {
                        it.forEach { task ->
                            add(DownloadTask(task.name, task.url, task.hash, File(task.dir)))
                        }
                    })
                }
            }
        }
    }

    private fun startTaskObserver(task: DownloadTask) {
        taskObserver = CoroutineScope(Dispatchers.IO).launch {
            var status = DownloadTask.Status.QUEUE
            var progress = 0
            var downloadBytes = 0L

            while (isActive && task.isRunning()) {
                delay(2000)
                if (task.progress != progress) {
                    progress = task.progress
                    TaskDatabase.INSTANCE.getDownloadDao().updateProgress(task.id, task.progress)
                }
                if (task.downloadBytes != downloadBytes) {
                    downloadBytes = task.downloadBytes
                    TaskDatabase.INSTANCE.getDownloadDao()
                        .updateDownloadedBytes(task.id, task.downloadBytes)
                }
                if (task.status != status) {
                    status = task.status
                    TaskDatabase.INSTANCE.getDownloadDao().updateSucceed(
                        task.id,
                        task.status == DownloadTask.Status.SUCCEED
                    )
                }
            }
        }
    }

    private fun saveData(task: DownloadTask, block: (() -> Unit)? = null) {
        thread {
            TaskDatabase.INSTANCE.getDownloadDao().put(task.toTaskEntity())
            block?.invoke()
        }
    }

    private fun DownloadTask.toTaskEntity(): DownloadTaskEntity {
        return DownloadTaskEntity().apply {
            this.id = this@toTaskEntity.id
            this.dir = this@toTaskEntity.dir.absolutePath
            this.name = this@toTaskEntity.name
            this.hash = this@toTaskEntity.hash
            this.path = this@toTaskEntity.file.absolutePath
            this.progress = this@toTaskEntity.progress
            this.downloadedBytes = this@toTaskEntity.downloadBytes
            this.totalBytes = this@toTaskEntity.totalBytes
            this.succeed = this@toTaskEntity.status == DownloadTask.Status.SUCCEED
            this.timestamp = System.currentTimeMillis()
        }
    }
}