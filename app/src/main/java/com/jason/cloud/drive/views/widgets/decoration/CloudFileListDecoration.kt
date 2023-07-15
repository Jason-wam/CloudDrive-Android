package com.jason.cloud.drive.views.widgets.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jason.cloud.drive.R
import com.jason.cloud.drive.extension.dp

class CloudFileListDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val dividerSize = context.resources.getDimension(R.dimen.itemDecorationSize).toInt()
    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorItemDecoration)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        parent.adapter?.let {
            when (parent.getChildLayoutPosition(view)) {
                0 -> {}
                it.itemCount - 1 -> {}
                else -> outRect.set(0, 0, 0, dividerSize)
            }
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)
        parent.adapter?.let {
            for (i in 0 until it.itemCount) {
                val child = parent.getChildAt(i)
                if (child != null) {
                    if (i > 0) {
                        val divider =
                            Rect(16.dp, child.top, parent.width - 16.dp, child.top - dividerSize)
                        c.drawRect(divider, paint)
                    }
                }
            }
        }
    }
}