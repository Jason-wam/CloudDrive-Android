package com.jason.cloud.drive.views.fragment.tasks

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.DownloadTaskDoneAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.databinding.FragmentDownloadDoneBinding
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration

class DownloadDoneFragment :
    BaseBindFragment<FragmentDownloadDoneBinding>(R.layout.fragment_download_done) {
    companion object {
        @JvmStatic
        fun newInstance() = DownloadDoneFragment()
    }

    private val adapter = DownloadTaskDoneAdapter()

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun initView(context: Context) {
        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(requireContext()))

        scopeNetLife {
            binding.stateLayout.showLoading()
            TaskDatabase.INSTANCE.getDownloadDao().list()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                    val list = it.filter { task ->
                        task.status == DownloadTask.Status.SUCCEED ||
                                task.status == DownloadTask.Status.FAILED
                    }

                    adapter.setData(list)
                    adapter.notifyDataSetChanged()
                    if (list.isEmpty()) {
                        binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
                    } else {
                        binding.stateLayout.showContent()
                    }
                }
        }
    }
}