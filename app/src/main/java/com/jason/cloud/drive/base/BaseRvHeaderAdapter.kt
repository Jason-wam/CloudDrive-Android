package com.jason.cloud.drive.base

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

abstract class BaseRvHeaderAdapter<ITEM, VH : RecyclerView.ViewHolder> : BaseRvAdapter<ITEM, VH>() {
    private var headerView: View? = null

    companion object {
        const val ITEM_HEADER = 0
        const val ITEM_NORMAL = 1
    }

    open fun setHeaderView(view: View?) {
        if (view != null) {
            this.headerView = view
            notifyItemInserted(0)
        } else {
            this.headerView = null
            notifyItemRemoved(0)
        }
    }

    open fun removeHeader() {
        this.headerView = null
        notifyItemRemoved(0)
    }

    open fun updateHeader(block: (view: View) -> Unit) {
        headerView?.let {
            block.invoke(it)
        }
    }

    override fun getItemCount(): Int {
        return if (headerView != null) itemData.size + 1 else itemData.size
    }

    override fun getItemViewType(position: Int): Int {
        if (headerView == null) {
            return ITEM_NORMAL
        }
        if (position == 0) {
            return ITEM_HEADER
        }
        return ITEM_NORMAL
    }

    private class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == ITEM_HEADER) {
            return
        } else {
            val pos = getRealPosition(holder)
            onBindViewHolder(holder.itemView.context, holder as VH, pos, getData(pos))
            if (onBindViewObservers.isNotEmpty()) {
                onBindViewObservers.forEach {
                    it.invoke(pos, getData(pos), holder)
                }
            }

            if (onClickObservers.isNotEmpty()) {
                holder.itemView.setOnClickListener {
                    onClickObservers.forEach {
                        it.invoke(pos, getData(pos), holder)
                    }
                }
            }

            if (onLongClickObservers.isNotEmpty()) {
                holder.itemView.setOnLongClickListener {
                    onLongClickObservers.forEach {
                        it.invoke(pos, getData(pos), holder)
                    }
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_HEADER) {
            HeaderHolder(headerView!!)
        } else {
            onCreateNormalViewHolder(parent, viewType)
        }
    }

    abstract fun onCreateNormalViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val lp = holder.itemView.layoutParams
        if (lp != null && lp is StaggeredGridLayoutManager.LayoutParams) {
            lp.isFullSpan = holder.layoutPosition == 0
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val manager = recyclerView.layoutManager
        if (manager is GridLayoutManager) {
            manager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (getItemViewType(position) == ITEM_HEADER) manager.spanCount else 1
                }
            }
        }
    }

    open fun getRealPosition(holder: RecyclerView.ViewHolder): Int {
        return if (headerView == null) holder.layoutPosition else holder.layoutPosition - 1
    }

}