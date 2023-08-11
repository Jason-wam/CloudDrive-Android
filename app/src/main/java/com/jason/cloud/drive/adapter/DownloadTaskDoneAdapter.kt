package com.jason.cloud.drive.adapter

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.view.isVisible
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.database.downloader.getStatusText
import com.jason.cloud.drive.databinding.ItemDownloadTaskDoneBinding
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.FileType.Media.*
import com.jason.cloud.drive.utils.ItemSelector
import com.jason.cloud.extension.toFileSizeString

@SuppressLint("NotifyDataSetChanged")
class DownloadTaskDoneAdapter :
    BaseBindRvAdapter<DownloadTaskEntity, ItemDownloadTaskDoneBinding>(R.layout.item_download_task_done) {
    val selector = ItemSelector<DownloadTaskEntity>().apply {
        addOnSelectListener(object : ItemSelector.OnSelectListener<DownloadTaskEntity> {
            override fun onSelectStart() {

            }

            override fun onSelectCanceled() {
                notifyDataSetChanged()
            }

            override fun onSelectChanged(selects: List<DownloadTaskEntity>) {

            }
        })
    }

    init {
        addOnClickObserver { position, item, _ ->
            if (selector.isInSelectMode) {
                selector.reverseSelect(item)
                notifyItemChanged(position)
            }
        }

        addOnLongClickObserver { position, item, _ ->
            if (selector.isInSelectMode.not()) {
                selector.startSelect()
                selector.reverseSelect(item)
                notifyItemChanged(position)
                notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemDownloadTaskDoneBinding>,
        position: Int,
        item: DownloadTaskEntity
    ) {
        holder.binding.ivIcon.setImageResource(getFileIcon(item.name))
        holder.binding.tvName.text = item.name
        holder.binding.indicator.progress = item.progress
        holder.binding.tvStatus.text = item.getStatusText()
        holder.binding.tvSize.text = item.downloadedBytes.toFileSizeString()
        holder.binding.btnControl.isVisible = selector.isInSelectMode.not()
        holder.binding.checkbox.isVisible = selector.isInSelectMode
        holder.binding.checkbox.isChecked = selector.isSelected(item)
        holder.binding.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                selector.serSelected(item, isChecked)
                notifyItemChanged(position)
            }
        }
    }

    private fun getFileIcon(name: String): Int {
        return when (FileType.getMediaType(name)) {
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
            WEB -> R.drawable.ic_round_file_web_24
            EXE -> R.drawable.ic_round_file_exe_24
            FONT -> R.drawable.ic_round_file_fonts_24
            UNKNOWN -> R.drawable.ic_round_file_unknown_24
            FOLDER -> R.drawable.ic_round_file_folder_24
        }
    }
}