package com.jason.cloud.media3.utils

import java.io.File

object Media3Configure {
    var cachePoolDir: File? = null
    var cachePoolSize: Long = 1024L * 1024 * 1024 * 3
}