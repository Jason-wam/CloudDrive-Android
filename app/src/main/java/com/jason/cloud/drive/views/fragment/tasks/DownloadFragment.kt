package com.jason.cloud.drive.views.fragment.tasks

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.DownloadTaskAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.database.downloader.DownloadQueue
import com.jason.cloud.drive.database.downloader.getStatusText
import com.jason.cloud.drive.databinding.FragmentDownloadBinding
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.utils.extension.toFileSizeString
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration
import kotlinx.coroutines.launch

class DownloadFragment : BaseBindFragment<FragmentDownloadBinding>(R.layout.fragment_download),
    ServiceConnection {
    companion object {
        @JvmStatic
        fun newInstance() = DownloadFragment()
    }

    private val adapter = DownloadTaskAdapter()

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun initView(context: Context) {
        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(requireContext()))
        binding.stateLayout.showEmpty(R.string.state_view_nothing_here)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val service = Intent(context, DownloadService::class.java)
                context.bindService(service, this@DownloadFragment, 0)
            }
            repeatOnLifecycle(Lifecycle.State.DESTROYED) {
                context.unbindService(this@DownloadFragment)
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        if (binder is DownloadService.DownloadBinder) {
            scopeNetLife {
                binder.service.downloadQueue.taskFlow.flowWithLifecycle(
                    lifecycle,
                    Lifecycle.State.STARTED
                ).collect { taskList ->
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
                            holder.binding.indicator.setProgressCompat(
                                downloader.progress,
                                true
                            )
                            holder.binding.tvStatus.text = downloader.getStatusText()
                            holder.binding.tvSize.text =
                                downloader.downloadBytes.toFileSizeString() + " / " + downloader.totalBytes.toFileSizeString()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onServiceDisconnected(name: ComponentName?) {
        adapter.clear()
        adapter.notifyDataSetChanged()
        binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun test() {
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