package com.jason.cloud.drive.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

open class BaseViewPager2Adapter : FragmentStateAdapter {
    private val ids = ArrayList<Long>()
    private val fragmentList = ArrayList<Fragment>()
    private val fragmentTitleArray = ArrayList<String>()

    constructor(fragment: Fragment) : super(fragment)

    constructor(fragmentActivity: FragmentActivity) : super(fragmentActivity)

    constructor(fragmentManager: FragmentManager, lifecycle: Lifecycle) : super(
        fragmentManager,
        lifecycle
    )

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun getItemId(position: Int): Long {
        if (ids.isNotEmpty()) {
            return ids[position]
        }
        return super.getItemId(position)
    }

    override fun containsItem(itemId: Long): Boolean {
        if (ids.isNotEmpty()) {
            return ids.contains(itemId)
        }
        return super.containsItem(itemId)
    }


    fun getPageTitle(position: Int): CharSequence {
        if (position > fragmentTitleArray.size - 1) {
            return ""
        }
        return fragmentTitleArray[position]
    }

    fun getFragment(position: Int): Fragment? {
        if (position <= fragmentList.size - 1) {
            return fragmentList[position]
        }
        return null
    }

    fun addFragment(id: Long, title: String, fragment: Fragment) {
        ids.add(id)
        fragmentList.add(fragment)
        fragmentTitleArray.add(title)
    }

    fun addFragment(title: String, fragment: Fragment) {
        fragmentList.add(fragment)
        fragmentTitleArray.add(title)
    }

    fun getFragmentPosition(fragment: Fragment): Int {
        return fragmentList.indexOf(fragment)
    }

    fun clear() { //必须同时清空FragmentManger中的缓存才能彻底重置
        ids.clear()
        fragmentList.clear()
        fragmentTitleArray.clear()
    }
}