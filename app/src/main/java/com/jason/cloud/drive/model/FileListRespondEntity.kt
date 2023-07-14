package com.jason.cloud.drive.model

data class FileListRespondEntity(
    val hash: String,
    val name: String,
    val path: String,
    val list: List<FileEntity>
)