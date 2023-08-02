package com.jason.cloud.media3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jason.cloud.media3.R
import com.jason.cloud.media3.model.TrackSelectEntity

class TrackSelectAdapter : RecyclerView.Adapter<TrackSelectAdapter.ViewHolder>() {
    private val items = ArrayList<TrackSelectEntity>()
    private var selectedPosition = 0
    private var onSelectionChangedListener: ((Int, TrackSelectEntity) -> Unit)? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView by lazy { itemView.findViewById(R.id.tv_title) }
        val checkbox: CheckBox by lazy { itemView.findViewById(R.id.checkbox) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_media3_track_select, parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTitle.text = items[position].name
        holder.checkbox.isChecked = position == selectedPosition
        holder.checkbox.isClickable = position != selectedPosition
        holder.checkbox.setOnCheckedChangeListener { buttonView, _ ->
            if (buttonView.isPressed) {
                selectedPosition = holder.bindingAdapterPosition
                notifyPositionSelected(holder.bindingAdapterPosition)
            }
        }
        holder.itemView.setOnClickListener {
            selectedPosition = holder.bindingAdapterPosition
            notifyPositionSelected(holder.bindingAdapterPosition)
        }
    }

    private fun notifyPositionSelected(position: Int) {
        onSelectionChangedListener?.invoke(position, items[position])
    }

    fun setSelectedPosition(position: Int) {
        this.selectedPosition = position
    }

    fun setOnSelectionChangedListener(listener: (Int, TrackSelectEntity) -> Unit) {
        this.onSelectionChangedListener = listener
    }

    fun setData(data: List<TrackSelectEntity>) {
        items.clear()
        items.addAll(data)
    }

    fun getSelectedItem(): TrackSelectEntity {
        return items[selectedPosition]
    }

    fun getSelectionPosition(): Int {
        return selectedPosition
    }
}