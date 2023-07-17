package com.jason.cloud.drive.views.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.databinding.FragmentHomeBinding
import com.jason.cloud.drive.utils.uploader.Uploader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : BaseBindFragment<FragmentHomeBinding>(R.layout.fragment_home) {
    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    override fun initView(context: Context) {

    }
}