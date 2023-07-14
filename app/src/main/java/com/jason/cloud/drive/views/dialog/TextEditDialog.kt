package com.jason.cloud.drive.views.dialog

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindDialog
import com.jason.cloud.drive.databinding.LayoutTextEditDialogBinding

class TextEditDialog(context: Context) :
    BaseBindDialog<LayoutTextEditDialogBinding>(context, R.layout.layout_text_edit_dialog) {
    init {
        setCanceledOnTouchOutside(false)
    }

    fun setTitle(title: CharSequence): TextEditDialog {
        binding.tvTitle.text = title
        return this
    }

    fun setHintText(hint: CharSequence): TextEditDialog {
        binding.tvContent.hint = hint
        return this
    }

    fun setHintText(@StringRes resId: Int): TextEditDialog {
        binding.tvContent.setHint(resId)
        return this
    }

    fun setText(text: CharSequence): TextEditDialog {
        binding.tvContent.setText(text)
        return this
    }

    fun setText(@StringRes resId: Int): TextEditDialog {
        binding.tvContent.setText(resId)
        return this
    }

    fun onPositive(text: CharSequence? = null, block: (text: String?) -> Boolean): TextEditDialog {
        if (text != null) {
            binding.btnPositive.text = text
        }

        binding.btnPositive.setOnClickListener {
            if (block.invoke((binding.tvContent.text ?: "").toString())) {
                dismiss()
            }
        }
        return this
    }

    fun onNegative(text: CharSequence? = null, block: (() -> Boolean)? = null): TextEditDialog {
        if (text != null) {
            binding.btnNegative.text = text
        }
        binding.btnNegative.isVisible = true
        binding.btnNegative.setOnClickListener {
            if (block?.invoke() != false) {
                dismiss()
            }
        }
        return this
    }

    fun onNeutral(text: CharSequence? = null, block: (() -> Boolean)? = null): TextEditDialog {
        if (text != null) {
            binding.btnNeutral.text = text
        }
        binding.btnNeutral.isVisible = true
        binding.btnNeutral.setOnClickListener {
            if (block?.invoke() != false) {
                dismiss()
            }
        }
        return this
    }
}