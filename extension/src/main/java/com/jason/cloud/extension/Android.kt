package com.jason.cloud.extension

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File

@Suppress("DEPRECATION")
fun Context.getKeystore(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val packageInfo = packageManager.getPackageInfoCompat(
            packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val signs = packageInfo.signingInfo.apkContentsSigners
        Base64.encodeToString(signs[0].toByteArray(), 0).toMd5String()
    } else if (Build.VERSION.SDK_INT >= 28) {
        val packageInfo = packageManager.getPackageInfoCompat(
            packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val signs = packageInfo.signingInfo.apkContentsSigners
        Base64.encodeToString(signs[0].toByteArray(), 0).toMd5String()
    } else {
        @SuppressLint("PackageManagerGetSignatures")
        val packageInfo =
            packageManager.getPackageInfoCompat(packageName, PackageManager.GET_SIGNATURES)
        val signs = packageInfo.signatures
        Base64.encodeToString(signs[0].toByteArray(), 0).toMd5String()
    }
}

inline val Context.windowWidth: Int
    get() = run {
        this.resources.displayMetrics.widthPixels
    }

inline val Context.windowHeight: Int
    get() = run {
        this.resources.displayMetrics.heightPixels
    }

inline val Context.windowSize: Pair<Int, Int>
    get() {
        return Pair(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    }

fun Context.getVersionCode(): Long {
    return packageManager.getPackageInfoCompat(this.packageName, PackageManager.GET_ACTIVITIES)
        .let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.longVersionCode
            } else {
                @Suppress("DEPRECATION") it.versionCode.toLong()
            }
        }
}

fun Context.getVersionName(): String {
    return packageManager.getPackageInfoCompat(
        this.packageName,
        PackageManager.GET_ACTIVITIES
    ).versionName
}


fun Context.browser(url: String) {
    kotlin.runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val chooser = Intent.createChooser(intent, "选择应用程序打开")
        startActivity(chooser)
    }.onFailure {
        toast(it.toString())
    }
}

fun Context.openURL(url: String) {
    try {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.data = Uri.parse(url)
        val chooser = Intent.createChooser(intent, "选择应用程序打开")
        startActivity(chooser)
    } catch (e: Exception) {
        toast(e.toMessage())
    }
}

fun Context.openURL(url: String, mimeType: String) {
    try {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(Uri.parse(url), mimeType)
        val chooser = Intent.createChooser(intent, "选择应用程序打开")
        startActivity(chooser)
    } catch (e: Exception) {
        toast(e.toMessage())
    }
}

fun Context.openFile(file: File) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val uri = FileProvider.getUriForFile(this, this.packageName + ".provider", file)
        intent.setDataAndType(uri, file.mimeType)
    } else {
        val uri = Uri.fromFile(file)
        intent.setDataAndType(uri, file.mimeType)
    }
    val chooser = Intent.createChooser(intent, "选择应用程序打开")
    startActivity(chooser)
}

/**
 * 检查包是否存在
 * @param packName 软件包名
 * @return
 */
fun Context?.checkPackExist(packName: String): Boolean {
    var packageInfo: PackageInfo? = null
    try {
        packageInfo = this?.packageManager?.getPackageInfoCompat(packName, 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return packageInfo != null
}

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }
}

fun Context?.openVideoPlayer(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setDataAndType(Uri.parse(url), "video/*")
        this?.startActivity(Intent.createChooser(intent, "选择应用程序打开"))
    } catch (e: Exception) {
        toast(e.toMessage())
    }
}

fun Context.getApplyList(url: String, type: String? = null): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        this.data = Uri.parse(url)
        if (type.isNullOrBlank().not()) {
            this.type = type
        }
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val flags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        packageManager.queryIntentActivities(intent, flags)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        @Suppress("DEPRECATION") packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )
    } else {
        @Suppress("DEPRECATION") packageManager.queryIntentActivities(intent, 0)
    }
}

fun Context.installApk(filePath: String) {
    val apkFile = File(filePath)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val uri = FileProvider.getUriForFile(this, this.packageName + ".provider", apkFile)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
    } else {
        val uri = Uri.fromFile(apkFile)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
    }
    startActivity(intent)
}

fun Context?.sendText(text: String) {
    val intent = Intent("android.intent.action.SEND")
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_SUBJECT, "分享")
    intent.putExtra(Intent.EXTRA_TEXT, text)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    this?.startActivity(Intent.createChooser(intent, "选择发送到"))
}

fun Context.sendEmail(title: String, to: String, subject: String, body: String) {
    val uri = Uri.parse("mailto:$to")
    val intent = Intent(Intent.ACTION_SENDTO, uri)
    intent.putExtra(Intent.EXTRA_EMAIL, to)
    intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    intent.putExtra(Intent.EXTRA_TEXT, body)
    startActivity(Intent.createChooser(intent, title))
}

fun Context?.copy2Clipboard(text: String?): Boolean {
    return this?.getSystemService(Context.CLIPBOARD_SERVICE)?.let {
        it as ClipboardManager
        val clipData = ClipData.newPlainText(null, text)
        it.setPrimaryClip(clipData)
        true
    } ?: false
}

fun Context?.readClipboardText(): String {
    this?.getSystemService(Context.CLIPBOARD_SERVICE)?.let {
        it as ClipboardManager
        val addedText = it.primaryClip?.getItemAt(0)?.text ?: ""
        val addedTextString = addedText.toString()
        if (addedTextString.isNotBlank()) {
            return addedTextString
        }
    }
    return ""
}

