package com.jason.cloud.drive.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock

open class TaskQueue<T : TaskQueue.Task> {
    val taskList = arrayListOf<T>()
    private var threadSize = 1
    private val taskLock = ReentrantLock()
    private var onTaskDoneListeners = arrayListOf<OnTaskDoneListener<T>>()
    private var onTaskStartListeners = arrayListOf<OnTaskStartListener<T>>()
    private var onTaskListDoneListeners = arrayListOf<OnTaskListDoneListener>()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var queueMonitor: Job? = null

    interface OnTaskDoneListener<T> {
        fun onTaskDone(task: T)
    }

    interface OnTaskStartListener<T> {
        fun onTaskStart(task: T)
    }

    interface OnTaskListDoneListener {
        fun onTaskListDone()
    }

    abstract class Task {
        abstract fun start(): Task
        abstract fun pause()
        abstract fun cancel()

        abstract fun isPaused(): Boolean
        abstract fun isDone(): Boolean
        abstract fun isRunning(): Boolean
        abstract fun getTaskId(): Any
    }

    val taskFlow: Flow<List<T>> by lazy {
        flow {
            while (true) {
                emit(taskList)
                delay(500)
            }
        }
    }

    val taskSizeFlow: Flow<Int> by lazy {
        flow {
            emit(taskList.size)
        }.distinctUntilChanged()
    }

    fun threadSize(size: Int): TaskQueue<T> {
        this.threadSize = size
        return this
    }

    open fun onTaskDone(listener: OnTaskDoneListener<T>): TaskQueue<T> {
        this.onTaskDoneListeners.add(listener)
        return this
    }

    /**
     * 任务队列执行完毕
     */
    open fun onTaskListDone(listener: OnTaskListDoneListener): TaskQueue<T> {
        this.onTaskListDoneListeners.add(listener)
        return this
    }

    open fun onTaskStart(listener: OnTaskStartListener<T>): TaskQueue<T> {
        this.onTaskStartListeners.add(listener)
        return this
    }

    open fun hasRunningTask() = taskList.any { it.isRunning() }

    open fun getRunningTaskList() = ArrayList(taskList.filter { it.isRunning() })

    open fun addTask(task: T): TaskQueue<T> {
        taskLock.lock()
        taskList.add(task)
        taskLock.unlock()
        return this
    }

    open fun addTaskAndStart(task: T): TaskQueue<T> {
        taskLock.lock()
        taskList.add(task)
        taskLock.unlock()
        start()
        return this
    }

    open fun addTask(task: List<T>): TaskQueue<T> {
        taskLock.lock()
        taskList.addAll(task)
        taskLock.unlock()
        return this
    }

    open fun addTaskAndStart(task: List<T>): TaskQueue<T> {
        taskLock.lock()
        taskList.addAll(task)
        taskLock.unlock()
        start()
        return this
    }

    open fun start() {
        queueMonitor?.cancel()
        launchQueueMonitor()
    }

    open fun startAll() {
        taskList.forEach { it.start() }
        start()
    }

    /**
     * 取消全部任务
     * 此操作将清空任务列表
     */
    open fun cancelAll() {
        taskLock.lock()
        taskList.forEach { it.cancel() }
        taskList.clear()
        taskLock.unlock()
    }

    /**
     * 暂停全部任务
     */
    open fun pauseAll() {
        taskList.forEach { it.pause() }
    }

    open fun pause(task: T) {
        taskList.find { it.getTaskId() == task.getTaskId() }?.pause()
    }

    open fun pause(taskId: Any) {
        taskList.find { it.getTaskId() == taskId }?.pause()
    }

    open fun cancel(task: T) {
        val foundTask = taskList.find { it.getTaskId() == task.getTaskId() }
        if (foundTask != null) {
            foundTask.cancel()
            taskLock.lock()
            taskList.remove(foundTask)
            taskLock.unlock()
        }
    }

    open fun cancel(tasks: List<T>) {
        tasks.forEach { task ->
            cancel(task)
        }
    }

    open fun cancelById(taskId: Any) {
        val foundTask = taskList.find { it.getTaskId() == taskId }
        if (foundTask != null) {
            foundTask.cancel()
            taskLock.lock()
            taskList.remove(foundTask)
            taskLock.unlock()
        }
    }

    private fun launchQueueMonitor() {
        if (queueMonitor?.isActive != true) {
            println("启动轮循器...")
            queueMonitor = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    delay(100)
                    if (taskList.isEmpty()) {
                        println("结束轮循器...")
                        withContext(Dispatchers.Main) {
                            onTaskListDoneListeners.forEach { it.onTaskListDone() }
                        }
                        break
                    } else {
                        taskList.filter { it.isDone() }.forEach {
                            removeDoneTask(it)
                        }

                        val runningTaskList = taskList.filter { it.isRunning() }
                        if (runningTaskList.size < threadSize) {
                            startNextTask()
                        }
                    }
                }
            }
        }
    }

    private suspend fun removeDoneTask(task: T) = withContext(Dispatchers.IO) { //移除已完成任务
        taskLock.lock()
        taskList.remove(task)
        taskLock.unlock()
        println("完成任务：${task.getTaskId()}")
        withContext(Dispatchers.Main) {
            onTaskDoneListeners.forEach { it.onTaskDone(task) }
        }
    }

    private suspend fun startNextTask() = withContext(Dispatchers.IO) {
        taskList.filter {
            !it.isPaused() && !it.isDone() && !it.isRunning()
        }.forEachIndexed { index, t ->
            if (index < threadSize) {
                println("启动任务：${t.getTaskId()}")
                withContext(Dispatchers.Main) {
                    onTaskStartListeners.forEach { it.onTaskStart(t) }
                }
                t.start()
            }
        }
    }

    open fun release() {
        taskLock.lock()
        taskList.clear()
        taskLock.unlock()

        queueMonitor?.cancel()
        queueMonitor = null
        scope.cancel()

        onTaskDoneListeners.clear()
        onTaskListDoneListeners.clear()
        onTaskStartListeners.clear()
    }
}