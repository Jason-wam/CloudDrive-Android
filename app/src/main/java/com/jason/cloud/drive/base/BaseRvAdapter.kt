package com.jason.cloud.drive.base

import android.content.Context
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRvAdapter<ITEM, VH : RecyclerView.ViewHolder> :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    protected var onClickObservers: ArrayList<((position: Int, item: ITEM, viewHolder: VH) -> Unit)> =
        arrayListOf()
    protected var onBindViewObservers: ArrayList<((position: Int, item: ITEM, viewHolder: VH) -> Unit)> =
        arrayListOf()
    protected var onLongClickObservers: ArrayList<((position: Int, item: ITEM, viewHolder: VH) -> Unit)> =
        arrayListOf()
    open var itemData = ArrayList<ITEM>()

    override fun getItemCount(): Int {
        return itemData.size
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder.itemView.context, holder as VH, position, getData(position))
        if (onBindViewObservers.isNotEmpty()) {
            onBindViewObservers.forEach {
                it.invoke(position, getData(position), holder)
            }
        }

        if (onClickObservers.isNotEmpty()) {
            holder.itemView.setOnClickListener {
                onClickObservers.forEach {
                    it.invoke(position, getData(position), holder)
                }
            }
        }

        if (onLongClickObservers.isNotEmpty()) {
            holder.itemView.setOnLongClickListener {
                onLongClickObservers.forEach {
                    it.invoke(position, getData(position), holder)
                }
                true
            }
        }
    }

    abstract fun onBindViewHolder(context: Context, holder: VH, position: Int, item: ITEM)

    @Suppress("UNCHECKED_CAST")
    open fun getViewHolder(recyclerView: RecyclerView, position: Int): VH? {
        return recyclerView.findViewHolderForAdapterPosition(position)?.let {
            it as VH
        }
    }

    open fun clear() {
        itemData.clear()
        isReversed = false
    }

    open fun setData(data: List<ITEM>) {
        itemData.clear()
        itemData.addAll(data)
        isReversed = false
    }

    open fun setData(data: ArrayList<ITEM>) {
        itemData.clear()
        itemData.addAll(data)
        isReversed = false
    }

    open fun getData(index: Int): ITEM {
        return itemData[index]
    }

    open fun getData(): ArrayList<ITEM> {
        return itemData
    }

    open fun addData(index: Int, item: ITEM) {
        itemData.add(index, item)
    }

    open fun addData(item: ITEM) {
        itemData.add(item)
    }

    open fun addData(data: List<ITEM>) {
        this.itemData.addAll(data)
    }

    open fun addData(data: ArrayList<ITEM>) {
        this.itemData.addAll(data)
    }

    open fun addData(index: Int, data: List<ITEM>) {
        itemData.addAll(index, data)
    }

    open fun addData(index: Int, data: ArrayList<ITEM>) {
        itemData.addAll(index, data)
    }

    open fun removeData(dataPosition: Int) {
        itemData.removeAt(dataPosition)
    }

    open fun removeData(item: ITEM): Int {
        val dataPosition = itemData.indexOf(item)
        if (dataPosition != -1) {
            itemData.removeAt(dataPosition)
        }
        return dataPosition
    }

    var isReversed = false

    open fun reverse() {
        itemData.reverse()
        isReversed = !isReversed
    }

    open fun clearOnClickObserver() {
        this.onClickObservers.clear()
    }

    open fun setOnClickObserver(observer: ((position: Int, item: ITEM, viewHolder: VH) -> Unit)) {
        this.onClickObservers.clear()
        this.onClickObservers.add(observer)
    }

    open fun addOnClickObserver(observer: ((position: Int, item: ITEM, viewHolder: VH) -> Unit)) {
        this.onClickObservers.add(observer)
    }

    open fun removeOnClickObserver(observer: ((position: Int, item: ITEM, viewHolder: VH) -> Unit)) {
        this.onClickObservers.remove(observer)
    }

    open fun clearOnLongClickObserver() {
        this.onLongClickObservers.clear()
    }

    open fun removeOnLongClickObserver(observer: ((position: Int, item: ITEM, viewHolder: VH) -> Unit)) {
        this.onLongClickObservers.remove(observer)
    }

    open fun addOnLongClickObserver(observer: ((position: Int, item: ITEM, viewHolder: VH) -> Unit)) {
        this.onLongClickObservers.add(observer)
    }

    open fun addOnBindViewObserver(observer: ((position: Int, item: ITEM, viewHolder: VH) -> Unit)) {
        this.onBindViewObservers.add(observer)
    }

    open fun removeOnBindViewObserver(observer: ((position: Int, item: ITEM, viewHolder: VH) -> Unit)) {
        this.onBindViewObservers.remove(observer)
    }

    open fun clearOnBindViewObserver() {
        this.onBindViewObservers.clear()
    }
}