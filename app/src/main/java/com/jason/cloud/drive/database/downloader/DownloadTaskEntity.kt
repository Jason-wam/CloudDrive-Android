package com.jason.cloud.drive.database.downloader

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "downloader")
class DownloadTaskEntity {
    @PrimaryKey
    var id: String = ""
    var url: String = ""
    var dir: String = ""
    var path: String = ""
    var hash: String = ""
    var name: String = ""
    var progress: Int = 0
    var totalBytes: Long = 0
    var downloadedBytes: Long = 0
    var timestamp: Long = 0
    var succeed: Boolean = false
}