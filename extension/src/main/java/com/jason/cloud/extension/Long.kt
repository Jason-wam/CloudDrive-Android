package com.jason.cloud.extension

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date


fun Long.toFileSizeString(suffix: String): String {
    return toFileSizeString() + suffix
}

/**
 * 转换为文件单位
 */
fun Long.toFileSizeString(): String {
    val fileSize = this
    val sizeMB = 1024L * 1024L
    val sizeGB = sizeMB * 1024L
    return if (fileSize >= sizeGB) {
        String.format("%.2f GB", fileSize.toFloat() / sizeGB.toFloat())
    } else {
        when {
            fileSize >= sizeMB -> {
                val size = fileSize.toFloat() / sizeMB.toFloat()
                if (size > 100.0f) {
                    String.format("%.0f MB", size)
                } else {
                    String.format("%.1f MB", size)
                }
            }

            fileSize >= 1024L -> {
                val size = fileSize.toFloat() / 1024L.toFloat()
                if (size > 100.0f) {
                    String.format("%.0f KB", size)
                } else {
                    String.format("%.1f KB", size)
                }
            }

            else -> {
                String.format("%d B", fileSize)
            }
        }
    }
}


/**
 * 将时间戳转换为日期
 */
@SuppressLint("SimpleDateFormat")
fun Long.toDateString(pattern: String = "yyyy-MM-dd"): String {
    val df = SimpleDateFormat(pattern) //yyyy-MM-dd HH:mm:ss.SSS
    return df.format(Date(this))
}

/**
 * 时间戳转日期精确到分钟
 */
@SuppressLint("SimpleDateFormat")
fun Long.toDateMinuteString(): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm") //yyyy-MM-dd HH:mm:ss.SSS
    return df.format(Date(this))
}

/**
 * 时间戳转日期精确到秒
 */
@SuppressLint("SimpleDateFormat")
fun Long.toDateSecondsString(): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") //yyyy-MM-dd HH:mm:ss.SSS
    return df.format(Date(this))
}

/**
 * 时间戳转日期精确到毫秒
 */
@SuppressLint("SimpleDateFormat")
fun Long.toDateMillisecondString(): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") //yyyy-MM-dd HH:mm:ss.SSS
    return df.format(Date(this))
}