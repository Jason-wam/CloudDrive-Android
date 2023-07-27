package com.jason.cloud.drive.views.fragment.tasks

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.UploadTaskDoneAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.databinding.FragmentUploadDoneBinding
import com.jason.cloud.drive.interfaces.CallMainActivity
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration

class UploadDoneFragment :
    BaseBindFragment<FragmentUploadDoneBinding>(R.layout.fragment_upload_done) {

    companion object {
        @JvmStatic
        fun newInstance() = UploadDoneFragment()
    }

    private val adapter = UploadTaskDoneAdapter().apply {
        addOnClickObserver { _, item, _ ->
            if (activity is CallMainActivity) {
                val mainActivity = activity as CallMainActivity
                mainActivity.locateFileLocation(item.hash, item.childHash)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView(context: Context) {
        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(requireContext()))
        scopeNetLife {
            binding.stateLayout.showLoading()
            TaskDatabase.instance.getUploadDao().list()
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