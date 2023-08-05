package com.jason.cloud.drive.views.fragment

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuCompat
import androidx.core.view.forEach
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.databinding.FragmentBrowseBinding
import com.jason.cloud.drive.interfaces.CallFragment
import com.jason.cloud.drive.service.BackupService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.ListSort
import com.jason.cloud.drive.views.activity.SearchFilesActivity
import com.jason.cloud.extension.startActivity
import com.jason.cloud.extension.toast

class BrowseFragment : BaseBindFragment<FragmentBrowseBinding>(R.layout.fragment_browse),
    CallFragment, Toolbar.OnMenuItemClickListener {
    companion object {
        @JvmStatic
        fun newInstance() = BrowseFragment()
    }

    var isCreated = false

    private val fragment by lazy { FileListFragment.newInstance() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCreated = true
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView(context: Context) {
        MenuCompat.setGroupDividerEnabled(binding.toolbar.menu, true)
        binding.toolbar.setOnMenuItemClickListener(this)
        updateSortMenu()

        val transaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.container, fragment, "fragment")
        transaction.commit()

        fragment.onAppbarCallback {
            if (it) {
                showAppbarElevation()
            } else {
                hideAppbarElevation()
            }
        }
    }

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

    override fun callBackPressed(): Boolean {
        return childFragmentManager.findFragmentByTag("fragment")?.let {
            it as FileListFragment
            it.callBackPressed()
        } ?: super.callBackPressed()
    }

    override fun locateFileLocation(hash: String, fileHash: String) {
        super.locateFileLocation(hash, fileHash)
        childFragmentManager.findFragmentByTag("fragment")?.let {
            it as FileListFragment
            if (it.isVisible) {
                it.arguments = Bundle().apply {
                    putString("hash", hash)
                    putString("fileHash", fileHash)
                }
                it.refresh()
            }
        }
    }

    fun refresh() {
        val hash = arguments?.getString("hash").orEmpty()
        childFragmentManager.findFragmentByTag("fragment")?.let {
            it as FileListFragment
            if (it.isVisible) {
                it.arguments = Bundle().apply {
                    putString("hash", hash)
                }
                it.refresh()
            }
        }
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

            R.id.folder -> fragment.createNewFolder()

            R.id.backup -> BackupService.launchWith(requireContext()) {
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