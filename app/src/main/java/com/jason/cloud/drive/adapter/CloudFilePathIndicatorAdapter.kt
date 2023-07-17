package com.jason.cloud.drive.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.databinding.ItemCloudFilePathIndicatorBinding
import com.jason.cloud.drive.model.FileIndicatorEntity
import com.jason.cloud.drive.model.FileNavigationEntity

@SuppressLint("NotifyDataSetChanged")
class CloudFilePathIndicatorAdapter :
    BaseBindRvAdapter<FileNavigationEntity, ItemCloudFilePathIndicatorBinding>(R.layout.item_cloud_file_path_indicator) {

    var currentHash: String = "%root"

    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemCloudFilePathIndicatorBinding>,
        position: Int,
        item: FileNavigationEntity
    ) {
        Log.e("IndicatorAdapter","currentHash = $currentHash : item.hash = ${item.hash}")
        val isActive = currentHash == item.hash
        val activeColor = ContextCompat.getColor(context, R.color.colorOnSurface)
        val inactiveColor = ContextCompat.getColor(context, R.color.colorOnSurfaceMedium)

        holder.binding.tvPath.text = item.name
        if (isActive) {
            holder.binding.tvPath.setTextColor(activeColor)
            holder.binding.tvPath.setIconTintResource(R.color.colorOnSurface)
        } else {
            holder.binding.tvPath.setTextColor(inactiveColor)
            holder.binding.tvPath.setIconTintResource(R.color.colorOnSurfaceMedium)
        }
    }
}