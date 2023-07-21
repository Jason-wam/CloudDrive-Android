package com.jason.cloud.drive.database.downloader

import TaskQueue
import com.jason.cloud.drive.database.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
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

    private val scope = CoroutineScope(Dispatchers.IO)
    private var taskObserver: Job? = null

    private fun cancelObserver() {
        taskObserver?.cancel()
    }

    fun startUnFinishedTasks() {
        scope.launch {
            val taskList = TaskDatabase.INSTANCE.getDownloadDao().list().first()
                .filter {
                    it.status != DownloadTask.Status.SUCCEED &&
                            it.status != DownloadTask.Status.FAILED
                }

            if (taskList.isNotEmpty()) {
                println("TaskList >> ${taskList.size}")
                instance.addTask(ArrayList<DownloadTask>().apply {
                    taskList.forEach { task ->
                        add(DownloadTask(task.name, task.url, task.hash, File(task.dir)))
                    }
                })
                instance.start()
            }
        }
    }

    private fun startTaskObserver(task: DownloadTask) {
        taskObserver = scope.launch {
            var status = DownloadTask.Status.QUEUE
            var progress = 0
            var downloadBytes = 0L

            while (isActive && task.isRunning()) {
                if (task.progress != progress) {
                    progress = task.progress
                    TaskDatabase.INSTANCE.getDownloadDao().updateProgress(task.hash, task.progress)
                }
                if (task.downloadBytes != downloadBytes) {
                    downloadBytes = task.downloadBytes
                    TaskDatabase.INSTANCE.getDownloadDao()
                        .updateDownloadedBytes(task.hash, task.downloadBytes)
                }
                if (task.status != status) {
                    status = task.status
                    TaskDatabase.INSTANCE.getDownloadDao().updateStatus(task.hash, task.status)
                }
                delay(2000)
            }
        }
    }

    private fun saveData(task: DownloadTask, block: (() -> Unit)? = null) {
        thread {
            TaskDatabase.INSTANCE.getDownloadDao().put(task.toTaskEntity())
            block?.invoke()
        }
    }

}