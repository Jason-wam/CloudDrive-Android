package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.DownloadTaskAdapter
import com.jason.cloud.drive.adapter.DownloadTaskDoneAdapter
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.downloader.DownloadQueue
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.CONNECTING
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.DOWNLOADING
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.FAILED
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.PAUSED
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.QUEUE
import com.jason.cloud.drive.database.downloader.DownloadTask.Status.SUCCEED
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.database.downloader.getStatusText
import com.jason.cloud.drive.databinding.ActivityTaskDownloadBinding
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.utils.ItemSelector
import com.jason.cloud.drive.utils.actions.showClearAllDownloadDoneTasks
import com.jason.cloud.drive.utils.actions.showClearAllDownloadTasks
import com.jason.cloud.drive.utils.actions.showDeleteDownloadDoneTasks
import com.jason.cloud.drive.utils.actions.showDeleteDownloadTasks
import com.jason.cloud.drive.utils.extension.view.bindNestedScrollViewElevation
import com.jason.cloud.drive.views.dialog.DownloadDoneTaskMenuDialog
import com.jason.cloud.drive.views.widgets.decoration.FileListDecoration
import com.jason.cloud.extension.toFileSizeString
import kotlinx.coroutines.Dispatchers
import java.io.File

class TaskDownloadActivity :
    BaseBindActivity<ActivityTaskDownloadBinding>(R.layout.activity_task_download),
    Toolbar.OnMenuItemClickListener {

    private val adapter = DownloadTaskAdapter().apply {
        addOnClickObserver { _, item, _ ->
            if (selector.isInSelectMode.not()) {
                if (item.isPaused()) {
                    item.status = QUEUE
                    DownloadQueue.instance.start()
                } else {
                    DownloadQueue.instance.pause(item)
                }
            }
        }

        selector.addOnSelectListener(object : ItemSelector.OnSelectListener<DownloadTask> {
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

            override fun onSelectChanged(selects: List<DownloadTask>) {
                binding.btnDelete.setOnClickListener {
                    showDeleteDownloadTasks(selects) {
                        selector.cancelSelect()
                    }
                }
            }
        })
    }

    private val adapterDone = DownloadTaskDoneAdapter().apply {
        addOnClickObserver { _, item, _ ->
            if (selector.isInSelectMode.not()) {
                DownloadDoneTaskMenuDialog(context).setFile(item)
                    .showNow(supportFragmentManager, "menu")
            }
        }

        addOnBindViewObserver { _, item, holder ->
            if (item.status == SUCCEED) {
                holder.binding.btnControl.setIconResource(R.drawable.ic_round_open_in_new_24)
                holder.binding.btnControl.text = "打开"
                holder.binding.btnControl.setOnClickListener {
                    DownloadDoneTaskMenuDialog(context).setFile(item)
                        .showNow(supportFragmentManager, "menu")
                }
            }
            if (item.status == FAILED) {
                holder.binding.btnControl.setIconResource(R.drawable.ic_round_refresh_24)
                holder.binding.btnControl.text = "重试"
                holder.binding.btnControl.setOnClickListener {
                    retryTask(item)
                }
            }
        }

        selector.addOnSelectListener(object : ItemSelector.OnSelectListener<DownloadTaskEntity> {
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

            override fun onSelectChanged(selects: List<DownloadTaskEntity>) {
                binding.btnDelete.setOnClickListener {
                    showDeleteDownloadDoneTasks(selects) {
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

        observeDownloadTasks()
        observeDownloadDoneTasks()

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
    private fun observeDownloadTasks() {
        scopeNetLife {
            DownloadQueue.instance.taskFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .collect { taskList ->
                    binding.tvTask.isVisible = taskList.isNotEmpty()
                    binding.rvTask.isVisible = taskList.isNotEmpty()

                    binding.toolbar.menu.findItem(R.id.start_all).isVisible =
                        taskList.find { it.isPaused() } != null
                    binding.toolbar.menu.findItem(R.id.pause_all).isVisible =
                        taskList.find { it.isPaused().not() } != null

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
                                task.downloadBytes.toFileSizeString() + " / " + task.totalBytes.toFileSizeString()

                            if (task.status == QUEUE) {
                                holder.binding.btnControl.text = "排队"
                                holder.binding.btnControl.setIconResource(R.drawable.ic_task_in_queue_24)
                                holder.binding.btnControl.setOnClickListener(null)
                            } else if (task.status == CONNECTING) {
                                holder.binding.btnControl.text = "准备"
                                holder.binding.btnControl.setIconResource(R.drawable.ic_round_connect_24)
                                holder.binding.btnControl.setOnClickListener(null)
                            } else if (task.status == DOWNLOADING) {
                                holder.binding.btnControl.text = "暂停"
                                holder.binding.btnControl.setIconResource(R.drawable.ic_round_pause_24)
                                holder.binding.btnControl.setOnClickListener {
                                    DownloadQueue.instance.pause(task)
                                }
                            } else if (task.status == PAUSED) {
                                holder.binding.btnControl.text = "开始"
                                holder.binding.btnControl.setIconResource(R.drawable.ic_round_play_arrow_24)
                                holder.binding.btnControl.setOnClickListener {
                                    task.status = QUEUE
                                    DownloadQueue.instance.start()
                                }
                            }
                        }
                    }
                }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun observeDownloadDoneTasks() {
        scopeNetLife {
            var rememberSize = 0
            TaskDatabase.instance.getDownloadDao().list()
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                    val list = it.filter { task ->
                        task.status == SUCCEED || task.status == FAILED
                    }
                    binding.tvTaskDone.isVisible = list.isNotEmpty()
                    binding.rvTaskDone.isVisible = list.isNotEmpty()
                    adapterDone.setData(list)
                    updateStateLayout()

                    if (rememberSize != list.size && adapterDone.selector.isInSelectMode.not()) {
                        rememberSize = list.size
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

    private fun retryTask(item: DownloadTaskEntity) {
        scopeNetLife(dispatcher = Dispatchers.IO) {
            TaskDatabase.instance.getDownloadDao().delete(item)
            DownloadService.launchWith(
                context,
                listOf(
                    DownloadService.DownloadParam(
                        item.name,
                        item.url,
                        item.hash,
                        File(item.dir)
                    )
                )
            )
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.start_all -> DownloadQueue.instance.startAll()

            R.id.pause_all -> DownloadQueue.instance.pauseAll()

            R.id.clear_all_downloads -> showClearAllDownloadTasks()

            R.id.clear_all_done_tasks -> showClearAllDownloadDoneTasks()
        }
        return true
    }
}