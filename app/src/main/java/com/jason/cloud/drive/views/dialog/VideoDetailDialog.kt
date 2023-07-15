package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutVideoDetailDialogBinding
import com.jason.cloud.drive.extension.getSerializableEx
import com.jason.cloud.drive.extension.glide.loadIMG
import com.jason.cloud.drive.extension.toDateMinuteString
import com.jason.cloud.drive.extension.toFileSizeString
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.Configure

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

        binding.cardImageView.post {
            val width = binding.cardImageView.width
            binding.cardImageView.minimumHeight =
                (width * (1080 / 1920f)).toInt()
        }

        arguments?.getSerializableEx("file", FileEntity::class.java)?.let {
            binding.tvName.text = it.name
            binding.tvURL.text = it.path
            binding.tvInfo.text = it.size.toFileSizeString() + " / " + it.date.toDateMinuteString()
            binding.ivImage.loadIMG("${Configure.hostURL}/thumbnail?hash=${it.hash}&isGif=true") {
                timeout(60000)
                addListener { _, _ ->
                    binding.progressBar.isVisible = false
                }
            }
            binding.btnCancel.setOnClickListener { dismiss() }
        }
    }

    fun setFile(file: FileEntity): VideoDetailDialog {
        arguments?.putSerializable("file", file)
        return this
    }
}