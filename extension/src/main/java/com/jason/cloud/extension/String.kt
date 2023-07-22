package com.jason.cloud.extension

import android.os.Build
import android.text.Html
import java.security.MessageDigest

fun String?.orDefault(value: String): String {
    return if (this.isNullOrBlank()) {
        value
    } else {
        this
    }
}

fun String.toMd5String(): String {
    val md: MessageDigest = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray())
    val b: ByteArray = md.digest()
    var i: Int
    val buf = StringBuffer()
    for (offset in b.indices) {
        i = b[offset].toInt()
        if (i < 0) {
            i += 256
        }
        if (i < 16) {
            buf.append("0")
        }
        buf.append(Integer.toHexString(i))
    }
    return buf.toString()
}

fun String.htmlToString(): String {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
            Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
        }

        else -> {
            Html.fromHtml(this).toString()
        }
    }
}