package com.jason.cloud.drive.utils.extension.view

import androidx.recyclerview.widget.RecyclerView

/**
 * 判断手指能否向下滑动
 * 如果Item为空，返回false
 */
fun RecyclerView.addCanScrollDownObserver(block: (canScrollDown: Boolean) -> Unit) {
    fun notify() {
        if (adapter?.itemCount == 0) {
            block.invoke(false)
        } else {
            //负值检查向上滚动，正向检查向下滚动
            block.invoke(canScrollVertically(-1))
        }
    }
    
    adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            notify()
        }
        
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            notify()
        }
        
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            notify()
        }
        
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            notify()
        }
        
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            notify()
        }
        
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            notify()
        }
    })
    
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            notify()
        }
    })
}

/**
 * 判断手指能否向上滑动
 * 如果Item为空，返回true（显示阴影）
 */
fun RecyclerView.addCanScrollUpObserver(block: (canScrollDown: Boolean) -> Unit) {
    fun notify() {
        if (adapter?.itemCount == 0) {
            block.invoke(true)
        } else {
            //负值检查向上滚动，正向检查向下滚动
            block.invoke(canScrollVertically(1))
        }
    }

    adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            notify()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            notify()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            notify()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            notify()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            notify()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            notify()
        }
    })

    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            notify()
        }
    })
}