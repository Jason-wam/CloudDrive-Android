package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.drake.net.Get
import com.drake.net.utils.scopeDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutDetailVideoDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.PositionStore
import com.jason.cloud.drive.utils.UrlBuilder
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.extension.externalFilesDir
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.saveToGallery
import com.jason.cloud.extension.toDateMinuteString
import com.jason.cloud.extension.toFileSizeString
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.activity.VideoPlayActivity
import com.jason.cloud.media3.model.Media3Item
import java.io.File
import java.io.Serializable

class DetailVideoDialog(val parent: FragmentActivity) :
    BaseBindBottomSheetDialogFragment<LayoutDetailVideoDialogBinding>(R.layout.layout_detail_video_dialog) {
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

        val position = arguments?.getInt("position") ?: 0
        val fileList = arguments?.getSerializableListExtraEx<FileEntity>("list") ?: return
        val file = fileList[position]
        binding.tvName.text = file.name
        binding.tvURL.text = file.path
        binding.tvDate.text = file.date.toDateMinuteString()
        binding.tvSize.text = file.size.toFileSizeString()

        binding.cardImageView.post {
            val width = binding.cardImageView.width
            val imageURL = UrlBuilder(file.gifURL).param("size", width).build()
            binding.cardImageView.minimumHeight = (width * (1080 / 1920f)).toInt()
            binding.ivImage.loadIMG(imageURL) {
                timeout(60000)
                addListener { succeed, _ ->
                    binding.progressBar.isVisible = false
                    if (succeed.not()) {
                        binding.tvError.isVisible = true
                    } else {
                        binding.btnSaveGif.alpha = 1f
                        binding.btnSaveGif.setOnClickListener {
                            downloadGif(file)
                            dismiss()
                        }
                    }
                }
            }
        }

        binding.btnPlay.setOnClickListener {
            VideoPlayActivity.positionStore = PositionStore()
            VideoPlayActivity.open(requireContext(), fileList.map {
                Media3Item.create(it.name, it.rawURL, false)
            }, position)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setFileList(list: List<FileEntity>, position: Int): DetailVideoDialog {
        arguments?.putSerializable("list", list as Serializable)
        arguments?.putInt("position", position)
        return this
    }

    private fun downloadGif(file: FileEntity) {
        val dialog = LoadDialog(parent).setMessage("正在缓存动图...")
        scopeDialog(dialog, cancelable = true) {
            val outFile = Get<File>(file.gifURL) {
                setDownloadDir(parent.externalFilesDir("Gif"))
                setDownloadFileName(file.name)
            }.await()
            parent.saveToGallery(outFile)
            parent.toast("已保存动图到相册")
        }.catch {
            parent.toast("保存失败：${it.toMessage()}")
        }
    }
}