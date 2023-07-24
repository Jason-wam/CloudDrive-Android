package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutVideoDetailDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.UrlBuilder
import com.jason.cloud.drive.views.activity.VideoPreviewActivity
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.toDateMinuteString
import com.jason.cloud.extension.toFileSizeString
import com.jason.videoview.model.VideoData
import java.io.Serializable

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

        val position = arguments?.getInt("position") ?: 0
        val fileList = arguments?.getSerializableListExtraEx<FileEntity>("list").orEmpty()
        if (position in fileList.indices) {
            val file = fileList[position]
            binding.tvName.text = file.name
            binding.tvURL.text = file.path
            binding.tvInfo.text = file.size.toFileSizeString() + " / " +
                    file.date.toDateMinuteString()

            binding.cardImageView.post {
                val width = binding.cardImageView.width
                val imageURL = UrlBuilder(file.gifURL).param("size", width).build()
                binding.cardImageView.minimumHeight = (width * (1080 / 1920f)).toInt()
                binding.ivImage.loadIMG(imageURL) {
                    timeout(60000)
                    addListener { _, _ ->
                        binding.progressBar.isVisible = false
                    }
                }
            }

            binding.btnPlay.setOnClickListener {
                VideoPreviewActivity.open(requireContext(), position, fileList.map {
                    VideoData(it.hash, it.name, it.rawURL)
                })
                dismiss()
            }
        }


        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setFileList(list: List<FileEntity>, position: Int): VideoDetailDialog {
        arguments?.putSerializable("list", list as Serializable)
        arguments?.putInt("position", position)
        return this
    }
}