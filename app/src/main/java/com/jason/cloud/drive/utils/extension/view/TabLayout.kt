package com.jason.videocat.utils.extension.view

import com.google.android.material.tabs.TabLayout

fun TabLayout.addTab(title: String, selected: Boolean = false):TabLayout.Tab {
    val tab = newTab().setText(title)
    addTab(tab, selected)
    return tab
}

inline fun TabLayout.onTabSelected(crossinline block: (tab: TabLayout.Tab) -> Unit) {
    this.clearOnTabSelectedListeners()
    this.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            block(tab)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {

        }

        override fun onTabReselected(tab: TabLayout.Tab) {

        }
    })
}