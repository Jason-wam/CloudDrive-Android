package com.jason.cloud.drive.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter


class BasePagerAdapter(fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val fragmentIds = ArrayList<Long>()
    private val fragmentList = ArrayList<Fragment>()
    private val fragmentTitleList = ArrayList<String?>()

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentTitleList[position]
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getCount(): Int {
        return fragmentList.size
    }

    fun addFragment(fragment: Fragment) {
        fragmentIds.add(fragmentList.size.toLong())
        fragmentList.add(fragment)
        fragmentTitleList.add(fragmentList.size.toString())
    }

    fun addFragment(title: String, fragment: Fragment) {
        fragmentIds.add(fragmentList.size.toLong())
        fragmentList.add(fragment)
        fragmentTitleList.add(title)
    }

    fun addFragment(id: Long, title: String, fragment: Fragment) {
        fragmentIds.add(id)
        fragmentList.add(fragment)
        fragmentTitleList.add(title)
    }

    fun clear() { //必须同时清空FragmentManger中的缓存才能彻底重置
        fragmentIds.clear()
        fragmentList.clear()
        fragmentTitleList.clear()
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun getItemId(position: Int): Long {
        return fragmentIds[position]
    }
}