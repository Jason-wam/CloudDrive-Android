package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuCompat
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.CloudFileAdapter
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.databinding.ActivitySearchTypeFilesBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.FileType.Media.*
import com.jason.cloud.drive.utils.ListSort
import com.jason.cloud.drive.utils.actions.showFileMenu
import com.jason.cloud.drive.utils.actions.showFolderMenu
import com.jason.cloud.drive.utils.extension.view.bindRvElevation
import com.jason.cloud.drive.viewmodel.SearchTypeFilesViewModel
import com.jason.cloud.drive.views.widgets.decoration.FileListDecoration
import com.jason.cloud.extension.startActivity
import com.jason.cloud.extension.toast

class SearchTypeFilesActivity :
    BaseBindActivity<ActivitySearchTypeFilesBinding>(R.layout.activity_search_type_files),
    Toolbar.OnMenuItemClickListener {

    private val viewModel by lazy {
        ViewModelProvider(this)[SearchTypeFilesViewModel::class.java]
    }

    private val adapter = CloudFileAdapter().apply {
        addOnClickObserver { position, item, _ ->
            if (item.isDirectory) {
                FileBrowserActivity.openFolder(context, item.hash)
            } else {
                showFileMenu(itemData, position, onDelete = {
                    removeFileIndex(item)
                }, onRenamed = {
                    binding.stateLayout.showLoading()
                    viewModel.refresh()
                })
            }
        }

        addOnLongClickObserver { position, item, _ ->
            if (item.isDirectory) {
                showFolderMenu(
                    item,
                    onDelete = {
                        removeFileIndex(item)
                    },
                    onRenamed = {
                        binding.stateLayout.showLoading()
                        viewModel.refresh()
                    }
                )
            } else {
                showFileMenu(
                    itemData,
                    position,
                    onDelete = {
                        removeFileIndex(item)
                    },
                    onRenamed = {
                        binding.stateLayout.showLoading()
                        viewModel.refresh()
                    }
                )
            }
        }
    }

    private fun removeFileIndex(item: FileEntity) {
        val index = adapter.itemData.indexOfFirst {
            item.path == it.path && item.hash == it.hash
        }
        if (index != -1) {
            adapter.removeData(index)
            adapter.notifyItemRemoved(index)
            adapter.notifyItemRangeChanged(index, adapter.itemCount)
        }
    }

    companion object {
        fun search(context: Context, type: FileType.Media) {
            context.startActivity(SearchTypeFilesActivity::class) {
                putExtra("type", type.name)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        MenuCompat.setGroupDividerEnabled(binding.toolbar.menu, true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener(this)

        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(FileListDecoration(this))
        binding.appBarLayout.bindRvElevation(binding.rvData)

        updateSortMenu()

        viewModel.onError.observe(this) {
            binding.refreshLayout.finishRefresh(false)
            binding.refreshLayout.finishLoadMore(false)
            if (adapter.itemData.isNotEmpty()) {
                toast(it)
            } else {
                binding.stateLayout.showError(it) {
                    binding.stateLayout.showLoading()
                    viewModel.refresh()
                }
            }
        }

        viewModel.onSucceed.observe(this) {
            binding.refreshLayout.finishRefresh(true)
            binding.refreshLayout.finishLoadMore(true)
            binding.refreshLayout.setNoMoreData(it.hasMore.not())

            adapter.addData(it.list)
            adapter.notifyDataSetChanged()
            if (adapter.itemData.isEmpty()) {
                binding.stateLayout.showEmpty(R.string.state_view_nothing_searched)
            } else {
                binding.stateLayout.showContent()
            }
        }

        binding.refreshLayout.setOnRefreshListener {
            adapter.clear()
            adapter.notifyDataSetChanged()
            viewModel.refresh()
        }

        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.nextPage()
        }

        val type = intent.getStringExtra("type")?.let { valueOf(it) } ?: return
        updateTitle(type)
        search(type)
    }

    private fun updateTitle(type: FileType.Media) {
        binding.toolbar.title = when (type) {
            VIDEO -> getString(R.string.video)
            IMAGE -> getString(R.string.image)
            AUDIO -> getString(R.string.audio)
            COMPRESS -> getString(R.string.compress)
            DOCUMENTS -> getString(R.string.documents)
            PPT -> getString(R.string.ppt)
            TEXT -> getString(R.string.text)
            WORD -> getString(R.string.word)
            EXCEL -> getString(R.string.excel)
            APPLICATION -> getString(R.string.application)
            DATABASE -> getString(R.string.database)
            TORRENT -> getString(R.string.torrent)
            EXE -> getString(R.string.exe)
            WEB -> getString(R.string.web)
            FONT -> getString(R.string.font)
            FOLDER -> getString(R.string.folder)
            UNKNOWN -> getString(R.string.unknown)
        }
    }

    private fun updateSortMenu() {
        val sort = Configure.SearchConfigure.sortModel
        binding.toolbar.menu.children().forEach {
            when (it.itemId) {
                R.id.name -> it.isChecked = sort == ListSort.NAME
                R.id.date -> it.isChecked = sort == ListSort.DATE
                R.id.size -> it.isChecked = sort == ListSort.SIZE
                R.id.name_desc -> it.isChecked = sort == ListSort.NAME_DESC
                R.id.date_desc -> it.isChecked = sort == ListSort.DATE_DESC
                R.id.size_desc -> it.isChecked = sort == ListSort.SIZE_DESC
                R.id.show_hidden -> it.isChecked = Configure.SearchConfigure.showHidden
            }
        }
    }

    private fun Menu.children(): List<MenuItem> {
        return ArrayList<MenuItem>().apply {
            this@children.forEach { child ->
                add(child)
                if (child.hasSubMenu()) {
                    child.subMenu?.let {
                        MenuCompat.setGroupDividerEnabled(it, true)
                    }
                    addAll(child.subMenu?.children().orEmpty())
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun search(type: FileType.Media) {
        adapter.clear()
        adapter.notifyDataSetChanged()
        binding.stateLayout.showLoading()
        viewModel.search(type)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.name -> {
                Configure.SearchConfigure.sortModel = ListSort.NAME
                binding.stateLayout.showLoading()
                viewModel.refresh()
                updateSortMenu()
            }

            R.id.date -> {
                Configure.SearchConfigure.sortModel = ListSort.DATE
                binding.stateLayout.showLoading()
                viewModel.refresh()
                updateSortMenu()
            }

            R.id.size -> {
                Configure.SearchConfigure.sortModel = ListSort.SIZE
                binding.stateLayout.showLoading()
                viewModel.refresh()
                updateSortMenu()
            }

            R.id.name_desc -> {
                Configure.SearchConfigure.sortModel = ListSort.NAME_DESC
                binding.stateLayout.showLoading()
                viewModel.refresh()
                updateSortMenu()
            }

            R.id.date_desc -> {
                Configure.SearchConfigure.sortModel = ListSort.DATE_DESC
                binding.stateLayout.showLoading()
                viewModel.refresh()
                updateSortMenu()
            }

            R.id.size_desc -> {
                Configure.SearchConfigure.sortModel = ListSort.SIZE_DESC
                binding.stateLayout.showLoading()
                viewModel.refresh()
                updateSortMenu()
            }

            R.id.show_hidden -> {
                Configure.SearchConfigure.showHidden =
                    !Configure.SearchConfigure.showHidden
                binding.stateLayout.showLoading()
                viewModel.refresh()
                updateSortMenu()
            }
        }
        return true
    }
}