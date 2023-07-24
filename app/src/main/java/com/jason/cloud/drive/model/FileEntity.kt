package com.jason.cloud.drive.model

import android.webkit.MimeTypeMap
import com.flyjingfish.openimagelib.beans.OpenImageUrl
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.FileType
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
                rawURL = "${Configure.hostURL}/file?hash=$hash",
                gifURL = "${Configure.hostURL}/thumbnail?hash=$hash&isGif=true",
                thumbnailURL = "${Configure.hostURL}/thumbnail?hash=$hash&size=200"
            )
        }
    }
}

fun FileEntity.mimeType(): String {
    val extension = name.substringAfterLast(".")
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
}

fun FileEntity.toOpenImageUrl(): OpenImageUrl {
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

fun List<FileEntity>.toOpenImageUrlList(): List<OpenImageUrl> {
    return arrayListOf<OpenImageUrl>().apply {
        addAll(this@toOpenImageUrlList.map {
            it.toOpenImageUrl()
        })
    }
}