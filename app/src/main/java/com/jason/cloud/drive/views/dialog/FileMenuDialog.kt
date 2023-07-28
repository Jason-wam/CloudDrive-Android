package com.jason.cloud.drive.views.dialog

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutFileMenuDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.extension.getSerializableListExtraEx
import java.io.Serializable

class FileMenuDialog :
    BaseBindBottomSheetDialogFragment<LayoutFileMenuDialogBinding>(R.layout.layout_file_menu_dialog) {
    private var callback: Callback? = null

    fun setFile(list: List<FileEntity>, position: Int): FileMenuDialog {
        arguments?.putSerializable("list", list as Serializable)
        arguments?.putInt("position", position)
        return this
    }

    fun setCallback(callback: Callback): FileMenuDialog {
        this.callback = callback
        return this
    }

    interface Callback {
        fun viewVideos(list: List<FileEntity>, position: Int)

        fun viewAudios(list: List<FileEntity>, position: Int)

        fun viewImages(list: List<FileEntity>, position: Int)

        fun viewOthers(list: List<FileEntity>, position: Int)

        fun openWithOtherApplication(list: List<FileEntity>, position: Int)

        fun viewVideoDetail(list: List<FileEntity>, position: Int)

        fun viewAudioDetail(list: List<FileEntity>, position: Int)

        fun downloadIt(file: FileEntity)

        fun deleteIt(file: FileEntity)
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


        val position = arguments?.getInt("position") ?: 0
        val list = arguments?.getSerializableListExtraEx<FileEntity>("list") ?: emptyList()
        if (list.isNotEmpty()) {
            val current = list[position]
            binding.tvTitle.text = current.name

            binding.btnOpen.setOnClickListener {
                if (FileType.isVideo(current.name)) {
                    callback?.viewVideos(list, position)
                    dismiss()
                } else if (FileType.isImage(current.name)) {
                    callback?.viewImages(list, position)
                    dismiss()
                } else if (FileType.isAudio(current.name)) {
                    callback?.viewAudios(list, position)
                    dismiss()
                } else {
                    callback?.viewOthers(list, position)
                    dismiss()
                }
            }

            binding.btnOtherApp.setOnClickListener {
                callback?.openWithOtherApplication(list, position)
                dismiss()
            }

            binding.btnDownload.setOnClickListener {
                callback?.downloadIt(current)
                dismiss()
            }

            binding.btnDetail.setOnClickListener {
                if (FileType.isVideo(current.name)) {
                    callback?.viewVideoDetail(list, position)
                    dismiss()
                } else if (FileType.isAudio(current.name)) {
                    callback?.viewAudioDetail(list, position)
                    dismiss()
                } else {

                }
            }

            binding.btnDelete.setOnClickListener {
                callback?.deleteIt(current)
                dismiss()
            }

            binding.btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }
}