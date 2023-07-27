package com.jason.cloud.drive.utils

import android.content.Context
import com.jason.cloud.extension.cacheDirectory
import com.jason.cloud.extension.externalFilesDir
import java.io.File

object DirManager {
    fun getNetDir(context: Context): File {
        return context.cacheDirectory("net")
    }

    fun getGlideDir(context: Context): File {
        return context.cacheDirectory("glide")
    }

    fun getDownloadDir(context: Context): File {
        return context.externalFilesDir("downloads")
    }

    fun getAttachCacheDir(context: Context): File {
        return context.cacheDirectory("attach-files")
    }
}