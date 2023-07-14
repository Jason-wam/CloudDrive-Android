package com.jason.cloud.drive.views.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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