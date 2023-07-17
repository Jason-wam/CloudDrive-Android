package com.jason.cloud.drive.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat.getParcelableArrayList
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.reflect.KClass

inline fun Fragment.startIntent(block: Intent.() -> Unit) {
    context?.startIntent(block)
}

inline fun Context.startIntent(block: Intent.() -> Unit) {
    try {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        block.invoke(intent)
        startActivity(intent)
    } catch (e: Exception) {
        toast(e.toMessage())
    }
}

fun Fragment.openURL(url: String) {
    context?.openURL(url)
}

fun Context.openURL(url: String) {
    try {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.data = Uri.parse(url)
        startActivity(intent)
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
        startActivity(intent)
    } catch (e: Exception) {
        toast(e.toMessage())
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Intent.getSerializableExtraEx(
    name: String,
    clazz: Class<T>
): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(name, clazz)
    } else {
        getSerializableExtra(name)?.let {
            if (it is T) it else null
        }
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Serializable> Bundle.getSerializableEx(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(name, clazz)
    } else {
        getSerializable(name)?.let {
            if (it is T) it else null
        }
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableExtraEx(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    } else {
        getParcelableExtra(name)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableArrayListEx(
    name: String,
    clazz: Class<T>
): List<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(name, clazz)
    } else {
        getParcelableArrayListExtra(name)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.getParcelableExtraEx(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(name, clazz)
    } else {
        getParcelable(name)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Bundle.getParcelableArrayListEx(
    name: String,
    clazz: Class<T>
): List<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayList(name, clazz)
    } else {
        getParcelableArrayList(name)
    }
}

fun Activity?.startActivity(
    cls: KClass<*>,
    newTask: Boolean = false,
    block: (Intent.() -> Unit)? = null
) {
    this?.startActivityEx(cls, newTask, block)
}

fun Fragment?.startActivity(
    cls: KClass<*>,
    newTask: Boolean = false,
    block: (Intent.() -> Unit)? = null
) {
    this?.context?.startActivityEx(cls, newTask, block)
}

fun Context?.startActivity(
    cls: KClass<*>,
    newTask: Boolean = false,
    block: (Intent.() -> Unit)? = null
) {
    this?.startActivityEx(cls, newTask, block)
}

private fun Context.startActivityEx(
    cls: KClass<*>,
    newTask: Boolean = false,
    block: (Intent.() -> Unit)? = null
) {
    val intent = Intent(this, cls.java)
    if (newTask) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    block?.invoke(intent)
    this.startActivity(intent)
}