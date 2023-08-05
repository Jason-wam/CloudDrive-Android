package com.jason.cloud.media3.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTrackNameProvider
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.model.Media3Track


@SuppressLint("UnsafeOptInUsageError")
fun ExoPlaybackException.toMessage(): String {
    return "$errorCode : $errorCodeName"
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