package com.jason.cloud.drive.views.fragment

import android.animation.AnimatorInflater
import android.content.Context
import android.view.View
import androidx.core.view.MenuCompat
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.base.BaseViewPager2Adapter
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.downloader.DownloadQueue
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.uploader.UploadQueue
import com.jason.cloud.drive.database.uploader.UploadTask
import com.jason.cloud.drive.databinding.FragmentTasksBinding
import com.jason.cloud.drive.utils.extension.view.onMenuItemClickListener
import com.jason.cloud.drive.utils.extension.view.onTabSelected
import com.jason.cloud.drive.utils.extension.view.setTitleFont
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.drive.views.fragment.tasks.UploadDoneFragment
import com.jason.cloud.drive.views.fragment.tasks.UploadFragment
import com.jason.cloud.extension.toast
import kotlinx.coroutines.Dispatchers

class TasksFragment : BaseBindFragment<FragmentTasksBinding>(R.layout.fragment_tasks) {
    companion object {
        fun newInstance() = TasksFragment()
    }

    override fun initView(context: Context) {
        initToolBar()

        binding.viewPager2.offscreenPageLimit = 3
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.getChildAt(0)?.overScrollMode = View.OVER_SCROLL_NEVER
        binding.viewPager2.adapter = BaseViewPager2Adapter(this).apply {
            addFragment("2", UploadFragment.newInstance())
            addFragment("3", UploadDoneFragment.newInstance())
        }

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("上传任务"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("上传完成任务"))

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("离线任务"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("离线完成任务"))

        binding.tabLayout.onTabSelected {
            binding.viewPager2.setCurrentItem(it.position, false)
        }

        binding.indicatorBar.addOnOffsetChangedListener { _, verticalOffset ->
            binding.appBarLayout.stateListAnimator = if (verticalOffset != 0) {
                AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_layout_elevation)
            } else {
                AnimatorInflater.loadStateListAnimator(
                    context, R.animator.appbar_layout_elevation_nil
                )
            }
        }
    }

    private fun initToolBar() {
        MenuCompat.setGroupDividerEnabled(binding.toolbar.menu, true)

        binding.toolbar.setTitleFont("fonts/AaJianHaoTi.ttf")
        binding.toolbar.onMenuItemClickListener(R.id.start_all_uploads) {
            toast("暂不支持启动上传任务")
        }
        binding.toolbar.onMenuItemClickListener(R.id.start_all_downloads) {
            DownloadQueue.instance.startAll()
            toast("已启动全部取回任务")
        }

        binding.toolbar.onMenuItemClickListener(R.id.pause_all_uploads) {
            toast("暂不支持暂停上传任务")
        }
        binding.toolbar.onMenuItemClickListener(R.id.pause_all_downloads) {
            DownloadQueue.instance.pauseAll()
            toast("已暂停全部取回任务")
        }
        binding.toolbar.onMenuItemClickListener(R.id.cancel_all_uploads) {
            showCancelAllUploadsDialog()
        }
        binding.toolbar.onMenuItemClickListener(R.id.cancel_all_downloads) {
            showCancelAllDownloadsDialog()
        }
        binding.toolbar.onMenuItemClickListener(R.id.clear_all_uploads) {
            showClearAllUploadsDialog()
        }
        binding.toolbar.onMenuItemClickListener(R.id.clear_all_downloads) {
            showClearAllDownloadsDialog()
        }
    }

    private fun showCancelAllUploadsDialog() {
        TextDialog(requireContext())
            .setTitle("上传任务")
            .setText("是否确认取消全部上传任务？")
            .onPositive("取消") {
            }.onNegative("确认取消") {
                scopeNetLife(dispatcher = Dispatchers.IO) {
                    UploadQueue.instance.cancelAll()
                    toast("已取消全部上传任务")
                }
            }.show()
    }

    private fun showCancelAllDownloadsDialog() {
        TextDialog(requireContext())
            .setTitle("取回任务")
            .setText("是否确认取消全部取回任务？")
            .onPositive("取消") {
            }.onNegative("确认取消") {
                scopeNetLife(dispatcher = Dispatchers.IO) {
                    toast("已取消全部取回任务")
                    DownloadQueue.instance.cancelAll()
                    TaskDatabase.instance.getDownloadDao().clear(DownloadTask.Status.QUEUE)
                    TaskDatabase.instance.getDownloadDao().clear(DownloadTask.Status.PAUSED)
                    TaskDatabase.instance.getDownloadDao().clear(DownloadTask.Status.CONNECTING)
                    TaskDatabase.instance.getDownloadDao().clear(DownloadTask.Status.DOWNLOADING)
                }
            }.show()
    }

    private fun showClearAllUploadsDialog() {
        TextDialog(requireContext())
            .setTitle("上传任务")
            .setText("是否确认清空全部上传记录？")
            .onPositive("取消") {
            }.onNegative("确认清空") {
                scopeNetLife(dispatcher = Dispatchers.IO) {
                    toast("已清空全部上传记录")
                    TaskDatabase.instance.getUploadDao().clear(UploadTask.Status.FAILED)
                    TaskDatabase.instance.getUploadDao().clear(UploadTask.Status.SUCCEED)
                    TaskDatabase.instance.getUploadDao().clear(UploadTask.Status.FLASH_UPLOADED)
                }
            }.show()
    }

    private fun showClearAllDownloadsDialog() {
        TextDialog(requireContext())
            .setTitle("取回任务")
            .setText("是否确认清空全部上取回任务？")
            .onPositive("取消") {
            }.onNegative("保留文件") {
                scopeNetLife(dispatcher = Dispatchers.IO) {
                    toast("已清空全部取回记录")
                    TaskDatabase.instance.getDownloadDao().clear(DownloadTask.Status.FAILED)
                    TaskDatabase.instance.getDownloadDao().clear(DownloadTask.Status.SUCCEED)
                }
            }.show()
    }
}