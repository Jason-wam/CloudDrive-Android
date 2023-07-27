package com.jason.cloud.extension

import android.content.Context
import android.webkit.MimeTypeMap
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

inline val File.mimeType: String
    get() {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
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

/**
 * 校验整个文件的MD5
 */
fun File.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    return inputStream().use { stream ->
        stream.createMD5String(block)
    }
}

fun InputStream.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    val buffer = ByteArray(8 * 1024)
    var bytesRead: Int
    var totalBytesRead: Long = 0
    val messageDigest = MessageDigest.getInstance("MD5")

    while (read(buffer).also { bytesRead = it } != -1) {
        messageDigest.update(buffer)
        totalBytesRead += bytesRead
        block?.invoke(totalBytesRead)
    }

    val digest = messageDigest.digest()
    val checksum = BigInteger(1, digest).toString(16)
    return checksum.padStart(32, '0')
}

/**
 * 因为大文件校验过慢，所以可以选择读取文件开头和结尾
 */
fun File.createSketchedMD5String(blockSize: Long = 2.MB): String {
    return inputStream().use {
        it.createSketchedMD5String(length(), blockSize)
    }
}

fun InputStream.createSketchedMD5String(fileLength: Long, blockSize: Long = 2.MB): String {
    if (blockSize >= fileLength) return createMD5String()
    val messageDigest = MessageDigest.getInstance("MD5")
    var readPoint = readBlock(blockSize) { buffer ->
        messageDigest.update(buffer)
    }

    var nextStart = fileLength / 2 - blockSize / 2
    if (nextStart > 0) {
        val skipOffset = nextStart - readPoint
        skip(skipOffset)
        readPoint += skipOffset + readBlock(blockSize) { buffer ->
            messageDigest.update(buffer)
        }
    }

    nextStart = fileLength - blockSize
    if (nextStart > 0) {
        val skipOffset = nextStart - readPoint
        skip(skipOffset)
        readBlock(blockSize) { buffer ->
            messageDigest.update(buffer)
        }
    }
    val digest = messageDigest.digest()
    val checksum = BigInteger(1, digest).toString(16)
    return checksum.padStart(32, '0')
}

inline fun InputStream.readBlock(blockSize: Long, block: (buffer: ByteArray) -> Unit): Long {
    var bytesRead: Int
    var totalBytesRead: Long = 0
    val buffer = ByteArray(4096)
    while (read(buffer).also { bytesRead = it } != -1) {
        totalBytesRead += bytesRead
        block.invoke(buffer)
        if (totalBytesRead >= blockSize) {
            break
        }
    }
    return totalBytesRead
}