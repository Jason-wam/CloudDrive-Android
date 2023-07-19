package com.jason.cloud.drive.utils.uploader

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "uploader")
class UploadTask {
    @PrimaryKey
    var id: String = ""
    var uri: String = ""
    var url: String = ""
    var name: String = ""
    var totalBytes: Long = 0
    var uploadedBytes: Long = 0
    var progress: Int = 0
    var status: String = Uploader.Status.QUEUE.name
}