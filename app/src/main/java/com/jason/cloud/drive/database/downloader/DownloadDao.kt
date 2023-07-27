package com.jason.cloud.drive.database.downloader

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT COUNT(*) FROM downloader")
    fun count(): Int

    @Query("SELECT * FROM downloader ORDER BY timestamp DESC")
    fun list(): Flow<List<DownloadTaskEntity>>

    @Query("DELETE FROM downloader")
    fun clear()

    @Query("DELETE FROM downloader WHERE status = :status")
    fun clear(status: DownloadTask.Status)

    @Delete
    fun delete(task: DownloadTaskEntity)

    @Delete
    fun delete(vararg tasks: DownloadTaskEntity)

    @Delete
    fun delete(tasks: List<DownloadTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun put(task: DownloadTaskEntity)

    @Query("Delete From downloader where hash = :hash")
    fun deleteByHash(hash: String)

    @Query("UPDATE downloader SET progress = :progress where hash = :hash")
    fun updateProgress(hash: String, progress: Int)

    @Query("UPDATE downloader SET downloadedBytes = :bytes where hash = :hash")
    fun updateDownloadedBytes(hash: String, bytes: Long)

    @Query("UPDATE downloader SET status = :status where hash = :hash")
    fun updateStatus(hash: String, status: DownloadTask.Status)
}