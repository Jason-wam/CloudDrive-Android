package com.jason.cloud.drive.adapter

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.databinding.ItemCloudFilePathIndicatorBinding
import com.jason.cloud.drive.model.FileIndicatorEntity

@SuppressLint("NotifyDataSetChanged")
class CloudFilePathIndicatorAdapter :
    BaseBindRvAdapter<FileIndicatorEntity, ItemCloudFilePathIndicatorBinding>(R.layout.item_cloud_file_path_indicator) {

    var currentHash: String? = null

    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemCloudFilePathIndicatorBinding>,
        position: Int,
        item: FileIndicatorEntity
    ) {
        holder.binding.tvPath.text = item.name

        val select = currentHash == item.hash
        val activeColor = ContextCompat.getColor(context, R.color.colorOnSurface)
        val inactiveColor = ContextCompat.getColor(context, R.color.colorOnSurfaceMedium)

        if (select) {
            holder.binding.tvPath.setTextColor(activeColor)
            holder.binding.tvPath.setIconTintResource(R.color.colorOnSurface)
        } else {
            holder.binding.tvPath.setTextColor(inactiveColor)
            holder.binding.tvPath.setIconTintResource(R.color.colorOnSurfaceMedium)
        }
    }
}