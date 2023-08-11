package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutDetailOtherFileDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.extension.getSerializableEx
import com.jason.cloud.extension.toDateMinuteString
import com.jason.cloud.extension.toFileSizeString
import java.io.File

class DetailOtherDialog(val parent: FragmentActivity) :
    BaseBindBottomSheetDialogFragment<LayoutDetailOtherFileDialogBinding>(R.layout.layout_detail_other_file_dialog) {

    fun setFile(file: File): DetailOtherDialog {
        arguments?.putSerializable("file", file)
        return this
    }

    fun setFile(file: FileEntity): DetailOtherDialog {
        arguments?.putSerializable("fileEntity", file)
        return this
    }

    @SuppressLint("SetTextI18n")
    override fun initView(view: View) {
        super.initView(view)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.handler.isSelected = slideOffset != 0f
            }
        })

        arguments?.getSerializableEx("file", File::class.java)?.let {
            binding.tvName.text = it.name
            binding.tvURL.text = it.path
            binding.tvDate.text = it.lastModified().toDateMinuteString()
            binding.tvSize.text = it.length().toFileSizeString()
        }

        arguments?.getSerializableEx("fileEntity", FileEntity::class.java)?.let {
            binding.tvName.text = it.name
            binding.tvURL.text = it.path
            binding.tvDate.text = it.date.toDateMinuteString()
            binding.tvSize.text = it.size.toFileSizeString()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

}