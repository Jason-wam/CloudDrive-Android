package com.jason.cloud.drive.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class BaseBindBottomSheetDialogFragment<VB : ViewDataBinding>(@LayoutRes override val layoutId: Int) :
    BaseBottomSheetDialogFragment(layoutId) {
    protected lateinit var binding: VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun initView(view: View) {
        binding = DataBindingUtil.bind(view)!!
    }
}