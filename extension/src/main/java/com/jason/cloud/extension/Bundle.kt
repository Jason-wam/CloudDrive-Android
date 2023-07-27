package com.jason.cloud.extension

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

@Suppress("DEPRECATION", "UNCHECKED_CAST")
inline fun <reified T : Serializable> Bundle.getSerializableListExtraEx(
    name: String
): List<T> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(name, Serializable::class.java)?.let { it as List<T> } ?: emptyList()
    } else {
        getSerializable(name)?.let { it as List<T> } ?: emptyList()
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