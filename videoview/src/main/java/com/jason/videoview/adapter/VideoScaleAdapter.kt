package com.jason.videoview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jason.videoview.R
import com.jason.videoview.model.ScaleEntity

class VideoScaleAdapter(var scaleType: Int) : RecyclerView.Adapter<VideoScaleAdapter.ViewHolder>() {
    private val list = ArrayList<ScaleEntity>()
    private var onSelectListener: OnSelectListener? = null

    fun getData() = list

    fun addData(item: ScaleEntity) = list.add(item)

    fun setData(data: List<ScaleEntity>) {
        list.clear()
        list.addAll(data)
    }

    interface OnSelectListener {
        fun onSelect(position: Int, item: ScaleEntity)
    }

    fun setOnSelectListener(listener: OnSelectListener) {
        this.onSelectListener = listener
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_video_scale_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvText.text = list[position].title

        holder.itemView.isSelected = scaleType == list[position].scale
        holder.itemView.setOnClickListener {
            onSelectListener?.onSelect(position, list[position])
        }
    }
}