package com.jason.cast.util

import android.annotation.SuppressLint
import android.util.Log
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.ProtocolInfo
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.item.AudioItem
import org.fourthline.cling.support.model.item.ImageItem
import org.fourthline.cling.support.model.item.VideoItem
import org.seamless.util.MimeType
import java.text.SimpleDateFormat
import java.util.Date

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-28 3:33
 * @Description: TODO
 */
object MetadataUtil {
    private const val DIDL_LITE_FOOTER = "</DIDL-Lite>"
    private const val DIDL_LITE_HEADER =
        "<?xml version=\"1.0\"?>" + "<DIDL-Lite " + "xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " + "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" " + "xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\">"

    enum class Type {
        IMAGE, VIDEO, AUDIO
    }

    fun createMetadata(url: String, id: String, name: String, type: Type): String {
        val size: Long = 0
        val res = Res(MimeType(ProtocolInfo.WILDCARD, ProtocolInfo.WILDCARD), size, url)
        val creator = "unknow"
        var metadata = ""
        if (type == Type.IMAGE) {
            val imageItem = ImageItem(id, "0", name, creator, res)
            metadata = createItemMetadata(imageItem)
        }
        if (type == Type.VIDEO) {
            val videoItem = VideoItem(id, "0", name, creator, res)
            metadata = createItemMetadata(videoItem)
        }
        if (type == Type.AUDIO) {
            val audioItem = AudioItem(id, "0", name, creator, res)
            metadata = createItemMetadata(audioItem)
        }
        Log.e("MediaCastUtil", "metadata: $metadata")
        return metadata
    }

    @SuppressLint("SimpleDateFormat")
    private fun createItemMetadata(item: DIDLObject): String {
        val metadata = StringBuilder()
        metadata.append(DIDL_LITE_HEADER)
        metadata.append(
            String.format(
                "<item id=\"%s\" parentID=\"%s\" restricted=\"%s\">",
                item.id,
                item.parentID,
                if (item.isRestricted) "1" else "0"
            )
        )
        metadata.append(String.format("<dc:title>%s</dc:title>", item.title))
        var creator = item.creator
        if (creator != null) {
            creator = creator.replace("<".toRegex(), "_")
            creator = creator.replace(">".toRegex(), "_")
        }
        metadata.append(String.format("<upnp:artist>%s</upnp:artist>", creator))
        metadata.append(String.format("<upnp:class>%s</upnp:class>", item.clazz.value))

        val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())

        metadata.append(String.format("<dc:date>%s</dc:date>", time))
        val res = item.firstResource
        if (res != null) {
            // protocol info
            var protocolInfo = ""
            val pi = res.protocolInfo
            if (pi != null) {
                protocolInfo = String.format(
                    "protocolInfo=\"%s:%s:%s:%s\"",
                    pi.protocol,
                    pi.network,
                    pi.contentFormatMimeType,
                    pi.additionalInfo
                )
            }

            // resolution, extra info, not adding yet
            var resolution = ""
            if (res.resolution != null && res.resolution.isNotEmpty()) {
                resolution = String.format("resolution=\"%s\"", res.resolution)
            }

            // duration
            var duration = ""
            if (res.duration != null && res.duration.isNotEmpty()) {
                duration = String.format("duration=\"%s\"", res.duration)
            }

            // res begin
            //metadata.append(String.format("<res %s>", protocolinfo)); // no resolution & duration yet
            metadata.append(String.format("<res %s %s %s>", protocolInfo, resolution, duration))

            // url
            val url = res.value
            metadata.append(url)

            // res end
            metadata.append("</res>")
        }
        metadata.append("</item>")
        metadata.append(DIDL_LITE_FOOTER)
        return metadata.toString()
    }
}