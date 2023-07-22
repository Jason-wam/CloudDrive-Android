package com.jason.cloud.extension

import org.json.JSONArray
import org.json.JSONObject

fun String.asJSONArray(): JSONArray {
    return try {
        JSONArray(this)
    } catch (e: Exception) {
        JSONArray()
    }
}

fun String.asJSONObject(): JSONObject {
    return try {
        JSONObject(this)
    } catch (e: Exception) {
        JSONObject()
    }
}

fun JSONArray.forEachInt(block: (obj: Int) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(getInt(i))
    }
}

fun JSONArray.forEachLong(block: (obj: Long) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(getLong(i))
    }
}

fun JSONArray.forEachDouble(block: (obj: Double) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(getDouble(i))
    }
}

fun JSONArray.forEachBoolean(block: (obj: Boolean) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(getBoolean(i))
    }
}

fun JSONArray.forEachJSONArray(block: (obj: JSONArray) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(getJSONArray(i))
    }
}

fun JSONArray.forEachObject(block: (obj: JSONObject) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(getJSONObject(i))
    }
}

fun JSONArray.forEachString(block: (obj: String) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(getString(i))
    }
}

fun <T> JSONArray.forEachObjectIndexed(block: (index: Int, obj: JSONObject) -> Unit) {
    for (i in 0 until length()) {
        block.invoke(i, getJSONObject(i))
    }
}

fun JSONArray.isEmpty(): Boolean {
    return length() == 0
}

fun JSONArray.isNotEmpty(): Boolean {
    return length() > 0
}

fun JSONObject.isEmpty(): Boolean {
    return length() == 0
}

fun JSONObject.putOptExt(key: String?, value: Any?): JSONObject {
    if (key == null || value == null) {
        return this
    }
    if (value is Boolean && value == false) {
        return this
    }
    if (value is String && value.isBlank()) {
        return this
    }
    if (value is List<*> && value.isEmpty()) {
        return this
    }
    if (value is HashMap<*, *> && value.isEmpty()) {
        return this
    }
    if (value is JSONArray && value.length() == 0) {
        return this
    }
    if (value is JSONObject && value.length() == 0) {
        return this
    }
    return this.put(key, value)
}