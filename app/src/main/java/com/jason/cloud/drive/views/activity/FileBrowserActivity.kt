package com.jason.cloud.drive.views.activity

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuCompat
import androidx.core.view.forEach
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.databinding.ActivityFileBrowserBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.service.BackupService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.ListSort
import com.jason.cloud.drive.views.fragment.FileListFragment
import com.jason.cloud.extension.getSerializableExtraEx
import com.jason.cloud.extension.startActivity
import com.jason.cloud.extension.toast

class FileBrowserActivity :
    BaseBindActivity<ActivityFileBrowserBinding>(R.layout.activity_file_browser),
    Toolbar.OnMenuItemClickListener {
    companion object {
        fun openFolder(context: Context, folder: FileEntity) {
            context.startActivity(FileBrowserActivity::class) {
                putExtra("folder", folder)
            }
        }

        /**
         * 定位到指定文件
         * @param hash 目标文件夹
         * @param fileHash 目标文件
         */
        fun locationTargetFile(context: Context, hash: String, fileHash: String) {
            context.startActivity(FileBrowserActivity::class) {
                putExtra("hash", hash)
                putExtra("fileHash", fileHash)
            }
        }
    }

    private lateinit var fragment: FileListFragment

    private fun showAppbarElevation() {
        binding.appBarLayout.stateListAnimator = AnimatorInflater.loadStateListAnimator(
            context,
            R.animator.appbar_layout_elevation
        )
    }

    private fun hideAppbarElevation() {
        binding.appBarLayout.stateListAnimator = AnimatorInflater.loadStateListAnimator(
            context,
            R.animator.appbar_layout_elevation_nil
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        MenuCompat.setGroupDividerEnabled(binding.toolbar.menu, true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener(this)
        updateSortMenu()

        if (intent.hasExtra("hash") && intent.hasExtra("fileHash")) {
            val hash = intent.getStringExtra("hash")!!
            val fileHash = intent.getStringExtra("fileHash")!!
            fragment = FileListFragment.newInstance(hash, fileHash)
        } else if (intent.hasExtra("folder")) {
            val folder = intent.getSerializableExtraEx("folder", FileEntity::class.java)!!
            fragment = FileListFragment.newInstance(folder)
        } else {
            fragment = FileListFragment.newInstance()
        }


        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment, "fragment")
        transaction.commit()

        fragment.setAppbarElevationCallback {
            if (it) {
                showAppbarElevation()
            } else {
                hideAppbarElevation()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fragment.callBackPressed()) {
                    finish()
                }
            }
        })
    }

    private fun updateSortMenu() {
        val sort = Configure.CloudFileConfigure.sortModel
        binding.toolbar.menu.children().forEach {
            when (it.itemId) {
                R.id.name -> it.isChecked = sort == ListSort.NAME
                R.id.date -> it.isChecked = sort == ListSort.DATE
                R.id.size -> it.isChecked = sort == ListSort.SIZE
                R.id.name_desc -> it.isChecked = sort == ListSort.NAME_DESC
                R.id.date_desc -> it.isChecked = sort == ListSort.DATE_DESC
                R.id.size_desc -> it.isChecked = sort == ListSort.SIZE_DESC
                R.id.show_hidden -> it.isChecked = Configure.CloudFileConfigure.showHidden
            }
        }
    }

    private fun Menu.children(): List<MenuItem> {
        return ArrayList<MenuItem>().apply {
            this@children.forEach { child ->
                add(child)
                if (child.hasSubMenu()) {
                    addAll(child.subMenu?.children().orEmpty())
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> startActivity(SearchFilesActivity::class)
            R.id.download -> startActivity(TaskDownloadActivity::class)
            R.id.folder -> fragment.createNewFolder()

            R.id.backup -> BackupService.launchWith(context) {
                toast("正在后台备份文件..")
            }

            R.id.name -> {
                Configure.CloudFileConfigure.sortModel = ListSort.NAME
                fragment.refresh(isGoBack = false)
                updateSortMenu()
            }

            R.id.date -> {
                Configure.CloudFileConfigure.sortModel = ListSort.DATE
                fragment.refresh(isGoBack = false)
                updateSortMenu()
            }

            R.id.size -> {
                Configure.CloudFileConfigure.sortModel = ListSort.SIZE
                fragment.refresh(isGoBack = false)
                updateSortMenu()
            }

            R.id.name_desc -> {
                Configure.CloudFileConfigure.sortModel = ListSort.NAME_DESC
                fragment.refresh(isGoBack = false)
                updateSortMenu()
            }

            R.id.date_desc -> {
                Configure.CloudFileConfigure.sortModel = ListSort.DATE_DESC
                fragment.refresh(isGoBack = false)
                updateSortMenu()
            }

            R.id.size_desc -> {
                Configure.CloudFileConfigure.sortModel = ListSort.SIZE_DESC
                fragment.refresh(isGoBack = false)
                updateSortMenu()
            }

            R.id.show_hidden -> {
                Configure.CloudFileConfigure.showHidden =
                    !Configure.CloudFileConfigure.showHidden
                fragment.refresh(isGoBack = false)
                updateSortMenu()
            }
        }
        return true
    }
}