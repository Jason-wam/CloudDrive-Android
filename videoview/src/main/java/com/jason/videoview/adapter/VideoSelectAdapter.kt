package com.jason.videoview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jason.videoview.R
import com.jason.videoview.model.VideoData

class VideoSelectAdapter : RecyclerView.Adapter<VideoSelectAdapter.ViewHolder>() {
    private val list = ArrayList<VideoData>()
    private var onSelectListener: OnSelectListener? = null
    var selectedPosition = 0

    fun getData() = list

    fun addData(item: VideoData) = list.add(item)

    fun setData(data: List<VideoData>) {
        list.clear()
        list.addAll(data)
    }

    interface OnSelectListener {
        fun onSelect(position: Int)
    }

    fun setOnSelectListener(listener: OnSelectListener) {
        this.onSelectListener = listener
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_video_select_view, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text = list[position].name
        holder.itemView.isSelected = selectedPosition == position
        holder.itemView.setOnClickListener {
            onSelectListener?.onSelect(position)
        }
    }
}