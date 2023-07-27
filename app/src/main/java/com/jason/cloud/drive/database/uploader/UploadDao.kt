package com.jason.cloud.drive.database.uploader

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadDao {
    @Query("SELECT COUNT(*) FROM uploader")
    fun count(): Int

    @Query("SELECT * FROM uploader ORDER BY timestamp DESC")
    fun list(): Flow<List<UploadTaskEntity>>

    @Query("DELETE FROM uploader")
    fun clear()

    @Query("DELETE FROM uploader WHERE status = :status")
    fun clear(status: UploadTask.Status)

    @Delete
    fun delete(task: UploadTaskEntity)

    @Query("Delete From uploader where id = :taskId")
    fun deleteById(taskId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(task: UploadTaskEntity)
}