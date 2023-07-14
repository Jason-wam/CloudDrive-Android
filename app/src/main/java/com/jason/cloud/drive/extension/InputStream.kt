package com.jason.cloud.drive.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

suspend fun InputStream.suspendCopyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long = withContext(Dispatchers.IO) {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        if (isActive.not()) {
            bytesCopied = -1
            break
        }
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    bytesCopied
}