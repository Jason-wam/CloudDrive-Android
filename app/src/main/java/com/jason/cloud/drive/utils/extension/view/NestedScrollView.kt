package com.jason.cloud.drive.utils.extension.view

import androidx.core.widget.NestedScrollView
import androidx.core.widget.NestedScrollView.OnScrollChangeListener

fun NestedScrollView.addCanScrollDownObserver(block: (canScrollDown: Boolean) -> Unit) {
    setOnScrollChangeListener(OnScrollChangeListener { _, _, _, _, _ ->
        block.invoke(canScrollVertically(-1))
    })
}