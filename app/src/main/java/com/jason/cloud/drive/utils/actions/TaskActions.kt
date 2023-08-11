package com.jason.cloud.drive.utils.actions

import androidx.fragment.app.FragmentActivity
import com.drake.net.utils.scopeNetLife
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.ColorSpan
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.downloader.DownloadQueue
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.database.downloader.taskFile
import com.jason.cloud.drive.database.uploader.UploadQueue
import com.jason.cloud.drive.database.uploader.UploadTask
import com.jason.cloud.drive.database.uploader.UploadTaskEntity
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.PositionStore
import com.jason.cloud.drive.views.dialog.DetailOtherDialog
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.activity.VideoPlayActivity
import com.jason.cloud.media3.model.Media3Item
import com.jason.theme.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 播放同目录的所有视频
 */
fun FragmentActivity.viewVideoFiles(list: List<File>, position: Int) {
    val path = list[position].absolutePath
    val videos = list.filter { FileType.isVideo(it.name) }
    val videoIndex = videos.indexOfFirst { it.absolutePath == path }.coerceAtLeast(0)
    VideoPlayActivity.positionStore = PositionStore()
    VideoPlayActivity.open(this, videos.map {
        Media3Item.create(it.name, it.absolutePath, false)
    }, videoIndex)
}

/**
 * 查看其他文件的详细信息，不支持打开
 */
fun FragmentActivity.viewOtherDetail(file: File) {
    DetailOtherDialog(this).setFile(file).showNow(supportFragmentManager, "detail")
}

fun FragmentActivity.showDeleteDownloadTask(
    task: DownloadTask,
    onDeleted: (() -> Unit)? = null
) {
    val text = "是否确认取消任务：${task.name}? 取消后无法恢复！"
    TextDialog(this).setTitle("取消任务")
        .setText(text.replaceSpan(task.name) {
            ColorSpan(this, R.color.colorSecondary)
        }).onPositive("取消") {
            //啥也不做
        }.onNegative("确认取消") {
            deleteDownloadTask(false, task, onDeleted)
        }.onNeutral("保留文件") {
            deleteDownloadTask(true, task, onDeleted)
        }.show()
}

fun FragmentActivity.showDeleteDownloadTasks(
    tasks: List<DownloadTask>,
    onDeleted: (() -> Unit)? = null
) {
    TextDialog(this).setTitle("取消任务")
        .setText("是否确定取消选中的 ${tasks.size} 个任务? 取消后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认取消") {
            deleteDownloadTasks(false, tasks, onDeleted)
        }.onNeutral("保留文件") {
            deleteDownloadTasks(true, tasks, onDeleted)
        }.show()
}

fun FragmentActivity.deleteDownloadTask(
    keepFile: Boolean,
    task: DownloadTask,
    onDeleted: (() -> Unit)? = null
) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        DownloadQueue.instance.cancel(task)
        TaskDatabase.instance.getDownloadDao().delete(task.hash)
        if (keepFile.not()) {
            task.taskFile().delete()
        }
        withContext(Dispatchers.Main) {
            onDeleted?.invoke()
            toast("已删除任务！")
        }
    }
}

fun FragmentActivity.deleteDownloadTasks(
    keepFile: Boolean,
    tasks: List<DownloadTask>,
    onDeleted: (() -> Unit)? = null
) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        DownloadQueue.instance.cancel(tasks)
        tasks.forEach {
            TaskDatabase.instance.getDownloadDao().delete(it.hash)
            if (keepFile.not()) {
                it.taskFile().delete()
            }
        }
        withContext(Dispatchers.Main) {
            onDeleted?.invoke()
            toast("已删除任务！")
        }
    }
}

fun FragmentActivity.showDeleteDownloadDoneTask(
    task: DownloadTaskEntity,
    onDeleted: (() -> Unit)? = null
) {
    val text = "是否确认删除记录：${task.name}? 删除后无法恢复！"
    TextDialog(this).setTitle("删除记录")
        .setText(text.replaceSpan(task.name) {
            ColorSpan(this, R.color.colorSecondary)
        }).onPositive("取消") {
            //啥也不做
        }.onNegative("确认删除") {
            deleteDownloadDoneTask(false, task, onDeleted)
        }.onNeutral("保留文件") {
            deleteDownloadDoneTask(true, task, onDeleted)
        }.show()
}

fun FragmentActivity.showDeleteDownloadDoneTasks(
    tasks: List<DownloadTaskEntity>,
    onDeleted: (() -> Unit)? = null
) {
    TextDialog(this).setTitle("删除记录")
        .setText("是否确定删除选中的 ${tasks.size} 个任务? 删除后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认删除") {
            deleteDownloadDoneTasks(false, tasks, onDeleted)
        }.onNeutral("保留文件") {
            deleteDownloadDoneTasks(true, tasks, onDeleted)
        }.show()
}

fun FragmentActivity.deleteDownloadDoneTask(
    keepFile: Boolean,
    task: DownloadTaskEntity,
    onDeleted: (() -> Unit)? = null
) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        TaskDatabase.instance.getDownloadDao().delete(task)
        if (keepFile.not()) {
            File(task.dir, task.name).delete()
        }
        withContext(Dispatchers.Main) {
            onDeleted?.invoke()
            toast("已删除记录！")
        }
    }
}

