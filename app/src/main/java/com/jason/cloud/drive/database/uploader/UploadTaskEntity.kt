package com.jason.cloud.drive.database.uploader

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jason.cloud.drive.utils.ItemSelector

@Keep
@Entity(tableName = "uploader")
class UploadTaskEntity : ItemSelector.SelectableItem {
    @PrimaryKey
    var id: String = ""
    var uri: String = ""
    var hash: String = ""
    var fileName: String = ""
    var fileHash: String = ""
    var progress: Int = 0
    var totalBytes: Long = 0
    var uploadedBytes: Long = 0
    var timestamp: Long = 0
    var status: UploadTask.Status = UploadTask.Status.QUEUE

    override fun primaryKey(): Any {
        return id
    }
}