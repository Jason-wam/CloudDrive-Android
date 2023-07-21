package com.jason.cloud.drive.database.downloader

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT COUNT(*) FROM downloader")
    fun count(): Int

    @Query("SELECT * FROM downloader ORDER BY timestamp DESC")
    fun list(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM downloader WHERE succeed = :succeed  ORDER BY timestamp DESC")
    fun succeed(succeed: Boolean): Flow<List<DownloadTaskEntity>>

    @Query("DELETE FROM downloader")
    fun clear()

    @Delete
    fun delete(task: DownloadTaskEntity)

    @Query("Delete From downloader where id = :taskId")
    fun deleteById(taskId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(task: DownloadTaskEntity)

    @Query("UPDATE downloader SET progress = :progress where id = :taskId")
    fun updateProgress(taskId: String, progress: Int)

    @Query("UPDATE downloader SET downloadedBytes = :bytes where id = :taskId")
    fun updateDownloadedBytes(taskId: String, bytes: Long)

    @Query("UPDATE downloader SET succeed = :succeed where id = :taskId")
    fun updateSucceed(taskId: String, succeed: Boolean)
}