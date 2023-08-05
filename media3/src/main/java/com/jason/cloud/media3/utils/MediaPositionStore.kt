package com.jason.cloud.media3.utils

interface MediaPositionStore {
    fun get(url: String): Long

    fun save(url: String, position: Long)

    fun remove(url: String)
}