fun FragmentActivity.deleteDownloadDoneTasks(
    keepFile: Boolean,
    tasks: List<DownloadTaskEntity>,
    onDeleted: (() -> Unit)? = null
) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        tasks.forEach {
            if (keepFile) {
                TaskDatabase.instance.getDownloadDao().delete(it)
            } else {
                if (it.taskFile().delete()) {
                    TaskDatabase.instance.getDownloadDao().delete(it)
                }
            }
        }
        withContext(Dispatchers.Main) {
            onDeleted?.invoke()
            toast("已删除记录！")
        }
    }
}

fun FragmentActivity.showClearAllDownloadTasks() {
    TextDialog(this).setTitle("取消全部取回任务")
        .setText("是否确定取消全部取回任务? 删除后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认删除") {
            clearAllDownloadTasks(false)
        }.onNeutral("保留文件") {
            clearAllDownloadTasks(true)
        }.show()
}

fun FragmentActivity.clearAllDownloadTasks(keepFile: Boolean) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        DownloadQueue.instance.cancelAll()
        TaskDatabase.instance.getDownloadDao().list().collectLatest {
            it.filter { task ->
                task.status != DownloadTask.Status.SUCCEED && task.status != DownloadTask.Status.PAUSED
            }.forEach { task ->
                if (keepFile) {
                    TaskDatabase.instance.getDownloadDao().delete(it)
                } else {
                    if (task.taskFile().delete()) {
                        TaskDatabase.instance.getDownloadDao().delete(it)
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            toast("已取消全部取回任务！")
        }
    }
}

fun FragmentActivity.showClearAllDownloadDoneTasks() {
    TextDialog(this).setTitle("清空取回记录")
        .setText("是否确定清空取回完成的任务? 删除后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认清空") {
            clearDownloadDoneTasks(false)
        }.onNeutral("保留文件") {
            clearDownloadDoneTasks(true)
        }.show()
}

fun FragmentActivity.clearDownloadDoneTasks(keepFile: Boolean) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        TaskDatabase.instance.getDownloadDao().list().collectLatest {
            it.forEach { task ->
                if (keepFile) {
                    TaskDatabase.instance.getDownloadDao().delete(it)
                } else {
                    if (task.taskFile().delete()) {
                        TaskDatabase.instance.getDownloadDao().delete(it)
                    }
                }
            }
        }
        withContext(Dispatchers.Main) {
            toast("已清空取回完成任务！")
        }
    }
}

fun FragmentActivity.showCancelUploadTasks(
    tasks: List<UploadTask>,
    onDeleted: (() -> Unit)? = null
) {
    TextDialog(this).setTitle("取消任务")
        .setText("是否确定取消选中的 ${tasks.size} 个任务? 取消后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认取消") {
            cancelUploadTasks(tasks, onDeleted)
        }.show()
}

fun FragmentActivity.cancelUploadTasks(
    tasks: List<UploadTask>,
    onDeleted: (() -> Unit)? = null
) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        UploadQueue.instance.cancel(tasks)
        tasks.forEach {
            TaskDatabase.instance.getUploadDao().deleteById(it.id)
        }
        withContext(Dispatchers.Main) {
            onDeleted?.invoke()
            toast("已取消任务！")
        }
    }
}

fun FragmentActivity.showDeleteUploadDoneTasks(
    tasks: List<UploadTaskEntity>,
    onDeleted: (() -> Unit)? = null
) {
    TextDialog(this).setTitle("删除记录")
        .setText("是否确定删除选中的 ${tasks.size} 个记录? 删除后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认删除") {
            deleteUploadDoneTasks(tasks, onDeleted)
        }.show()
}

fun FragmentActivity.deleteUploadDoneTasks(
    tasks: List<UploadTaskEntity>,
    onDeleted: (() -> Unit)? = null
) {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        tasks.forEach {
            TaskDatabase.instance.getUploadDao().deleteById(it.id)
        }
        withContext(Dispatchers.Main) {
            onDeleted?.invoke()
            toast("已删除记录！")
        }
    }
}

fun FragmentActivity.showClearAllUploadTasks() {
    TextDialog(this).setTitle("取消全部上传任务")
        .setText("是否确定取消全部上传任务? 删除后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认取消") {
            clearAllUploadTasks()
        }.show()
}

fun FragmentActivity.clearAllUploadTasks() {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        UploadQueue.instance.cancelAll()
        TaskDatabase.instance.getUploadDao().list().collectLatest {
            it.filter { task ->
                task.status != UploadTask.Status.SUCCEED &&
                        task.status != UploadTask.Status.FLASH_UPLOADED &&
                        task.status != UploadTask.Status.FAILED
            }.forEach { task ->
                TaskDatabase.instance.getUploadDao().delete(task)
            }
        }
        withContext(Dispatchers.Main) {
            toast("已取消全部上传任务！")
        }
    }
}

fun FragmentActivity.showClearAllUploadDoneTasks() {
    TextDialog(this).setTitle("清空上传记录")
        .setText("是否确定清空上传记录? 删除后无法恢复！").onPositive("取消") {
            //啥也不做
        }.onNegative("确认清空") {
            clearAllUploadDoneTasks()
        }.show()
}

fun FragmentActivity.clearAllUploadDoneTasks() {
    scopeNetLife(dispatcher = Dispatchers.IO) {
        TaskDatabase.instance.getUploadDao().list().collectLatest {
            it.filter { task ->
                task.status == UploadTask.Status.SUCCEED ||
                        task.status != UploadTask.Status.FLASH_UPLOADED ||
                        task.status != UploadTask.Status.FAILED
            }.forEach { task ->
                TaskDatabase.instance.getUploadDao().delete(task)
            }
        }
        withContext(Dispatchers.Main) {
            toast("已清空上传记录！")
        }
    }
}