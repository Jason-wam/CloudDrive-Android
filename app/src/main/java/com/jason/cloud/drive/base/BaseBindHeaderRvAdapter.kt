package com.jason.cloud.drive.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

/**
 * @Author: 进阶的面条
 * @Date: 2021-12-14 18:31
 * @Description: TODO
 */
abstract class BaseBindHeaderRvAdapter<ITEM, BINDING : ViewDataBinding>(
    private val itemLayoutId: Int,
    private val attachToParent: Boolean = false
) : BaseRvHeaderAdapter<ITEM, BaseBindHeaderRvAdapter.ViewHolder<BINDING>>() {
    open class ViewHolder<BINDING : ViewDataBinding>(val binding: BINDING) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateNormalViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            DataBindingUtil.inflate<BINDING>(inflater, itemLayoutId, parent, attachToParent)
        return ViewHolder(binding)
    }
}