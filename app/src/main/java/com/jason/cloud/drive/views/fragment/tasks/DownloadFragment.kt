package com.jason.cloud.drive.views.fragment.tasks

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.DownloadTaskAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.database.downloader.DownloadQueue
import com.jason.cloud.drive.database.downloader.getStatusText
import com.jason.cloud.drive.databinding.FragmentDownloadBinding
import com.jason.cloud.drive.views.widgets.decoration.FileListDecoration
import com.jason.cloud.extension.toFileSizeString

class DownloadFragment : BaseBindFragment<FragmentDownloadBinding>(R.layout.fragment_download) {
    companion object {
        @JvmStatic
        fun newInstance() = DownloadFragment()
    }

    private val adapter = DownloadTaskAdapter()

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun initView(context: Context) {
        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(FileListDecoration(requireContext()))

        scopeNetLife {
            binding.stateLayout.showLoading()
            DownloadQueue.instance.taskFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
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

                    taskList.forEachIndexed { index, downloader ->
                        adapter.getViewHolder(binding.rvData, index)?.let { holder ->
                            holder.binding.indicator.setProgressCompat(downloader.progress, true)
                            holder.binding.tvStatus.text = downloader.getStatusText()
                            holder.binding.tvSize.text =
                                downloader.downloadBytes.toFileSizeString() + " / " + downloader.totalBytes.toFileSizeString()
                        }
                    }
                }
        }
    }
}