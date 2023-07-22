package com.jason.cloud.drive.base

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

@SuppressLint("ResourceType")
abstract class BaseBindDialog<VB : ViewDataBinding>(context: Context, @LayoutRes layoutId: Int) :
    BaseDialog(context) {
    protected lateinit var binding: VB

    init {
        setContentView(layoutId)
    }

    final override fun setContentView(layoutResID: Int) {
        val view = View.inflate(context, layoutResID, null)
        super.setContentView(view)
        binding = DataBindingUtil.bind(view)!!
        addOnShowListener { initView(binding) }
    }

    protected open fun initView(binding: VB) {
    }
}