package com.jason.cloud.drive.database.downloader

import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.utils.TaskQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class DownloadQueue : TaskQueue<DownloadTask>() {
    companion object {
        val instance by lazy { DownloadQueue() }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var taskObserver: Job? = null

    init {
        onTaskStart(object : OnTaskStartListener<DownloadTask> {
            override fun onTaskStart(task: DownloadTask) {
                saveData(task) {
                    cancelObserver()
                    startTaskObserver(task)
                }
            }
        })

        onTaskDone(object : OnTaskDoneListener<DownloadTask> {
            override fun onTaskDone(task: DownloadTask) {
                cancelObserver()
                saveData(task)
            }
        })
    }

    private fun cancelObserver() {
        taskObserver?.cancel()
    }

    override fun startAll() {
        taskList.forEach {
            if (it.isPaused()) {
                it.status = DownloadTask.Status.QUEUE
            }
        }
        start()
    }

    private fun startTaskObserver(task: DownloadTask) {
        taskObserver = scope.launch {
            var status = DownloadTask.Status.QUEUE
            var progress = 0
            var downloadBytes = 0L

            while (isActive && task.isRunning()) {
                if (task.progress != progress) {
                    progress = task.progress
                    TaskDatabase.instance.getDownloadDao().updateProgress(task.hash, task.progress)
                }
                if (task.downloadBytes != downloadBytes) {
                    downloadBytes = task.downloadBytes
                    TaskDatabase.instance.getDownloadDao()
                        .updateDownloadedBytes(task.hash, task.downloadBytes)
                }
                if (task.status != status) {
                    status = task.status
                    TaskDatabase.instance.getDownloadDao().updateStatus(task.hash, task.status)
                }
                delay(2000)
            }
        }
    }

    private fun saveData(task: DownloadTask, block: (() -> Unit)? = null) {
        thread {
            TaskDatabase.instance.getDownloadDao().put(task.toTaskEntity())
            block?.invoke()
        }
    }

}