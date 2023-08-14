package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.View
import com.drake.net.utils.scopeNetLife
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.databinding.LayoutFileAttachDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.DirManager
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.extension.getSerializableEx
import com.jason.cloud.extension.openFile
import com.jason.cloud.extension.toDateMinuteString
import com.jason.cloud.extension.toFileSizeString
import com.jason.cloud.extension.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AttachFileDialog :
    BaseBindBottomSheetDialogFragment<LayoutFileAttachDialogBinding>(R.layout.layout_file_attach_dialog) {
    private var downloadTask: DownloadTask? = null
    private var progressJob: Job? = null

    @SuppressLint("SetTextI18n")
    override fun initView(view: View) {
        super.initView(view)
        setCanceledOnTouchOutside(false)

        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.handler.isSelected = slideOffset != 0f
            }
        })

        arguments?.getSerializableEx("file", FileEntity::class.java)?.let {
            binding.tvName.text = it.name
            binding.tvURL.text = it.path
            binding.tvDate.text = it.date.toDateMinuteString()
            binding.tvSize.text = it.size.toFileSizeString()
            binding.indicator.progress = 0
            binding.tvSpeed.text = "0 B/s"
            attachFile(it)
        }

        binding.btnCancel.setOnClickListener {
            progressJob?.cancel()
            downloadTask?.cancelButSaveFile()
            dismiss()
        }
    }

    fun setFile(file: FileEntity): AttachFileDialog {
        arguments?.putSerializable("file", file)
        return this
    }

    @SuppressLint("SetTextI18n")
    private fun attachFile(file: FileEntity) {
        scopeNetLife(dispatcher = Dispatchers.IO) {
            val dir = DirManager.getAttachCacheDir(requireContext())
            downloadTask = DownloadTask(file.name, file.rawURL, file.hash, dir).start()
            progressJob = launch {
                while (true) {
                    delay(100)
                    withContext(Dispatchers.Main) {
                        if (downloadTask!!.status == DownloadTask.Status.CONNECTING) {
                            binding.btnOpen.text = "正在连接服务器..."
                        }
                        if (downloadTask!!.status == DownloadTask.Status.DOWNLOADING) {
                            binding.indicator.progress = downloadTask!!.progress
                            binding.tvSpeed.text = downloadTask!!.speedBytes.toFileSizeString("/s")
                            binding.btnOpen.text = "正在取回文件(${downloadTask!!.progress}%)..."
                        }
                    }
                    if (downloadTask!!.status == DownloadTask.Status.FAILED) {
                        withContext(Dispatchers.Main) {
                            binding.btnOpen.text = "取回文件失败！"
                            binding.btnOpen.setOnClickListener {
                                requireContext().openFile(downloadTask!!.file)
                            }
                        }
                    }
                    if (downloadTask!!.status == DownloadTask.Status.SUCCEED) {
                        withContext(Dispatchers.Main) {
                            binding.indicator.progress = 100
                            binding.btnOpen.text = "打开文件"
                            binding.btnOpen.setOnClickListener {
                                requireContext().openFile(downloadTask!!.file)
                            }
                        }
                        break
                    }
                }
            }
        }.catch {
            binding.btnOpen.text = "取回失败：${it.javaClass.name}"
            toast(it.toMessage())
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        progressJob?.cancel()
        downloadTask?.cancelButSaveFile()
    }
}