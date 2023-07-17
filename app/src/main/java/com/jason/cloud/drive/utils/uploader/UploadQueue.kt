package com.jason.cloud.drive.utils.uploader

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.locks.ReentrantLock

class UploadQueue {
    private val tasks = ArrayList<UploadTask>()
    private val taskLock = ReentrantLock()

    companion object {
        val instance by lazy { Uploader() }
    }

    val taskFlow: Flow<HashMap<String, UploadTaskInfo>> by lazy {
        flow {
            while (true) {
                delay(1000)
                emit(HashMap<String, UploadTaskInfo>().apply {
                    taskLock.lock()
                    tasks.forEach {
                        this[it.id] = getTaskInfo(it)
                    }
                    taskLock.unlock()
                })
            }
        }
    }

    private fun getTaskInfo(task: UploadTask): UploadTaskInfo {
        return UploadTaskInfo()
    }
}