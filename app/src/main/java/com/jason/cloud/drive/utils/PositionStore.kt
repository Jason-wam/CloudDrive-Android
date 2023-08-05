package com.jason.cloud.drive.utils

import android.util.Log
import com.jason.cloud.media3.utils.MediaPositionStore
import com.jason.cloud.media3.utils.PlayerUtils
import com.jason.cloud.utils.MMKVStore

class PositionStore : MediaPositionStore {
    private val mmkv by lazy { MMKVStore.with("PositionStore") }

    override fun get(url: String): Long {
        return mmkv.getLong(url).also {
            Log.i("PositionStore", "get: $url >> ${PlayerUtils.stringForTime(it)}")
        }
    }

    override fun save(url: String, position: Long) {
        Log.i("PositionStore", "save: $url >> $position")
        mmkv.put(url, position)
    }

    override fun remove(url: String) {
        Log.i("PositionStore", "remove: $url")
        mmkv.remove(url)
    }
}