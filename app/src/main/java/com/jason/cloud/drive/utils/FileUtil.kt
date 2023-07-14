package com.jason.cloud.drive.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import java.io.File

object FileUtil {

    fun getCacheDir(context: Context): File {
        return context.externalCacheDir?:context.cacheDir
    }

    fun getCacheDir(context: Context, dirName: String): File {
        var dir = context.externalCacheDir
        return if (dir != null) {
            dir = File(dir, dirName)
            dir.mkdirs()
            dir
        } else {
            dir = File(context.cacheDir, dirName)
            dir.mkdirs()
            dir
        }
    }

    fun getExternalDir(context: Context, dirName: String): File {
        var dir = context.getExternalFilesDir(dirName)
        return if (dir != null) {
            dir.mkdirs()
            dir
        } else {
            dir = File(context.filesDir, dirName)
            dir.mkdirs()
            dir
        }
    }

    fun getAllMountedVolumes(context: Context): List<File> {
        val list = ArrayList<File>()
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                storageManager.storageVolumes.forEach {
                    if (it.state == Environment.MEDIA_MOUNTED) {
                        val clazz = Class.forName(it::class.java.name)
                        val filed = clazz.getDeclaredField("mPath")
                        filed.isAccessible = true
                        val mPath = filed.get(it) as File
                        list.add(mPath)
                    }
                }
            } else {
                val clazz = Class.forName(storageManager::class.java.name)
                val method = clazz.getMethod("getVolumePaths")
                method.isAccessible = true
                val paths = method.invoke(storageManager) as Array<*>
                for (path in paths) {
                    val state =
                        StorageManager::class.java.getMethod("getVolumeState", String::class.java)
                            .invoke(storageManager, path) as String
                    if (state == Environment.MEDIA_MOUNTED) {
                        list.add(File(path as String))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (list.isEmpty()) {
                list.add(Environment.getExternalStorageDirectory())
            }
        }
        return list.sortedBy { it.name }
    }
}