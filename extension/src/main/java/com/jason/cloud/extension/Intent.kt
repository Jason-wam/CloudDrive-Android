package com.jason.cloud.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("DEPRECATION", "UNCHECKED_CAST")
inline fun <reified T : Serializable> Intent.getSerializableListExtraEx(
    name: String
): List<T> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(name, Serializable::class.java)?.let { it as List<T> } ?: emptyList()
    } else {
        getSerializableExtra(name)?.let { it as List<T> } ?: emptyList()
    }
}

fun Intent.putSerializableListExtra(name: String, serializableList: List<Serializable>) {
    putExtra(name, serializableList as Serializable)
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