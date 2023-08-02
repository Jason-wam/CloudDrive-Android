package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuCompat
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import com.drake.softinput.hasSoftInput
import com.drake.softinput.hideSoftInput
import com.drake.softinput.setWindowSoftInput
import com.drake.softinput.showSoftInput
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.CloudFileAdapter
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.databinding.ActivitySearchFilesBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.ListSort
import com.jason.cloud.drive.utils.extension.view.bindRvElevation
import com.jason.cloud.drive.utils.extension.view.onMenuItemClickListener
import com.jason.cloud.drive.views.dialog.showFileMenu
import com.jason.cloud.drive.views.widgets.decoration.FileListDecoration
import com.jason.cloud.extension.toast

class SearchFilesActivity :
    BaseBindActivity<ActivitySearchFilesBinding>(R.layout.activity_search_files),
    Toolbar.OnMenuItemClickListener {

    private val stateLogo = "stateLogo"
    private val viewModel by lazy {
        ViewModelProvider(this)[SearchFilesActivityViewModel::class.java]
    }

    private val adapter = CloudFileAdapter().apply {
        addOnClickObserver { position, item, _ ->
            if (item.isDirectory) {
                toast("浏览目录：${item.path}")
            } else {
                showFileMenu(itemData, position) {
                    removeFileIndex(item)
                }
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

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        MenuCompat.setGroupDividerEnabled(binding.toolbar.menu, true)
        binding.toolbar.setOnMenuItemClickListener(this)

        binding.stateLayout.bindView(stateLogo, R.layout.layout_state_logo)
        binding.stateLayout.switchView(stateLogo)

        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(FileListDecoration(this))
        binding.appBarLayout.bindRvElevation(binding.rvData)

        updateSortMenu()
        initSearchView()

        viewModel.onError.observe(this) {
            binding.refreshLayout.finishRefresh(false)
            binding.refreshLayout.finishLoadMore(false)
            if (adapter.itemData.isEmpty()) {
                binding.stateLayout.showError(it)
            } else {
                toast(it)
            }
        }

        viewModel.onSucceed.observe(this) {
            it.hasMore
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
                    addAll(child.subMenu?.children().orEmpty())
                }
            }
        }
    }

    private fun initSearchView() {
        fun showDimView() {
            binding.dimView.alpha = 0f
            binding.dimView.visibility = View.VISIBLE
            binding.dimView.animate().alphaBy(0f).alpha(1f).duration = 300
            binding.dimView.setOnClickListener {
                it.isEnabled = false
                it.animate().alphaBy(1f).alpha(0f).setDuration(300).withEndAction {
                    it.isEnabled = true
                    it.visibility = View.GONE
                    binding.searchView.hideSoftInput()
                }
            }
        }

        fun hideDimView() {
            binding.dimView.animate().alphaBy(1f).alpha(0f).setDuration(300).withEndAction {
                binding.dimView.visibility = View.GONE
            }
            binding.dimView.setOnClickListener {
                it.isEnabled = false
                it.animate().alphaBy(1f).alpha(0f).setDuration(300).withEndAction {
                    it.isEnabled = true
                    it.visibility = View.GONE
                    binding.searchView.hideSoftInput()
                }
            }
        }

        setWindowSoftInput(float = binding.searchBar,
            editText = binding.searchView,
            setPadding = true,
            onChanged = {
                if (hasSoftInput()) {
                    showDimView()
                    viewModel.cancel()
                    binding.searchBar.setNavigationIcon(R.drawable.ic_round_keyboard_hide_24)
                    binding.searchBar.setNavigationOnClickListener {
                        binding.searchView.hideSoftInput()
                    }
                } else {
                    hideDimView()
                    binding.searchBar.setNavigationIcon(R.drawable.ic_round_keyboard_24)
                    binding.searchBar.setNavigationOnClickListener {
                        binding.searchView.showSoftInput()
                    }
                }
            }
        )

        binding.searchBar.addOnScrollStateChangedListener { _, _ ->
            if (binding.searchBar.isScrolledDown) {
                binding.searchView.hideSoftInput()
                hideDimView()
            }
        }

        binding.searchBar.setNavigationOnClickListener {
            binding.searchView.showSoftInput()
        }

        binding.searchBar.onMenuItemClickListener(R.id.search) {
            val text = binding.searchView.text
            if (text?.isNotBlank() == true) {
                search(text.toString())
            } else {
                toast(R.string.please_input_search_keywords)
            }
        }

        binding.searchView.onSearchListener {
            if (it.isNotBlank()) {
                search(it)
            } else {
                toast(R.string.please_input_search_keywords)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun search(text: String) {
        adapter.clear()
        adapter.notifyDataSetChanged()
        binding.stateLayout.showLoading()
        viewModel.search(text)
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