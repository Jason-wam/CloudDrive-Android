package com.jason.cloud.drive.views.dialog

import android.content.Context
import android.view.View
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindDialog
import com.jason.cloud.drive.databinding.LayoutLoadingDialogBinding

/**
 * @Author: 进阶的面条
 * @Date: 2021-08-12 9:50
 * @Description: TODO
 */
class LoadDialog(context: Context) :
    BaseBindDialog<LayoutLoadingDialogBinding>(context, R.layout.layout_loading_dialog) {
    fun setMessage(message: CharSequence): LoadDialog {
        binding.tvMessage.text = message
        binding.tvMessage.visibility = View.VISIBLE
        return this
    }
}