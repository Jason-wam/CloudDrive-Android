package com.jason.cloud.drive.adapter

import android.content.Context
import androidx.core.view.isVisible
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindHeaderRvAdapter
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.databinding.ItemFileBrowserBinding
import com.jason.cloud.drive.extension.toDateMinuteString
import com.jason.cloud.drive.extension.toFileSizeString
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.MediaType
import com.jason.cloud.drive.utils.MediaType.Media.*
import com.jason.cloud.drive.extension.glide.loadIMG
import com.jason.cloud.drive.utils.Configure

class FileAdapter :
    BaseBindHeaderRvAdapter<FileEntity, ItemFileBrowserBinding>(R.layout.item_file_browser) {
    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemFileBrowserBinding>,
        position: Int,
        item: FileEntity
    ) {
        if (item.isDirectory.not()) {
            if (item.hasImage.not()) {
                holder.binding.flCoverLayout.isVisible = false
                holder.binding.ivCover.isVisible = false
                holder.binding.ivIcon.isVisible = true
                holder.binding.ivIcon.setImageResource(getFileIcon(item.name))
            } else {
                holder.binding.flCoverLayout.isVisible = false
                holder.binding.ivIcon.isVisible = false
                holder.binding.ivCover.isVisible = true
                holder.binding.ivCover.loadIMG("${Configure.hostURL}/thumbnail?hash=${item.hash}") {
                    placeholder(getFileIcon(item.name))
                }
            }
        } else {
            if (item.hasImage.not()) {
                holder.binding.flCoverLayout.isVisible = false
                holder.binding.ivCover.isVisible = false
                holder.binding.ivIcon.isVisible = true
                holder.binding.ivIcon.setImageResource(R.drawable.ic_round_file_folder_24)
            } else {
                holder.binding.flCoverLayout.isVisible = true
                holder.binding.ivIcon.isVisible = false
                holder.binding.ivCover.isVisible = false
                holder.binding.ivCoverCenter.loadIMG("${Configure.hostURL}/thumbnail?hash=${item.hash}") {
                    placeholder(R.color.colorCardViewBackground)
                }
            }
        }

        holder.binding.tvName.text = item.name
        holder.binding.tvDate.text = item.date.toDateMinuteString()
        holder.binding.tvSize.text = if (item.isDirectory) {
            "${item.childCount} 个项目"
        } else {
            item.size.toFileSizeString()
        }
    }

    private fun getFileIcon(name: String): Int {
        return when (MediaType.getMediaType(name)) {
            VIDEO -> R.drawable.ic_round_file_video_24
            IMAGE -> R.drawable.ic_round_file_image_24
            AUDIO -> R.drawable.ic_round_file_audio_24
            COMPRESS -> R.drawable.ic_round_file_compress_24
            PPT -> R.drawable.ic_round_file_ppt_24
            TEXT -> R.drawable.ic_round_file_text_24
            WORD -> R.drawable.ic_round_file_word_24
            EXCEL -> R.drawable.ic_round_file_excel_24
            APPLICATION -> R.drawable.ic_round_file_apk_24
            DATABASE -> R.drawable.ic_round_file_database_24
            TORRENT -> R.drawable.ic_round_file_url_24
            UNKNOWN -> R.drawable.ic_round_file_unknown_24
        }
    }
}