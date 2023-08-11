package com.jason.cloud.drive.views.dialog

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.flyjingfish.openimagelib.OpenImage
import com.flyjingfish.openimagelib.enums.MediaType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.databinding.LayoutDownloadDoneMenuDialogBinding
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.actions.showDeleteDownloadDoneTask
import com.jason.cloud.drive.utils.actions.viewOtherDetail
import com.jason.cloud.drive.utils.actions.viewVideoFiles
import com.jason.cloud.extension.getSerializableEx
import com.jason.cloud.extension.openFile
import com.jason.cloud.extension.sendFile
import java.io.File

class DownloadDoneTaskMenuDialog(val parent: FragmentActivity) :
    BaseBindBottomSheetDialogFragment<LayoutDownloadDoneMenuDialogBinding>(R.layout.layout_download_done_menu_dialog) {

    private var onFileDeleteListener: (() -> Unit)? = null
    private var onFileRenamedListener: (() -> Unit)? = null

    fun setOnFileDeleteListener(listener: () -> Unit): DownloadDoneTaskMenuDialog {
        this.onFileDeleteListener = listener
        return this
    }

    fun setOnFileRenamedListener(listener: () -> Unit): DownloadDoneTaskMenuDialog {
        this.onFileRenamedListener = listener
        return this
    }

    fun setFile(task: DownloadTaskEntity): DownloadDoneTaskMenuDialog {
        arguments?.putSerializable("task", task)
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

        val task = arguments?.getSerializableEx("task", DownloadTaskEntity::class.java) ?: return
        val file = File(task.dir, task.name)
        binding.tvTitle.text = task.name
        binding.btnOpen.setOnClickListener {
            if (FileType.isVideo(task.name)) {
                parent.viewVideoFiles(listOf(file), 0)
                dismiss()
            } else if (FileType.isImage(task.name)) {
                OpenImage.with(this).setNoneClickView()
                    .setImageUrl(file.absolutePath, MediaType.IMAGE).show()
                dismiss()
            } else if (FileType.isAudio(task.name)) {
                AudioPlayDialog().setFiles(listOf(file), 0)
                    .show(parent.supportFragmentManager, "audio")
                dismiss()
            } else {
                parent.openFile(file)
                dismiss()
            }
        }

        binding.btnOtherApp.setOnClickListener {
            parent.openFile(file)
            dismiss()
        }

        binding.btnShare.setOnClickListener {
            parent.sendFile(file)
            dismiss()
        }

        binding.btnDetail.setOnClickListener {
            parent.viewOtherDetail(file)
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            parent.showDeleteDownloadDoneTask(task)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}