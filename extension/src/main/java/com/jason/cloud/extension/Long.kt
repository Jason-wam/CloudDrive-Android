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
    if (this <= 0) return "0 B"
    val fileSize = this
    val sizes =
        listOf(1L, 1024L, 1024L * 1024L, 1024L * 1024L * 1024L, 1024L * 1024L * 1024L * 1024L)
    val names = listOf("B", "KB", "MB", "GB", "TB")

    return sizes.indexOfLast { fileSize >= it }.let {
        val size = fileSize.toFloat() / sizes[it].toFloat()
        if (size > 100.0f) {
            String.format("%.0f %s", size, names[it])
        } else {
            String.format("%.2f %s", size, names[it])
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