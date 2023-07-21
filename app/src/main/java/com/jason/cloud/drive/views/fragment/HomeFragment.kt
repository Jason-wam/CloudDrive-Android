package com.jason.cloud.drive.views.fragment

import android.content.Context
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.databinding.FragmentHomeBinding

class HomeFragment : BaseBindFragment<FragmentHomeBinding>(R.layout.fragment_home) {
    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    override fun initView(context: Context) {

    }
}