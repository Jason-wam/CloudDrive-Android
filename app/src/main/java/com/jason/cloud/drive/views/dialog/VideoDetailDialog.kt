package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutVideoDetailDialogBinding
import com.jason.cloud.drive.utils.extension.getSerializableEx
import com.jason.cloud.drive.utils.extension.glide.loadIMG
import com.jason.cloud.drive.utils.extension.openURL
import com.jason.cloud.drive.utils.extension.toDateMinuteString
import com.jason.cloud.drive.utils.extension.toFileSizeString
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.UrlBuilder

class VideoDetailDialog :
    BaseBindBottomSheetDialogFragment<LayoutVideoDetailDialogBinding>(R.layout.layout_video_detail_dialog) {
    @SuppressLint("SetTextI18n")
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
            binding.tvName.text = file.name
            binding.tvURL.text = file.path
            binding.tvInfo.text =
                file.size.toFileSizeString() + " / " + file.date.toDateMinuteString()

            binding.cardImageView.post {
                val width = binding.cardImageView.width
                binding.cardImageView.minimumHeight = (width * (1080 / 1920f)).toInt()

                binding.ivImage.loadIMG(UrlBuilder(file.gifURL).param("size", width).build()) {
                    timeout(60000)
                    addListener { _, _ ->
                        binding.progressBar.isVisible = false
                    }
                }
            }

            binding.btnOpen.setOnClickListener {
                context?.openURL(file.rawURL, "video/*")
            }
            binding.btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    fun setFile(file: FileEntity): VideoDetailDialog {
        arguments?.putSerializable("file", file)
        return this
    }
}