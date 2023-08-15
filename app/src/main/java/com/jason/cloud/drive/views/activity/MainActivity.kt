package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.drake.net.Get
import com.drake.net.cache.CacheMode
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.CloudFileAdapter
import com.jason.cloud.drive.adapter.MountedDirsAdapter
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.database.backup.BackupQueue
import com.jason.cloud.drive.database.downloader.DownloadTask
import com.jason.cloud.drive.database.downloader.DownloadTaskEntity
import com.jason.cloud.drive.database.uploader.UploadQueue
import com.jason.cloud.drive.databinding.ActivityMainBinding
import com.jason.cloud.drive.interfaces.CallMainActivity
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.model.MountedDirEntity
import com.jason.cloud.drive.service.BackupService
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.TaskQueue
import com.jason.cloud.drive.utils.extension.view.bindNestedScrollViewElevation
import com.jason.cloud.drive.utils.extension.view.setTitleFont
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.forEachObject
import com.jason.cloud.extension.startActivity
import com.jason.cloud.extension.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

class MainActivity : BaseBindActivity<ActivityMainBinding>(R.layout.activity_main),
    CallMainActivity {
    private val adapter = CloudFileAdapter().apply {
        addOnClickObserver { _, item, _ ->
            if (item.isDirectory) {
                FileBrowserActivity.openFolder(context, item.hash)
            } else {
                FileBrowserActivity.openFolder(context, item.parentHash, item.hash)
            }
        }
    }

    private val mountedDirsAdapter = MountedDirsAdapter().apply {
        addOnClickObserver { _, item, _ ->
            FileBrowserActivity.openFolder(context, item.hash)
        }
        addOnBindViewObserver { _, item, holder ->
            holder.binding.btnControl.setOnClickListener {
                if (BackupQueue.instance.taskList.isNotEmpty()) {
                    toast("仍有备份任务在执行，请稍候再试..")
                } else {
                    BackupService.launchWith(context, item.hash) {
                        binding.btnBackup.text = "正在执行备份任务.."
                        toast("正在备份文件..")
                        BackupQueue.instance.onTaskListDone(object :
                            TaskQueue.OnTaskListDoneListener {
                            override fun onTaskListDone() {
                                binding.btnBackup.setText(R.string.backup_files)
                            }
                        })
                    }
                }
            }
        }
    }

    override fun initView() {
        binding.appBarLayout.bindNestedScrollViewElevation(binding.nestedScrollView)
        binding.toolbar.setTitleFont("fonts/AaJianHaoTi.ttf")

        binding.btnSearch.setOnClickListener { startActivity(SearchFilesActivity::class) }
        binding.btnVideo.setOnClickListener {
            SearchTypeFilesActivity.search(this, FileType.Media.VIDEO)
        }
        binding.btnImage.setOnClickListener {
            SearchTypeFilesActivity.search(this, FileType.Media.IMAGE)
        }
        binding.btnAudio.setOnClickListener {
            SearchTypeFilesActivity.search(this, FileType.Media.AUDIO)
        }
        binding.btnDocument.setOnClickListener {
            SearchTypeFilesActivity.search(this, FileType.Media.DOCUMENTS)
        }
        binding.btnCommpress.setOnClickListener {
            SearchTypeFilesActivity.search(this, FileType.Media.COMPRESS)
        }

        binding.btnUploads.setOnClickListener {
            startActivity(TaskUploadActivity::class)
        }
        binding.btnDownloads.setOnClickListener {
            startActivity(TaskDownloadActivity::class)
        }

        binding.rvMountedDirs.adapter = mountedDirsAdapter

        binding.rvRecent.adapter = adapter
        binding.refreshLayout.setOnRefreshListener {
            loadRecentFiles()
        }

        observeTaskSize()
        showHistoriesDownloadDialog()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                callBackPressed()
            }
        })
    }

    private fun observeTaskSize() {
        scopeNetLife(dispatcher = Dispatchers.IO) {
            launch {
                UploadQueue.instance.taskSizeFlow
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .collect {
                        val size = TaskDatabase.instance.getUploadDao().count()
                        withContext(Dispatchers.Main) {
                            binding.btnUploads.text = getString(
                                R.string.upload_task_formatter,
                                UploadQueue.instance.taskList.size + size
                            )
                        }
                    }
                TaskDatabase.instance.getUploadDao().list()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .collect {
                        withContext(Dispatchers.Main) {
                            binding.btnUploads.text = getString(
                                R.string.upload_task_formatter,
                                UploadQueue.instance.taskList.size + it.size
                            )
                        }
                    }
            }
            launch {
                TaskDatabase.instance.getDownloadDao().list()
                    .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                    .collect {
                        withContext(Dispatchers.Main) {
                            binding.btnDownloads.text = getString(
                                R.string.download_task_formatter,
                                it.size
                            )
                        }
                    }
            }
        }
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


    /**
     * 获取存储信心和最近文件
     */
    override fun onResume() {
        super.onResume()
        loadRecentFiles()
    }

    private fun loadRecentFiles() {
        scopeNetLife(dispatcher = Dispatchers.IO) {
            val obj = Get<String>("${Configure.hostURL}/homePage") {
                param("recentSize", 50)
                param("showHidden", Configure.CloudFileConfigure.showHidden)
                setHeader("password", Configure.password)
                setCacheMode(CacheMode.WRITE)
            }.await().asJSONObject()

            parseHomePage(obj)
        }.preview {
            val obj = Get<String>("${Configure.hostURL}/homePage") {
                param("size", 50)
                param("showHidden", Configure.CloudFileConfigure.showHidden)
                setHeader("password", Configure.password)
                setCacheMode(CacheMode.READ)
            }.await().asJSONObject()

            parseHomePage(obj)
        }.finally {
            binding.refreshLayout.finishRefresh(it == null)
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private suspend fun parseHomePage(obj: JSONObject) {
        val mountedDirList = ArrayList<MountedDirEntity>().apply {
            obj.optJSONArray("mountedDirs")?.forEachObject {
                add(
                    MountedDirEntity(
                        it.optString("hash"),
                        it.optString("name"),
                        it.optLong("usedStorage"),
                        it.optLong("totalStorage"),
                        it.optLong("selfUsedStorage"),
                        it.optString("usedStorageText"),
                        it.optString("totalStorageText"),
                        it.optString("selfUsedStorageText"),
                    )
                )
            }
        }

        mountedDirsAdapter.setData(mountedDirList)
        withContext(Dispatchers.Main) {
            mountedDirsAdapter.notifyDataSetChanged()
        }

        obj.optJSONArray("recentFiles")?.let {
            adapter.setData(ArrayList<FileEntity>().apply {
                for (i in 0 until it.length()) {
                    add(FileEntity.createFromJson(it.getJSONObject(i)))
                }
            })
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
                binding.cardRecent.isVisible = adapter.itemData.isNotEmpty()
            }
        }
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