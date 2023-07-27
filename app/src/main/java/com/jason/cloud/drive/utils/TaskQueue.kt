package com.jason.cloud.drive.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.ReentrantLock

open class TaskQueue<T : TaskQueue.Task> {
    val taskList = arrayListOf<T>()
    val clonedTaskList: List<Task>
        get() {
            return ArrayList(taskList)
        }

    private val taskLock = ReentrantLock()
    private var threadSize = 1
    private var onTaskDoneListener: ((T) -> Unit)? = null
    private var onTaskStartListener: ((T) -> Unit)? = null
    private var onTaskListDoneListener: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private var queueMonitor: Job? = null
    private var isActive = true

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
            while (isActive) {
                if (taskLock.isLocked.not()) {
                    emit(ArrayList(taskList))
                    delay(1000)
                }
            }
        }
    }

    fun threadSize(size: Int): TaskQueue<T> {
        this.threadSize = size
        return this
    }

    open fun onTaskDone(listener: (T) -> Unit): TaskQueue<T> {
        this.onTaskDoneListener = listener
        return this
    }

    /**
     * 任务队列执行完毕
     */
    open fun onTaskListDone(listener: () -> Unit): TaskQueue<T> {
        this.onTaskListDoneListener = listener
        return this
    }

    open fun onTaskStart(listener: (T) -> Unit): TaskQueue<T> {
        this.onTaskStartListener = listener
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

    open fun addTask(task: List<T>): TaskQueue<T> {
        taskLock.lock()
        taskList.addAll(task)
        taskLock.unlock()
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
            queueMonitor = scope.launch {
                while (taskList.isNotEmpty()) {
                    delay(1000)
                    if (taskList.isEmpty()) {
                        onTaskListDoneListener?.invoke()
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

    private fun removeDoneTask(task: T) { //移除已完成任务
        taskLock.lock()
        taskList.remove(task)
        taskLock.unlock()
        println("完成任务：${task.getTaskId()}")
        onTaskDoneListener?.invoke(task)
    }

    private suspend fun startNextTask() = withContext(Dispatchers.IO) {
        if (taskList.isEmpty()) {
            queueMonitor?.cancel()
            onTaskListDoneListener?.invoke()
        } else {
            taskList.filter {
                it.isPaused().not() && it.isDone().not() && it.isRunning().not()
            }.forEachIndexed { index, t ->
                if (index < threadSize) {
                    println("启动任务：${t.getTaskId()}")
                    onTaskStartListener?.invoke(t)
                    t.start()
                }
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

        onTaskDoneListener = null
        onTaskListDoneListener = null
        onTaskStartListener = null
        isActive = false
    }
}