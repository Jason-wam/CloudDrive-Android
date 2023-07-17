package com.jason.cloud.drive.extension

import android.content.Context
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest


fun File.allFiles(): List<File> {
    if (isDirectory.not()) {
        return listOf(this)
    }

    return ArrayList<File>().apply {
        listFiles()?.forEach {
            if (it.isDirectory) {
                addAll(it.allFiles())
                add(it)
            } else {
                add(it)
            }
        }
    }
}

inline val Context.cacheDirectory: File
    get() {
        return externalCacheDir ?: cacheDir
    }

inline val Context.externalFilesDir: File
    get() {
        return getExternalFilesDir(null) ?: filesDir
    }

fun Context.cacheDirectory(name: String): File {
    return File(cacheDirectory, name).also {
        it.mkdirs()
    }
}

fun Context.externalFilesDir(name: String): File {
    return File(externalFilesDir, name).also {
        it.mkdirs()
    }
}

fun Context.cacheFile(name: String): File {
    return File(cacheDirectory, name).also {
        println(it.absolutePath)
        it.createNewFile()
    }
}

fun Context.externalFile(name: String): File {
    return File(externalFilesDir, name).also {
        println(it.absolutePath)
        it.createNewFile()
    }
}

fun Context.cacheFile(dir: String, name: String): File {
    return File(cacheDirectory(dir), name).also {
        println(it.absolutePath)
        it.createNewFile()
    }
}

fun Context.externalFile(dir: String, name: String): File {
    return File(externalFilesDir(dir), name).also {
        println(it.absolutePath)
        it.createNewFile()
    }
}


fun File.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    return inputStream().use { stream ->
        stream.createMD5String(block)
    }
}

fun File.createSketchedMD5String(): String? {
    return inputStream().use { stream ->
        stream.createSketchedMD5String()
    }
}

fun InputStream.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    return use { stream ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        val messageDigest = MessageDigest.getInstance("MD5")

        while (stream.read(buffer).also { bytesRead = it } != -1) {
            messageDigest.update(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            block?.invoke(totalBytesRead)
        }

        val digest = messageDigest.digest()
        val checksum = BigInteger(1, digest).toString(16)
        checksum.padStart(32, '0')
    }
}

fun InputStream.createSketchedMD5String(): String? {
    try {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        val messageDigest = MessageDigest.getInstance("MD5")

        while (this.read(buffer).also { bytesRead = it } != -1) {
            messageDigest.update(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            if (totalBytesRead > 0.5.MB) {//2MB
                break
            }
        }

        val digest = messageDigest.digest()
        val checksum = BigInteger(1, digest).toString(16)
        return checksum.padStart(32, '0')
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}