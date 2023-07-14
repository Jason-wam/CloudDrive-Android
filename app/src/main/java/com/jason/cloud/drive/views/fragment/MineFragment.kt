package com.jason.cloud.drive.views.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.databinding.FragmentMineBinding

class MineFragment : BaseBindFragment<FragmentMineBinding>(R.layout.fragment_mine) {
    companion object {
        @JvmStatic
        fun newInstance() = MineFragment()
    }

    override fun initView(context: Context) {

    }
}