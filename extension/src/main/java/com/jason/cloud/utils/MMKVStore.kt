package com.jason.cloud.utils

import android.content.Context
import com.jason.cloud.extension.toMd5String
import com.tencent.mmkv.MMKV
import org.json.JSONObject

class MMKVStore private constructor(group: String) {
    private val mmkv = MMKV.mmkvWithID(group.toMd5String())

    companion object {
        private val hashMap = HashMap<String, MMKVStore>()

        fun with(group: String): MMKVStore {
            val name = group.toMd5String()
            return hashMap[name] ?: MMKVStore(name).also {
                hashMap[name] = it
            }
        }

        fun init(context: Context){
            MMKV.initialize(context)
        }
    }

    fun put(key: String, value: Int): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: Long): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: String): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: Double): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: Float): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: Boolean): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: ByteArray): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: Set<String>): Boolean {
        return mmkv.encode(key, value)
    }

    fun put(key: String, value: JSONObject): Boolean {
        return mmkv.encode(key, value.toString())
    }

    fun getInt(key: String, defValue: Int = 0): Int {
        return mmkv.decodeInt(key, defValue)
    }

    fun getLong(key: String, defValue: Long = 0): Long {
        return mmkv.decodeLong(key, defValue)
    }

    fun getString(key: String, defValue: String = ""): String {
        return mmkv.decodeString(key) ?: defValue
    }

    fun getDouble(key: String, defValue: Double = 0.0): Double {
        return mmkv.decodeDouble(key, defValue)
    }

    fun getFloat(key: String, defValue: Float = 0.0f): Float {
        return mmkv.decodeFloat(key, defValue)
    }

    fun getBool(key: String, defValue: Boolean = false): Boolean {
        return mmkv.decodeBool(key, defValue)
    }

    fun getBytes(key: String, defValue: ByteArray = byteArrayOf()): ByteArray {
        return mmkv.decodeBytes(key) ?: defValue
    }

    fun getStringSet(key: String, defValue: Set<String> = setOf()): Set<String> {
        return mmkv.decodeStringSet(key) ?: defValue
    }

    fun allKeys(): List<String> {
        return mmkv.allKeys().orEmpty().toList()
    }

    fun all(): Map<String,*> {
        return mmkv.all
    }

    fun isExists(key: String): Boolean {
        return mmkv.containsKey(key)
    }

    fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    fun count(): Long {
        return mmkv.count()
    }

    fun clear() {
        mmkv.clearAll()
    }
}