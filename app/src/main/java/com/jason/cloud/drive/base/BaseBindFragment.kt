package com.jason.cloud.drive.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseBindFragment<VB : ViewDataBinding>(@LayoutRes val contentLayoutId: Int = 0) : Fragment() {
    protected lateinit var binding: VB
    protected lateinit var viewContext: Context
    
    protected var lazyLoad = true
    private var hasLoaded = false
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, contentLayoutId, container, false)
        viewContext = binding.root.context
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!lazyLoad) {
            hasLoaded = true
            initView(viewContext)
        }
    }

    override fun onResume() {
        super.onResume()
        if (lazyLoad && !hasLoaded) {
            initView(viewContext)
            hasLoaded = true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        hasLoaded = false
    }
    
    abstract fun initView(context: Context)
}