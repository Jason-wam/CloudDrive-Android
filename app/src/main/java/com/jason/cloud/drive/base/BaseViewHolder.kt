package com.jason.cloud.drive.base

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

class BaseViewHolder(context: Context, @LayoutRes val resId: Int) : RecyclerView.ViewHolder(
    LayoutInflater.from(context).inflate(
        resId, null, false
    )
)