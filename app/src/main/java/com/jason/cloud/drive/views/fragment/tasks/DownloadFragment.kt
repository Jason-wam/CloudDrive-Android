package com.jason.cloud.drive.views.fragment.tasks

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.databinding.FragmentDownloadBinding

class DownloadFragment : BaseBindFragment<FragmentDownloadBinding>(R.layout.fragment_download) {
    companion object {
        @JvmStatic
        fun newInstance() = DownloadFragment()
    }

    override fun initView(context: Context) {
        binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
    }
}