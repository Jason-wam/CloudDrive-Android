package com.jason.cloud.drive.views.dialog

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutFolderMenuDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.actions.showDeleteFolderDialog
import com.jason.cloud.drive.utils.actions.showRenameDialog
import com.jason.cloud.drive.utils.actions.viewOtherDetail
import com.jason.cloud.extension.getSerializableEx
import com.jason.cloud.extension.toast

class FolderMenuDialog(val parent: FragmentActivity) :
    BaseBindBottomSheetDialogFragment<LayoutFolderMenuDialogBinding>(R.layout.layout_folder_menu_dialog) {

    private var onFileDeleteListener: (() -> Unit)? = null
    private var onFileRenamedListener: (() -> Unit)? = null
    private var onRequestOpenFolderListener: (() -> Unit)? = null

    fun setOnFileDeleteListener(listener: () -> Unit): FolderMenuDialog {
        this.onFileDeleteListener = listener
        return this
    }

    fun setOnFileRenamedListener(listener: () -> Unit): FolderMenuDialog {
        this.onFileRenamedListener = listener
        return this
    }

    fun setRequestOpenFolderListener(listener: () -> Unit): FolderMenuDialog {
        this.onRequestOpenFolderListener = listener
        return this
    }

    fun setFile(file: FileEntity): FolderMenuDialog {
        arguments?.putSerializable("file", file)
        return this
    }

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

        val file = arguments?.getSerializableEx("file", FileEntity::class.java) ?: return

        binding.tvTitle.text = file.name
        binding.btnOpen.setOnClickListener {
            onRequestOpenFolderListener?.invoke()
            dismiss()
        }

        binding.btnDownload.setOnClickListener {
            toast("暂未实现")
            dismiss()
        }

        binding.btnDetail.setOnClickListener {
            parent.viewOtherDetail(file)
            dismiss()
        }

        binding.btnRename.setOnClickListener {
            parent.showRenameDialog(file) { onFileRenamedListener?.invoke() }
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            parent.showDeleteFolderDialog(file) { onFileDeleteListener?.invoke() }
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
