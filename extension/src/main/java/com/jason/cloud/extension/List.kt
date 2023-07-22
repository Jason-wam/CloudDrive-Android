package com.jason.cloud.extension

inline fun <T> Collection<T>.forEachWithTotal(action: (total: Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) {
        action(size, item)
    }
}

inline fun <T> Collection<T>.forEachIndexedWithTotal(action: (index: Int, total: Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) {
        action(index++, size, item)
    }
}