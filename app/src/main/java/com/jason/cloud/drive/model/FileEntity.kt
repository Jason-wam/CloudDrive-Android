package com.jason.cloud.drive.model

import java.io.Serializable

data class FileEntity(
    val name: String,
    val path: String,
    val hash: String,
    val size: Long,
    val date: Long,
    val isFile: Boolean,
    val isDirectory: Boolean,
    val childCount: Int,
    val hasImage: Boolean
) : Serializable