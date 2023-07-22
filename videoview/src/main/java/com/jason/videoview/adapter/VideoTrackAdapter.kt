package com.jason.videoview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jason.videoview.R
import xyz.doikki.videoplayer.model.Track

class VideoTrackAdapter(var trackIndex: Int) :
    RecyclerView.Adapter<VideoTrackAdapter.ViewHolder>() {
    private val list = ArrayList<Track>()
    private var onSelectListener: OnSelectListener? = null

    fun getData() = list

    fun addData(item: Track) = list.add(item)

    fun setData(data: List<Track>) {
        list.clear()
        list.addAll(data)
    }

    interface OnSelectListener {
        fun onSelect(position: Int, item: Track)
    }

    fun setOnSelectListener(listener: OnSelectListener) {
        this.onSelectListener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvText: TextView = itemView.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_video_track_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvText.text = list[position].name
        holder.itemView.isSelected = trackIndex == list[position].index
        holder.itemView.setOnClickListener {
            onSelectListener?.onSelect(position, list[position])
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}