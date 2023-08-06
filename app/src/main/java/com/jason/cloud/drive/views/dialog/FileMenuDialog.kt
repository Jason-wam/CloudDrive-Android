package com.jason.cloud.drive.views.dialog

import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutFileMenuDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.extension.getSerializableListExtraEx
import java.io.Serializable

class FileMenuDialog(val parent: FragmentActivity) :
    BaseBindBottomSheetDialogFragment<LayoutFileMenuDialogBinding>(R.layout.layout_file_menu_dialog) {

    private var onFileDeleteListener: (() -> Unit)? = null
    private var onFileRenamedListener: (() -> Unit)? = null

    fun setOnFileDeleteListener(listener: () -> Unit): FileMenuDialog {
        this.onFileDeleteListener = listener
        return this
    }

    fun setOnFileRenamedListener(listener: () -> Unit): FileMenuDialog {
        this.onFileRenamedListener = listener
        return this
    }

    fun setFile(list: List<FileEntity>, position: Int): FileMenuDialog {
        arguments?.putSerializable("list", list as Serializable)
        arguments?.putInt("position", position)
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

        val position = arguments?.getInt("position") ?: 0
        val list = arguments?.getSerializableListExtraEx<FileEntity>("list") ?: return
        val file = list[position]
        binding.tvTitle.text = file.name

        binding.btnOpen.setOnClickListener {
            if (FileType.isVideo(file.name)) {
                parent.viewVideos(list, position)
                dismiss()
            } else if (FileType.isImage(file.name)) {
                parent.viewImages(list, position)
                dismiss()
            } else if (FileType.isAudio(file.name)) {
                parent.viewAudios(list, position)
                dismiss()
            } else {
                parent.viewOthers(list, position)
                dismiss()
            }
        }

        binding.btnOtherApp.setOnClickListener {
            parent.openWithOtherApplication(list, position)
            dismiss()
        }

        binding.btnCast.isVisible = FileType.isMedia(file.name)
        binding.btnCast.setOnClickListener {
            parent.castMedia(list, position)
            dismiss()
        }

        binding.btnDownload.setOnClickListener {
            parent.downloadFile(file)
            dismiss()
        }

        binding.btnDetail.setOnClickListener {
            if (FileType.isVideo(file.name)) {
                parent.viewVideoDetail(list, position)
                dismiss()
            } else if (FileType.isAudio(file.name)) {
                parent.viewAudioDetail(list, position)
                dismiss()
            } else {
                parent.viewOtherDetail(list[position])
                dismiss()
            }
        }

        binding.btnRename.setOnClickListener {
            parent.showRenameDialog(file) { onFileRenamedListener?.invoke() }
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            parent.showDeleteDialog(file) { onFileDeleteListener?.invoke() }
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}
