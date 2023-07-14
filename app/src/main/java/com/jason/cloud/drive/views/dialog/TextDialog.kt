package com.jason.cloud.drive.views.dialog

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindDialog
import com.jason.cloud.drive.databinding.LayoutTextDialogBinding

class TextDialog(context: Context) :
    BaseBindDialog<LayoutTextDialogBinding>(context, R.layout.layout_text_dialog) {
    init {
        setCanceledOnTouchOutside(false)
    }

    fun setTitle(title: CharSequence): TextDialog {
        binding.tvTitle.text = title
        return this
    }

    fun setText(text: CharSequence): TextDialog {
        binding.tvContent.text = text
        return this
    }

    fun setText(@StringRes resId: Int): TextDialog {
        binding.tvContent.setText(resId)
        return this
    }

    fun onPositive(text: CharSequence? = null, block: () -> Unit): TextDialog {
        if (text != null) {
            binding.btnPositive.text = text
        }
        binding.btnPositive.setOnClickListener {
            block.invoke()
            dismiss()
        }
        return this
    }

    fun onNegative(text: CharSequence? = null, block: () -> Unit): TextDialog {
        if (text != null) {
            binding.btnNegative.text = text
        }
        binding.btnNegative.isVisible = true
        binding.btnNegative.setOnClickListener {
            block.invoke()
            dismiss()
        }
        return this
    }

    fun onNeutral(text: CharSequence? = null, block: () -> Unit): TextDialog {
        if (text != null) {
            binding.btnNeutral.text = text
        }
        binding.btnNeutral.isVisible = true
        binding.btnNeutral.setOnClickListener {
            block.invoke()
            dismiss()
        }
        return this
    }
}