package com.jason.cloud.drive.adapter

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.view.isVisible
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindHeaderRvAdapter
import com.jason.cloud.drive.databinding.ItemCastDeviceBinding
import org.fourthline.cling.model.meta.RemoteDevice

@SuppressLint("NotifyDataSetChanged")
class MediaCastDeviceAdapter :
    BaseBindHeaderRvAdapter<RemoteDevice, ItemCastDeviceBinding>(R.layout.item_cast_device) {
    private var selectedDevice: RemoteDevice? = null

    init {
        addOnClickObserver { _, item, _ ->
            selectedDevice = item
            notifyDataSetChanged()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        context: Context,
        holder: ViewHolder<ItemCastDeviceBinding>,
        position: Int,
        item: RemoteDevice
    ) {
        holder.binding.tvName.text = item.details.friendlyName.ifBlank { item.displayString }
        holder.binding.tvDescription.text =
            item.details.modelDetails.modelDescription + " - " + item.details.modelDetails.modelName
        holder.binding.ivCheck.isVisible = item == selectedDevice
    }
}