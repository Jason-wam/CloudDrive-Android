package com.jason.cloud.drive.model

import android.webkit.MimeTypeMap
import com.flyjingfish.openimagelib.beans.OpenImageUrl
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.UrlBuilder
import org.json.JSONObject
import java.io.Serializable


data class FileEntity(
    val name: String,
    val path: String,
    val hash: String,
    val size: Long,
    val date: Long,
    val isFile: Boolean,
    val isDirectory: Boolean,
    val childCount: Int,
    val firstFileHash: String,
    val firstFileType: FileType.Media,
    val isVirtual: Boolean,
    val rawURL: String,
    val gifURL: String,
    val thumbnailURL: String,
) : Serializable {
    companion object {
        fun createFromJson(obj: JSONObject): FileEntity {
            val hash = obj.getString("hash")
            return FileEntity(
                obj.getString("name"),
                obj.getString("path"),
                obj.getString("hash"),
                obj.getLong("size"),
                obj.getLong("date"),
                obj.getBoolean("isFile"),
                obj.getBoolean("isDirectory"),
                obj.getInt("childCount"),
                obj.getString("firstFileHash"),
                obj.getString("firstFileType").let { type -> FileType.Media.valueOf(type) },
                obj.getBoolean("isVirtual"),
                rawURL = createRawUrl(hash),
                gifURL = createThumbnailUrl(hash, true),
                thumbnailURL = createThumbnailUrl(hash, false, 200)
            )
        }

        private fun createRawUrl(hash: String): String {
            return UrlBuilder(Configure.hostURL).path("/file").param("hash", hash).build()
        }

        private fun createThumbnailUrl(hash: String, isGif: Boolean, size: Int = -1): String {
            val builder = UrlBuilder(Configure.hostURL)
            builder.path("/thumbnail")
            builder.param("hash", hash)
            builder.param("isGif", isGif)
            if (size > 0) {
                builder.param("size", size)
            }
            return builder.build()
        }
    }

    fun mimeType(): String {
        val extension = name.substringAfterLast(".")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }

    fun toOpenImageUrl(): OpenImageUrl {
        return object : OpenImageUrl {
            override fun getImageUrl(): String {
                return rawURL
            }

            override fun getVideoUrl(): String {
                return ""
            }

            override fun getCoverImageUrl(): String {
                return thumbnailURL
            }

            override fun getType(): com.flyjingfish.openimagelib.enums.MediaType {
                return com.flyjingfish.openimagelib.enums.MediaType.IMAGE
            }
        }
    }
}

fun List<FileEntity>.toOpenImageUrlList(): List<OpenImageUrl> {
    return arrayListOf<OpenImageUrl>().apply {
        addAll(this@toOpenImageUrlList.map {
            it.toOpenImageUrl()
        })
    }
}