package com.jason.cloud.drive.database.backup

import com.jason.cloud.drive.utils.TaskQueue

class BackupQueue : TaskQueue<BackupTask>() {
    companion object {
        val instance by lazy { BackupQueue() }
    }
}