package com.jason.cloud.drive.views.dialog

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutFileMenuDialogBinding
import com.jason.cloud.drive.utils.extension.getSerializableEx
import com.jason.cloud.drive.model.FileEntity

class FileMenuDialog :
    BaseBindBottomSheetDialogFragment<LayoutFileMenuDialogBinding>(R.layout.layout_file_menu_dialog) {

    override fun initView(view: View) {
        super.initView(view)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.ivHandler.isSelected = slideOffset != 0f
            }
        })

        arguments?.getSerializableEx("file", FileEntity::class.java)?.let {
            binding.tvTitle.text = it.name

            binding.btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    fun setFile(file: FileEntity): FileMenuDialog {
        arguments?.putSerializable("file", file)
        return this
    }
}