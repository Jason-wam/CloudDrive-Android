package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindDialog
import com.jason.cloud.drive.databinding.LayoutProgressDialogBinding
import com.jason.cloud.drive.extension.toFileSizeString

class ProgressDialog(context: Context) :
    BaseBindDialog<LayoutProgressDialogBinding>(context, R.layout.layout_progress_dialog) {
    override fun initView(binding: LayoutProgressDialogBinding) {
        super.initView(binding)
        setCanceledOnTouchOutside(false)
    }

    fun update(value: Int): ProgressDialog {
        binding.indicator.setProgressCompat(value, true)
        return this
    }

    @SuppressLint("SetTextI18n")
    fun updateSpeed(value: Long): ProgressDialog {
        binding.tvSpeed.isVisible = true
        binding.tvSpeed.text = value.toFileSizeString() + "/s"
        return this
    }

    fun setIsIndeterminate(isIndeterminate: Boolean): ProgressDialog {
        binding.indicator.isIndeterminate = isIndeterminate
        return this
    }

    fun setMessage(message: CharSequence): ProgressDialog {
        binding.tvMessage.text = message
        binding.tvMessage.visibility = View.VISIBLE
        return this
    }
}