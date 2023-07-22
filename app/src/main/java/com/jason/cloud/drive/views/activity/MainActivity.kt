package com.jason.cloud.drive.views.activity

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.base.BaseViewPager2Adapter
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.databinding.ActivityMainBinding
import com.jason.cloud.drive.interfaces.CallActivityInterface
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.drive.views.fragment.FilesFragment
import com.jason.cloud.drive.views.fragment.HomeFragment
import com.jason.cloud.drive.views.fragment.MineFragment
import com.jason.cloud.drive.views.fragment.TasksFragment
import com.jason.cloud.extension.toast
import com.jason.videocat.utils.extension.view.bindBottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

class MainActivity : BaseBindActivity<ActivityMainBinding>(R.layout.activity_main),
    CallActivityInterface {

    override fun initView() {
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.getChildAt(0)?.overScrollMode = View.OVER_SCROLL_NEVER
        binding.viewPager2.bindBottomNavigationView(binding.bottomNavigationView)
        binding.viewPager2.adapter = BaseViewPager2Adapter(this).apply {
            addFragment("0", HomeFragment.newInstance())
            addFragment("1", FilesFragment.newInstance())
            addFragment("2", TasksFragment.newInstance())
            addFragment("3", MineFragment.newInstance())
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    binding.viewPager2.setCurrentItem(0, false)
                    return@setOnItemSelectedListener true
                }

                R.id.file -> {
                    binding.viewPager2.setCurrentItem(1, false)
                    return@setOnItemSelectedListener true
                }

                R.id.transfer -> {
                    binding.viewPager2.setCurrentItem(2, false)
                    return@setOnItemSelectedListener true
                }

                R.id.mine -> {
                    binding.viewPager2.setCurrentItem(3, false)
                    return@setOnItemSelectedListener true
                }

                else -> return@setOnItemSelectedListener true
            }
        }

        showHistoriesDownloadDialog()
    }

    /**
     * 检测未完成的取回任务
     */
    private fun showHistoriesDownloadDialog() {
        lifecycleScope.launch {
            val taskList = TaskDatabase.INSTANCE.getDownloadDao().list().first()
                .filter {
                    it.status != DownloadTask.Status.SUCCEED &&
                            it.status != DownloadTask.Status.FAILED
                }

            if (taskList.isNotEmpty()) {
                TextDialog(this@MainActivity)
                    .setTitle("历史任务")
                    .setText("检测到${taskList.size}个取回未完成的任务，是否继续取回任务？")
                    .onPositive("继续取回") {
                        DownloadService.launchWith(
                            context,
                            ArrayList<DownloadService.DownloadParam>().apply {
                                taskList.forEach { task ->
                                    add(
                                        DownloadService.DownloadParam(
                                            task.name,
                                            task.url,
                                            task.hash,
                                            File(task.dir)
                                        )
                                    )
                                }
                            }
                        )
                    }.onNegative("丢弃任务") {
                        dropTasks(taskList)
                    }.show()
            }
        }
    }

    private fun dropTasks(list: List<DownloadTaskEntity>) {
        toast("丢弃 ${list.size} 个取回任务！")
        lifecycleScope.launch(Dispatchers.IO) {
            TaskDatabase.INSTANCE.getDownloadDao().delete(list)
            list.forEach {
                val file = File(it.path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private var exitTime: Long = 0

    override fun callOnBackPressed() {
        if (System.currentTimeMillis() - exitTime > 2000) {
            toast("再按一次退出程序")
            exitTime = System.currentTimeMillis()
        } else {
            try {
                finishAndRemoveTask()
                exitProcess(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}