package com.jason.videoview.util

import com.jason.cloud.extension.toMd5String
import com.tencent.mmkv.MMKV
import xyz.doikki.videoplayer.player.ProgressManager

class VideoProgressManager : ProgressManager() {
    private val mmkvId = "VideoProgressManager"

    override fun saveProgress(url: String, position: Long) {
        MMKV.mmkvWithID(mmkvId).encode(url.createKey(), position)
    }

    override fun getSavedProgress(url: String): Long {
        return MMKV.mmkvWithID(mmkvId).decodeLong(url.createKey())
    }

    private fun String.createKey(): String {
        return this.replace(findPort(this), "port").toMd5String()
    }

    //解决本地地址端口不同导致的key变化
    private fun findPort(url: String): String {
        val values = Regex("^(http|https)://.*?:(\\d+)/").find(url)?.groupValues.orEmpty()
        if (values.size >= 3) {
            return values[2]
        }
        return ""
    }

    fun count(): Long {
        return MMKV.mmkvWithID(mmkvId).count()
    }

    fun clear() {
        MMKV.mmkvWithID(mmkvId).clear()
    }
}
