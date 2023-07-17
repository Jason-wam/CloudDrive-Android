package com.jason.cloud.drive.utils.uploader

import androidx.room.Dao
import androidx.room.Query

@Dao
interface UploadDao {
    @Query("SELECT COUNT(*) FROM uploader")
    fun count(): Int

    @Query("SELECT * FROM uploader")
    fun list(): List<UploadTask>

    @Query("DELETE FROM uploader")
    fun clear()
}