package com.jason.videocat.utils.extension.view

import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

fun ViewPager2.bindBottomNavigationView(navigationView: BottomNavigationView) {
    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            navigationView.menu.getItem(position).isChecked = true
        }
    })
}
