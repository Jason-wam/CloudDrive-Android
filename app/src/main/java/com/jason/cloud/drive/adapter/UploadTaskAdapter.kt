package com.jason.cloud.drive.adapter

import android.annotation.SuppressLint
import android.content.Context
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.database.uploader.UploadTask
import com.jason.cloud.drive.database.uploader.getStatusText
import com.jason.cloud.drive.databinding.ItemUploadTaskBinding
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.FileType.Media.*
import com.jason.cloud.extension.toFileSizeString

class UploadTaskAdapter :
    BaseBindRvAdapter<UploadTask, ItemUploadTaskBinding>(R.layout.item_upload_task) {
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemUploadTaskBinding>,
        position: Int,
        item: UploadTask
    ) {
        holder.binding.ivIcon.setImageResource(getFileIcon(item.fileName))
        holder.binding.tvName.text = item.fileName
        holder.binding.tvStatus.text = item.getStatusText()
        holder.binding.tvSize.text =
            item.uploadedBytes.toFileSizeString() + " / " + item.totalBytes.toFileSizeString()
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