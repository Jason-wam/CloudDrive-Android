package com.jason.cloud.drive.adapter

import android.content.Context
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.ColorSpan
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindRvAdapter
import com.jason.cloud.drive.databinding.ItemMountedDirBinding
import com.jason.cloud.drive.model.MountedDirEntity

class MountedDirsAdapter :
    BaseBindRvAdapter<MountedDirEntity, ItemMountedDirBinding>(R.layout.item_mounted_dir) {
    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemMountedDirBinding>,
        position: Int,
        item: MountedDirEntity
    ) {
        holder.binding.indicator.max = 100000
        holder.binding.indicator.setProgressCompat(
            (item.usedStorage / item.totalStorage.toFloat() * 100000).toInt(),
            true
        )

        holder.binding.tvName.text = item.name
        holder.binding.tvStorage.text = buildString {
            append(item.usedStorageText)
            append(" / ")
            append(item.totalStorageText)
        }.replaceSpan(item.totalStorageText) {
            ColorSpan(context, R.color.storageTrackColor)
        }.replaceSpan(item.usedStorageText) {
            ColorSpan(context, R.color.storageDriveUsedTrackColor)
        }
    }
}