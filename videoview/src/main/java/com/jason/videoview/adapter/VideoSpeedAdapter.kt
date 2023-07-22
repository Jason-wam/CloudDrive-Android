package com.jason.videoview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jason.videoview.R
import com.jason.videoview.model.SpeedEntity

class VideoSpeedAdapter(var speed: Float) : RecyclerView.Adapter<VideoSpeedAdapter.ViewHolder>() {
    private val list = ArrayList<SpeedEntity>()
    private var onSelectListener: OnSelectListener? = null

    fun getData() = list

    fun addData(item: SpeedEntity) = list.add(item)

    fun setData(data: List<SpeedEntity>) {
        list.clear()
        list.addAll(data)
    }

    interface OnSelectListener {
        fun onSelect(position: Int, item: SpeedEntity)
    }

    fun setOnSelectListener(listener: OnSelectListener) {
        this.onSelectListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_video_speed_view, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvText.text = list[position].title
        holder.itemView.isSelected = speed == list[position].speed
        holder.itemView.setOnClickListener {
            speed = list[position].speed
            onSelectListener?.onSelect(position, list[position])
        }
    }
}