package com.jason.cloud.drive.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.Serializable


class MediaEntity(val uri: Uri) : Serializable {
    var name = ""
    var size = 0L
    var date = 0L
    var duration = 0L

    companion object {
        fun create(uri: Uri, block: MediaEntity.() -> Unit): MediaEntity {
            val item = MediaEntity(uri)
            block.invoke(item)
            return item
        }
    }

    override fun toString(): String {
        return "MediaEntity(uri=$uri, name='$name', size=$size, date=$date, duration=$duration)"
    }
}

fun Context.scanVideos(): List<MediaEntity> {
    return ArrayList<MediaEntity>().apply {
        val order = MediaStore.Video.Media.DEFAULT_SORT_ORDER
        val externalURI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        contentResolver.query(externalURI, null, null, null, order)?.use {
            val indexId = it.getColumnIndex(MediaStore.Video.Media._ID)
            while (it.moveToNext()) {
                try {
                    val uri = Uri.withAppendedPath(externalURI, it.getLong(indexId).toString())
                    add(MediaEntity.create(uri) {
                        this.name =
                            it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                        this.size =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                        this.date =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)) * 1000
                        this.duration =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

fun Context.scanImages(): List<MediaEntity> {
    return ArrayList<MediaEntity>().apply {
        val order = MediaStore.Images.Media.DEFAULT_SORT_ORDER
        val externalURI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        contentResolver.query(externalURI, null, null, null, order)?.use {
            val indexId = it.getColumnIndex(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                try {
                    val uri = Uri.withAppendedPath(externalURI, it.getLong(indexId).toString())
                    add(MediaEntity.create(uri) {
                        this.name =
                            it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        this.size =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                        this.date =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)) * 1000
                        this.duration = 0
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

fun Context.scanAudios(): List<MediaEntity> {
    return ArrayList<MediaEntity>().apply {
        val order = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        val externalURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        contentResolver.query(externalURI, null, null, null, order)?.use {
            val indexId = it.getColumnIndex(MediaStore.Audio.Media._ID)
            while (it.moveToNext()) {
                try {
                    val uri = Uri.withAppendedPath(externalURI, it.getLong(indexId).toString())
                    add(MediaEntity.create(uri) {
                        this.name =
                            it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                        this.size =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                        this.date =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)) * 1000
                        this.duration =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
