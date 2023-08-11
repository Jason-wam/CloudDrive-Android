package com.jason.cloud.drive.views.activity

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.base.BaseViewPager2Adapter
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.databinding.ActivityMainBinding
import com.jason.cloud.drive.interfaces.CallMainActivity
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.utils.extension.view.bindBottomNavigationView
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.drive.views.fragment.BrowseFragment
import com.jason.cloud.drive.views.fragment.HomeFragment
import com.jason.cloud.drive.views.fragment.MineFragment
import com.jason.cloud.drive.views.fragment.TasksFragment
import com.jason.cloud.extension.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

class MainActivity : BaseBindActivity<ActivityMainBinding>(R.layout.activity_main),
    CallMainActivity {
    private val viewPager2Adapter by lazy {
        BaseViewPager2Adapter(this).apply {
            addFragment("0", HomeFragment.newInstance())
            addFragment("1", BrowseFragment.newInstance())
            addFragment("2", TasksFragment.newInstance())
            addFragment("3", MineFragment.newInstance())
        }
    }

    override fun initView() {
        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.getChildAt(0)?.overScrollMode = View.OVER_SCROLL_NEVER
        binding.viewPager2.bindBottomNavigationView(binding.bottomNavigationView)
        binding.viewPager2.adapter = viewPager2Adapter

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> binding.viewPager2.setCurrentItem(0, false)
                R.id.file -> binding.viewPager2.setCurrentItem(1, false)
                R.id.transfer -> binding.viewPager2.setCurrentItem(2, false)
                R.id.mine -> binding.viewPager2.setCurrentItem(3, false)
            }
            true
        }

        showHistoriesDownloadDialog()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragment: Fragment? =
                    viewPager2Adapter.getFragment(binding.viewPager2.currentItem)
                if (fragment !is BrowseFragment) {
                    callBackPressed()
                } else {
                    if (fragment.isVisible && fragment.callBackPressed()) {
                        callBackPressed()
                    }
                }
            }
        })
    }

    /**
     * 检测未完成的取回任务
     */
    private fun showHistoriesDownloadDialog() {
        scopeNetLife {
            val taskList = withContext(Dispatchers.IO) {
                TaskDatabase.instance.getDownloadDao().list().first().filter {
                    it.status != DownloadTask.Status.SUCCEED && it.status != DownloadTask.Status.FAILED
                }
            }

            if (taskList.isNotEmpty()) {
                TextDialog(this@MainActivity).apply {
                    setCancelable(false)
                }.setTitle("历史任务")
                    .setText("检测到${taskList.size}个取回未完成的任务，是否继续取回任务？")
                    .onPositive("继续取回") {
                        resumeTasks(taskList)
                    }.onNegative("丢弃任务") {
                        dropTasks(taskList)
                    }.show()
            }
        }
    }

    private fun dropTasks(list: List<DownloadTaskEntity>) {
        toast("丢弃 ${list.size} 个取回任务！")
        scopeNetLife(dispatcher = Dispatchers.IO) {
            TaskDatabase.instance.getDownloadDao().delete(list)
            list.forEach {
                val file = File(it.path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun resumeTasks(list: List<DownloadTaskEntity>) {
        toast("继续 ${list.size} 个取回任务！")
        DownloadService.launchWith(
            context,
            ArrayList<DownloadService.DownloadParam>().apply {
                list.forEach { task ->
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
    }

    private var exitTime: Long = 0
    private fun callBackPressed() {
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