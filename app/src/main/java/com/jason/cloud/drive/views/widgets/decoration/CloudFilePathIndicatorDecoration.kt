package com.jason.cloud.drive.views.widgets.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jason.cloud.drive.utils.extension.dp

class CloudFilePathIndicatorDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        parent.adapter?.let {
            when (parent.getChildLayoutPosition(view)) {
                0 -> outRect.set(8.dp, 0, 0, 0)
                it.itemCount - 1 -> outRect.set(0, 0,  4.dp, 0)
                else -> outRect.set(0, 0, 0, 0)
            }
        }
    }
}