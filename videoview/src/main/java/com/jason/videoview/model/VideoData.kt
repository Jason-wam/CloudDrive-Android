package com.jason.videoview.model

import java.io.Serializable
import java.security.MessageDigest

open class VideoData : Serializable, Cloneable {
    var id: String = ""
    var name: String = ""
    var url: String = ""

    companion object {
        fun create(name: String, url: String): VideoData {
            return VideoData(name, url)
        }
    }

    constructor() : super() {}

    constructor(name: String, url: String) : super() {
        this.id = url.toMd5String()
        this.name = name
        this.url = url
    }

    constructor(id: String, name: String, url: String) {
        this.id = id
        this.name = name
        this.url = url
    }

    private fun String.toMd5String(): String {
        val md: MessageDigest = MessageDigest.getInstance("MD5")
        md.update(this.toByteArray())
        val b: ByteArray = md.digest()
        var i: Int
        val buf = StringBuffer()
        for (offset in b.indices) {
            i = b[offset].toInt()
            if (i < 0) {
                i += 256
            }
            if (i < 16) {
                buf.append("0")
            }
            buf.append(Integer.toHexString(i))
        }
        return buf.toString()
    }

    public override fun clone(): VideoData {
        return VideoData().apply {
            this.id = this@VideoData.id
            this.url = this@VideoData.url
            this.name = this@VideoData.name
        }
    }
}