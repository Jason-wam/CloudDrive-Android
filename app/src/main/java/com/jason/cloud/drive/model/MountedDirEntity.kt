package com.jason.cloud.drive.model

data class MountedDirEntity(
    val hash: String,
    val name: String,
    val usedStorage: Long,
    val totalStorage: Long,
    val selfUsedStorage: Long,
    val usedStorageText: String,
    val totalStorageText: String,
    val selfUsedStorageText: String
)