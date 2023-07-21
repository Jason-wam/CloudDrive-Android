package com.jason.cloud.drive.views.dialog

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.database.downloader.DownloadQueue
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.databinding.LayoutFileMenuDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.model.mimeType
import com.jason.cloud.drive.utils.extension.externalFilesDir
import com.jason.cloud.drive.utils.extension.getSerializableEx
import com.jason.cloud.drive.utils.extension.openURL
import com.jason.cloud.drive.utils.extension.toast

class FileMenuDialog :
    BaseBindBottomSheetDialogFragment<LayoutFileMenuDialogBinding>(R.layout.layout_file_menu_dialog) {

    fun setFile(file: FileEntity): FileMenuDialog {
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
                binding.ivHandler.isSelected = slideOffset != 0f
            }
        })

        arguments?.getSerializableEx("file", FileEntity::class.java)?.let { file ->
            binding.tvTitle.text = file.name
            binding.btnOpen.setOnClickListener {
                context?.openURL(file.rawURL, file.mimeType())
            }
            binding.btnFetch.setOnClickListener {
                download(file)
            }
            binding.btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun download(file: FileEntity) {
        val dir = requireContext().externalFilesDir("downloads")
        DownloadQueue.instance.addTask(DownloadTask(file, dir))
        DownloadQueue.instance.start()
        toast("正在取回文件：${file.name}")
        dismiss()
    }
}