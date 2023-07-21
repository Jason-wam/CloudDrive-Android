package com.jason.cloud.drive.database.uploader

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "uploader")
class UploadTaskEntity {
    @PrimaryKey
    var id: String = ""
    var uri: String = ""
    var hash: String = ""
    var name: String = ""
    var fileHash: String = ""
    var progress: Int = 0
    var totalBytes: Long = 0
    var uploadedBytes: Long = 0
    var timestamp: Long = 0
    var succeed: Boolean = false
}