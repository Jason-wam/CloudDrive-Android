package com.jason.cloud.drive.adapter

import android.content.Context
import androidx.core.view.isVisible
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.databinding.ItemCloudFileBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.FileType.Media.APPLICATION
import com.jason.cloud.drive.utils.FileType.Media.AUDIO
import com.jason.cloud.drive.utils.FileType.Media.COMPRESS
import com.jason.cloud.drive.utils.FileType.Media.DATABASE
import com.jason.cloud.drive.utils.FileType.Media.EXCEL
import com.jason.cloud.drive.utils.FileType.Media.EXE
import com.jason.cloud.drive.utils.FileType.Media.FOLDER
import com.jason.cloud.drive.utils.FileType.Media.FONT
import com.jason.cloud.drive.utils.FileType.Media.IMAGE
import com.jason.cloud.drive.utils.FileType.Media.PPT
import com.jason.cloud.drive.utils.FileType.Media.TEXT
import com.jason.cloud.drive.utils.FileType.Media.TORRENT
import com.jason.cloud.drive.utils.FileType.Media.UNKNOWN
import com.jason.cloud.drive.utils.FileType.Media.VIDEO
import com.jason.cloud.drive.utils.FileType.Media.WEB
import com.jason.cloud.drive.utils.FileType.Media.WORD
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.toDateMinuteString
import com.jason.cloud.extension.toFileSizeString

class CloudFileAdapter :
    BaseBindRvAdapter<FileEntity, ItemCloudFileBinding>(R.layout.item_cloud_file) {

    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemCloudFileBinding>,
        position: Int,
        item: FileEntity
    ) {
        if (item.isDirectory) {
            when (item.firstFileType) {
                IMAGE, VIDEO, AUDIO -> {
                    holder.binding.flCoverLayout.isVisible = true
                    holder.binding.ivIcon.isVisible = false
                    holder.binding.ivCover.isVisible = false
                    holder.binding.ivCoverCenter.loadIMG(item.thumbnailURL) {
                        placeholder(getFileIcon(item.firstFileType))
                    }
                }

                UNKNOWN -> {
                    holder.binding.ivIcon.isVisible = true
                    holder.binding.ivCover.isVisible = false
                    holder.binding.ivIcon.setImageResource(R.drawable.ic_round_file_folder_24)
                    holder.binding.flCoverLayout.isVisible = false
                }

                else -> {
                    holder.binding.ivIcon.isVisible = false
                    holder.binding.ivCover.isVisible = false
                    holder.binding.ivCoverCenter.setImageResource(getFileIcon(item.firstFileType))
                    holder.binding.flCoverLayout.isVisible = true
                }
            }
        } else {
            when (FileType.getMediaType(item.name)) {
                IMAGE, VIDEO, AUDIO -> {
                    holder.binding.flCoverLayout.isVisible = false
                    holder.binding.ivIcon.isVisible = false
                    holder.binding.ivCover.isVisible = true
                    holder.binding.ivCover.loadIMG(item.thumbnailURL) {
                        placeholder(getFileIcon(item.name))
                    }
                }

                else -> {
                    holder.binding.flCoverLayout.isVisible = false
                    holder.binding.ivCover.isVisible = false
                    holder.binding.ivIcon.isVisible = true
                    holder.binding.ivIcon.setImageResource(getFileIcon(item.name))
                }
            }
        }

        holder.binding.tvName.text = item.name
        holder.binding.icVirtual.isVisible = item.isVirtual
        holder.binding.tvDate.text = item.date.toDateMinuteString()
        holder.binding.tvSize.text = if (item.isDirectory) {
            "${item.childCount} 个项目"
        } else {
            item.size.toFileSizeString()
        }
    }

    private fun getFileIcon(name: String): Int = getFileIcon(FileType.getMediaType(name))

    private fun getFileIcon(type: FileType.Media): Int {
        return when (type) {
            VIDEO -> R.drawable.ic_round_file_video_24
            IMAGE -> R.drawable.ic_round_file_image_24
            AUDIO -> R.drawable.ic_round_file_audio_24
            COMPRESS -> R.drawable.ic_round_file_compress_24
            WEB -> R.drawable.ic_round_file_web_24
            EXE -> R.drawable.ic_round_file_exe_24
            PPT -> R.drawable.ic_round_file_ppt_24
            TEXT -> R.drawable.ic_round_file_text_24
            WORD -> R.drawable.ic_round_file_word_24
            EXCEL -> R.drawable.ic_round_file_excel_24
            APPLICATION -> R.drawable.ic_round_file_apk_24
            DATABASE -> R.drawable.ic_round_file_database_24
            TORRENT -> R.drawable.ic_round_file_url_24
            FONT -> R.drawable.ic_round_file_fonts_24
            FOLDER -> R.drawable.ic_round_file_folder_24
            UNKNOWN -> R.drawable.ic_round_file_unknown_24
        }
    }
}