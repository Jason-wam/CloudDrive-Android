package com.jason.cloud.drive.views.fragment.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.UploadTaskAdapter
import com.jason.cloud.drive.adapter.UploadTaskDoneAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.uploader.UploadQueue
import com.jason.cloud.drive.databinding.FragmentUploadDoneBinding
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class UploadDoneFragment :
    BaseBindFragment<FragmentUploadDoneBinding>(R.layout.fragment_upload_done) {

    companion object {
        @JvmStatic
        fun newInstance() = UploadDoneFragment()
    }

    private val adapter = UploadTaskDoneAdapter()

    @SuppressLint("NotifyDataSetChanged")
    override fun initView(context: Context) {
        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(requireContext()))
        scopeNetLife {
            binding.stateLayout.showLoading()
            TaskDatabase.INSTANCE.getUploadDao().list()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                adapter.setData(it)
                adapter.notifyDataSetChanged()
                if (it.isEmpty()) {
                    binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
                } else {
                    binding.stateLayout.showContent()
                }
            }
        }
    }
}