package com.jason.cloud.drive.views.fragment.tasks

import android.annotation.SuppressLint
import android.content.Context
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.UploadQueueAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.databinding.FragmentUploadBinding
import com.jason.cloud.drive.utils.extension.toFileSizeString
import com.jason.cloud.drive.utils.uploader.UploadQueue
import com.jason.cloud.drive.utils.uploader.getStatusText
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration
import kotlinx.coroutines.flow.collectLatest

class UploadFragment : BaseBindFragment<FragmentUploadBinding>(R.layout.fragment_upload) {
    companion object {
        @JvmStatic
        fun newInstance() = UploadFragment()
    }

    private val adapter = UploadQueueAdapter()

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun initView(context: Context) {
        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(requireContext()))
        adapter.setData(UploadQueue.instance.tasks)
        adapter.notifyDataSetChanged()

        if (UploadQueue.instance.tasks.isEmpty()) {
            binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
        } else {
            binding.stateLayout.showContent()
        }

        scopeNetLife {
            UploadQueue.instance.runningTaskFlow.collectLatest { taskList ->
                if (taskList.size != adapter.itemData.size) {
                    adapter.setData(taskList)
                    adapter.notifyDataSetChanged()
                }
                if (taskList.isEmpty()) {
                    binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
                } else {
                    binding.stateLayout.showContent()
                }

                taskList.forEachIndexed { index, uploader ->
                    adapter.getViewHolder(binding.rvData, index)?.let { holder ->
                        holder.binding.indicator.progress = uploader.progress
                        holder.binding.tvStatus.text = uploader.getStatusText()
                        holder.binding.tvSize.text =
                            uploader.uploadedBytes.toFileSizeString() + " / " + uploader.totalBytes.toFileSizeString()
                    }
                }
            }
        }
    }
}