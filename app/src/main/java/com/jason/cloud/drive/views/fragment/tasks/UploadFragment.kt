package com.jason.cloud.drive.views.fragment.tasks

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.UploadTaskAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.database.uploader.UploadQueue
import com.jason.cloud.drive.database.uploader.getStatusText
import com.jason.cloud.drive.databinding.FragmentUploadBinding
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration
import com.jason.cloud.extension.toFileSizeString

class UploadFragment : BaseBindFragment<FragmentUploadBinding>(R.layout.fragment_upload) {
    companion object {
        @JvmStatic
        fun newInstance() = UploadFragment()
    }

    private val adapter = UploadTaskAdapter()

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun initView(context: Context) {
        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(requireContext()))

        scopeNetLife {
            binding.stateLayout.showLoading()
            UploadQueue.instance.taskFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { taskList ->
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
                            holder.binding.indicator.setProgressCompat(uploader.progress, true)
                            holder.binding.tvStatus.text = uploader.getStatusText()
                            holder.binding.tvSize.text =
                                uploader.uploadedBytes.toFileSizeString() + " / " + uploader.totalBytes.toFileSizeString()
                        }
                    }
                }
        }
    }
}