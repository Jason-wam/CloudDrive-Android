package com.jason.cloud.drive.adapter

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.databinding.ItemFileBrowserPathIndicatorBinding
import com.jason.cloud.drive.model.FileIndicatorEntity
import java.io.File

@SuppressLint("NotifyDataSetChanged")
class FilePathIndicatorAdapter :
    BaseBindRvAdapter<FileIndicatorEntity, ItemFileBrowserPathIndicatorBinding>(R.layout.item_file_browser_path_indicator) {

    var currentHash: String? = null

    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemFileBrowserPathIndicatorBinding>,
        position: Int,
        item: FileIndicatorEntity
    ) {
        holder.binding.tvPath.text = item.name

        val select = currentHash == item.hash
        val activeColor = ContextCompat.getColor(context, R.color.colorOnSurface)
        val inactiveColor = ContextCompat.getColor(context, R.color.colorOnSurfaceMedium)

        if (select) {
            holder.binding.tvPath.setTextColor(activeColor)
            holder.binding.tvPath.icon = null
        } else {
            holder.binding.tvPath.setTextColor(inactiveColor)
            holder.binding.tvPath.setIconResource(R.drawable.ic_round_chevron_right_16)
        }
    }
}