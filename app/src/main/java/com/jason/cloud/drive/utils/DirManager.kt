package com.jason.cloud.drive.utils

import android.content.Context
import com.jason.cloud.extension.externalFilesDir
import java.io.File

object DirManager {
    fun getGlideDir(context: Context): File {
        return context.externalFilesDir("glide")
    }

    fun getDownloadDir(context: Context): File {
        return context.externalFilesDir("downloads")
    }
}