package com.jason.cloud.media3.model

import java.io.Serializable

open class Media3VideoItem : Serializable {
    var url: String = ""
    var title: String = ""
    var image: String = ""
    var subtitle: String = ""
    var cacheEnabled: Boolean = false
    var headers: Map<String, String> = mapOf()

    companion object {
        fun create(title: String, url: String, cache: Boolean = false): Media3VideoItem {
            return Media3VideoItem().apply {
                this.title = title
                this.url = url
                this.cacheEnabled = cache
            }
        }

        fun create(
            title: String,
            subtitle: String,
            url: String,
            cache: Boolean = false
        ): Media3VideoItem {
            return Media3VideoItem().apply {
                this.title = title
                this.subtitle = subtitle
                this.url = url
                this.cacheEnabled = cache
            }
        }
    }
}