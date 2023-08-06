package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTrackNameProvider
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.model.Media3Track


@SuppressLint("UnsafeOptInUsageError")
fun ExoPlaybackException.toZhMessage(): String {
    return "$errorCode : " + getErrorCodeNameInZh()
}

@SuppressLint("UnsafeOptInUsageError")
fun ExoPlaybackException.getErrorCodeNameInZh(): String {
    return when (errorCode) {
        PlaybackException.ERROR_CODE_UNSPECIFIED -> "未知错误"
        PlaybackException.ERROR_CODE_REMOTE_ERROR -> "远程错误"
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "BEHIND_LIVE_WINDOW(??没理解??)"
        PlaybackException.ERROR_CODE_TIMEOUT -> "请求媒体超时"
        PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "运行时检查出错"
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "未知的输入输出错误"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时"
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "无效的HTTP内容类型"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "错误的HTTP状态码"
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "访问的文件不存在"
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "无权限访问文件"
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "不允许明文传输"
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "读取位置超出文件大小范围"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "媒体容器解析错误"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "媒体清单解析错误"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "不支持的媒体容器类型"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "不支持的媒体清单类型"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "初始化解码器失败"
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "查询解码器求失败"
        PlaybackException.ERROR_CODE_DECODING_FAILED -> "解码媒体文件失败"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "解码格式超出能力范围"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "解码格式不受支持"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "音轨初始化失败"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "音轨写入失败"
        PlaybackException.ERROR_CODE_DRM_UNSPECIFIED -> "未指定的数字版权(DRM)"
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED -> "不支持的数字版权(DRM)"
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> "数字版权(DRM)配置失败"
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> "错误的数字版权(DRM)内容"
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> "数字版权(DRM)许可证获取失败"
        PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION -> "数字版权(DRM)被拒绝"
        PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> "数字版权(DRM)系统错误"
        PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> "数字版权(DRM)设备已撤销"
        PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> "数字版权(DRM)许可证已过期"
        PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED -> "视频帧处理器初始化失败"
        PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED -> "视频帧处理失败"
        else -> "$errorCode : $errorCodeName"
    }
}

fun ExoPlayer.getCurrentMedia3Item(): Media3Item? {
    val tag = currentMediaItem?.localConfiguration?.tag ?: return null
    if (tag !is Media3Item) return null
    return tag
}

fun ExoPlayer.getMedia3ItemAt(index: Int): Media3Item? {
    if (index in 0 until mediaItemCount) {
        val tag = getMediaItemAt(index).localConfiguration?.tag
        if (tag !is Media3Item) return null
        return tag
    }
    return null
}

@SuppressLint("UnsafeOptInUsageError")
fun ExoPlayer?.getTrackList(context: Context, type: @C.TrackType Int): List<Media3Track> {
    this ?: return emptyList()
    val player = this
    val trackNameProvider = DefaultTrackNameProvider(context.resources)
    return ArrayList<Media3Track>().apply {
        player.currentTracks.groups.filter {
            it.type == type
        }.forEach { group ->
            if (group.type == type) {
                for (i in 0 until group.mediaTrackGroup.length) {
                    val format = group.getTrackFormat(i)
                    if (format.id != null) {
                        Log.i(
                            "ExoPlayer",
                            "track: ${format.label} >> ${group.isSelected}"
                        )
                        if (format.label == null) {
                            add(
                                Media3Track(
                                    format.id!!,
                                    trackNameProvider.getTrackName(format),
                                    type,
                                    group.isSelected
                                )
                            )
                        } else {
                            add(
                                Media3Track(
                                    format.id!!,
                                    trackNameProvider.getTrackName(format) + "(${format.label})",
                                    type,
                                    group.isSelected
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
fun ExoPlayer?.selectTrack(track: Media3Track) {
    this ?: return
    Log.i("TrackSelector", "selectTrack > ${track.name}")
    currentTracks.groups.filter {
        it.type == track.type
    }.forEach { group ->
        for (i in 0 until group.mediaTrackGroup.length) {
            val id = group.getTrackFormat(i).id
            if (id == track.id) {
                val selection = TrackSelectionOverride(group.mediaTrackGroup, 0)
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setOverrideForType(selection).build()
                break
            }
        }
    }
}