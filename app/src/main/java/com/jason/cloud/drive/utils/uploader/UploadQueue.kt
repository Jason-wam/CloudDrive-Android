package com.jason.cloud.drive.utils.uploader

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantLock

class UploadQueue {
    val tasks = ArrayList<Uploader>()
    private val taskLock = ReentrantLock()
    private var runningTask: Uploader? = null

    companion object {
        val instance by lazy { UploadQueue() }
    }

    val runningTaskFlow: Flow<List<Uploader>> by lazy {
        flow {
            while (true) {
                if (taskLock.isLocked.not()) {
                    delay(1000)
                    emit(tasks.filter {
                        it.isDone().not()
                    })
                }
            }
        }
    }

    fun upload(uri: Uri, hash: String) {
        taskLock.lock()
        tasks.add(Uploader().apply { setData(uri, hash) })
        taskLock.unlock()
        start()
        runTaskMonitor()
    }

    fun stop(taskId: String) {
        taskLock.lock()
        val foundTask = tasks.find { it.id == taskId }
        if (foundTask != null) {
            foundTask.cancel()
            tasks.remove(foundTask)
        }
        taskLock.unlock()
        start()
    }

    private var taskMonitor: Job? = null
    private fun runTaskMonitor() {
        if (taskMonitor == null || taskMonitor?.isActive == false) {
            Log.e("UploadQueue", "run TaskMonitor...")
            taskMonitor = CoroutineScope(Dispatchers.IO).launch {
                while (true) {
                    delay(1000)
                    start()

                    val isAllDone: Boolean = tasks.none {
                        it.isDone().not()
                    }
                    if (isAllDone) {
                        Log.e("UploadQueue", "cancel TaskMonitor...")
                        break
                    }
                }
            }
        }
    }

    private fun start() {
        val undoneTasks = tasks.filter { it.isDone().not() }
        val hasRunningTask = undoneTasks.find { it.isRunning() } != null
        if (hasRunningTask.not()) { //如果没有正在运行的任务..
            val firstUnDoneTask = undoneTasks.find { it.status == Uploader.Status.QUEUE }
            if (firstUnDoneTask != null) {
                runningTask?.let { saveDoneTask(it) }
                runningTask = firstUnDoneTask
                Log.e("UploadQueue", "start task ${firstUnDoneTask.name} ...")
                firstUnDoneTask.start()
            }
        }
    }

    private fun saveDoneTask(uploader: Uploader) {
        Log.e("UploadQueue", "saveDoneTask ${uploader.name} ...")
    }
}