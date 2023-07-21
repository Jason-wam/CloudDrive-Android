package com.jason.cloud.drive.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jason.cloud.drive.database.downloader.DownloadDao
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.database.uploader.UploadDao
import com.jason.cloud.drive.database.uploader.UploadTaskEntity

@Database(entities = [UploadTaskEntity::class, DownloadTaskEntity::class], version = 6)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun getUploadDao(): UploadDao

    abstract fun getDownloadDao(): DownloadDao

    companion object {
        lateinit var INSTANCE: TaskDatabase

        fun init(context: Context) {
            INSTANCE = Room.databaseBuilder(
                context,
                TaskDatabase::class.java, "task_data"
            ).fallbackToDestructiveMigration().build()
        }
    }
}