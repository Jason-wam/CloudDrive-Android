package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.UploadTaskAdapter
import com.jason.cloud.drive.adapter.UploadTaskDoneAdapter
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.uploader.UploadQueue
import com.jason.cloud.drive.database.uploader.UploadTask
import com.jason.cloud.drive.database.uploader.UploadTaskEntity
import com.jason.cloud.drive.database.uploader.getStatusText
import com.jason.cloud.drive.databinding.ActivityTaskUploadBinding
import com.jason.cloud.drive.service.UploadService
import com.jason.cloud.drive.utils.ItemSelector
import com.jason.cloud.drive.utils.actions.showCancelUploadTasks
import com.jason.cloud.drive.utils.actions.showClearAllUploadDoneTasks
import com.jason.cloud.drive.utils.actions.showClearAllUploadTasks
import com.jason.cloud.drive.utils.actions.showDeleteUploadDoneTasks
import com.jason.cloud.drive.utils.extension.view.bindNestedScrollViewElevation
import com.jason.cloud.drive.views.widgets.decoration.FileListDecoration
import com.jason.cloud.extension.toFileSizeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskUploadActivity :
    BaseBindActivity<ActivityTaskUploadBinding>(R.layout.activity_task_upload),
    Toolbar.OnMenuItemClickListener {
    private val adapter = UploadTaskAdapter().apply {
        selector.addOnSelectListener(object : ItemSelector.OnSelectListener<UploadTask> {
            override fun onSelectStart() {
                binding.btnDelete.show()
                binding.rvTaskDoneOverly.isVisible = true
                binding.rvTaskDoneOverly.setOnClickListener { }
                binding.tvTaskDone.alpha = 0.5f
                binding.rvTaskDone.alpha = 0.5f
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onSelectCanceled() {
                binding.btnDelete.hide()
                binding.rvTaskDoneOverly.isVisible = false
                binding.tvTaskDone.alpha = 1f
                binding.rvTaskDone.alpha = 1f
                notifyDataSetChanged()
            }

            override fun onSelectChanged(selects: List<UploadTask>) {
                binding.btnDelete.setOnClickListener {
                    showCancelUploadTasks(selects) {
                        selector.cancelSelect()
                    }
                }
            }
        })
    }

    private val adapterDone = UploadTaskDoneAdapter().apply {
        addOnClickObserver { _, item, _ ->
            FileBrowserActivity.locationTargetFile(context, item.hash, item.fileHash)
        }
        addOnBindViewObserver { _, item, holder ->
            if (item.status == UploadTask.Status.SUCCEED) {
                holder.binding.btnControl.setIconResource(R.drawable.ic_round_open_in_new_24)
                holder.binding.btnControl.text = "跳转"
                holder.binding.btnControl.setOnClickListener {
                    FileBrowserActivity.locationTargetFile(context, item.hash, item.fileHash)
                }
            }
            if (item.status == UploadTask.Status.FAILED) {
                holder.binding.btnControl.setIconResource(R.drawable.ic_round_refresh_24)
                holder.binding.btnControl.text = "重试"
                holder.binding.btnControl.setOnClickListener {
                    retryTask(item)
                }
            }
        }

        selector.addOnSelectListener(object : ItemSelector.OnSelectListener<UploadTaskEntity> {
            override fun onSelectStart() {
                binding.btnDelete.show()
                binding.rvTaskOverly.isVisible = true
                binding.rvTaskOverly.setOnClickListener { }
                binding.tvTask.alpha = 0.5f
                binding.rvTask.alpha = 0.5f
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onSelectCanceled() {
                binding.btnDelete.hide()
                binding.rvTaskOverly.isVisible = false
                binding.tvTask.alpha = 1f
                binding.rvTask.alpha = 1f
                notifyDataSetChanged()
            }

            override fun onSelectChanged(selects: List<UploadTaskEntity>) {
                binding.btnDelete.setOnClickListener {
                    showDeleteUploadDoneTasks(selects) {
                        selector.cancelSelect()
                    }
                }
            }
        })
    }

    override fun initView() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener(this)
        binding.appBarLayout.bindNestedScrollViewElevation(binding.nestedScrollView)

        binding.rvTask.adapter = adapter
        binding.rvTask.addItemDecoration(FileListDecoration(context))

        binding.rvTaskDone.adapter = adapterDone
        binding.rvTaskDone.addItemDecoration(FileListDecoration(context))

        observeUploadTasks()
        observeUploadDoneTasks()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.selector.isInSelectMode) {
                    adapter.selector.cancelSelect()
                    return
                }
                if (adapterDone.selector.isInSelectMode) {
                    adapterDone.selector.cancelSelect()
                    return
                }
                finish()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun observeUploadTasks() {
        scopeNetLife {
            UploadQueue.instance.taskFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { taskList ->
                    binding.tvTask.isVisible = taskList.isNotEmpty()
                    binding.rvTask.isVisible = taskList.isNotEmpty()

                    if (taskList.size != adapter.itemData.size) {
                        adapter.setData(taskList)
                        adapter.notifyDataSetChanged()
                        updateStateLayout()
                    }

                    taskList.forEachIndexed { index, task ->
                        adapter.getViewHolder(binding.rvTask, index)?.let { holder ->
                            holder.binding.indicator.setProgressCompat(task.progress, true)
                            holder.binding.tvStatus.text = task.getStatusText()
                            holder.binding.tvSize.text =
                                task.uploadedBytes.toFileSizeString() + " / " + task.totalBytes.toFileSizeString()

                            if (task.status == UploadTask.Status.QUEUE) {
                                holder.binding.btnControl.text = "排队"
                                holder.binding.btnControl.setIconResource(R.drawable.ic_task_in_queue_24)
                                holder.binding.btnControl.setOnClickListener(null)
                            } else if (task.status == UploadTask.Status.CONNECTING) {
                                holder.binding.btnControl.text = "准备"
                                holder.binding.btnControl.setIconResource(R.drawable.ic_round_connect_24)
                                holder.binding.btnControl.setOnClickListener(null)
                            } else if (task.status == UploadTask.Status.UPLOADING || task.status == UploadTask.Status.CHECKING) {
                                holder.binding.btnControl.text = "取消"
                                holder.binding.btnControl.setIconResource(R.drawable.ic_round_close_24)
                                holder.binding.btnControl.setOnClickListener {
                                    UploadQueue.instance.cancel(task)
                                }
                            }
                        }
                    }
                }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun observeUploadDoneTasks() {
        scopeNetLife {
            var rememberSize = 0
            TaskDatabase.instance.getUploadDao().list()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                    binding.tvTaskDone.isVisible = it.isNotEmpty()
                    binding.rvTaskDone.isVisible = it.isNotEmpty()
                    adapterDone.setData(it)
                    updateStateLayout()

                    if (rememberSize != it.size && adapterDone.selector.isInSelectMode.not()) {
                        rememberSize = it.size
                        adapterDone.notifyDataSetChanged()
                    }
                }
        }
    }

    private fun updateStateLayout() {
        if (adapter.itemData.isEmpty() && adapterDone.itemData.isEmpty()) {
            binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
        } else {
            binding.stateLayout.showContent()
        }
    }

    private fun retryTask(item: UploadTaskEntity) {
        scopeNetLife(dispatcher = Dispatchers.IO) {
            TaskDatabase.instance.getUploadDao().delete(item)
            withContext(Dispatchers.Main) {
                UploadService.launchWith(context, item.hash, listOf(item.uri.toUri()))
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.clear_all_uploads -> showClearAllUploadTasks()
            R.id.clear_all_done_tasks -> showClearAllUploadDoneTasks()
        }
        return true
    }

}