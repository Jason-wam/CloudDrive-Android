package com.jason.videocat.utils.extension.view

import android.widget.CheckBox

/**
 * 只回调用户切换状态
 */
fun CheckBox.onCheckStateChangeObserver(block: (checked: Boolean) -> Unit) {
    setOnCheckedChangeListener { buttonView, isChecked ->
        if (buttonView.isPressed) {
            block.invoke(isChecked)
        }
    }
}