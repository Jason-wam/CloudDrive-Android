package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.MenuCompat
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
import com.jason.cloud.drive.utils.extension.view.bindRvElevation
import com.jason.cloud.drive.views.dialog.showFileMenu
import com.jason.cloud.drive.views.fragment.FilesFragmentViewModel
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration
import com.jason.cloud.extension.toast
import com.jason.videocat.utils.extension.view.onMenuItemClickListener

class SearchFilesActivity :
    BaseBindActivity<ActivitySearchFilesBinding>(R.layout.activity_search_files) {

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

    override fun initView() {
        initToolBar()
        initSearchView()
        initViewModel()

        binding.stateLayout.bindView(stateLogo, R.layout.layout_state_logo)
        binding.stateLayout.switchView(stateLogo)

        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(this))
        binding.appBarLayout.bindRvElevation(binding.rvData)
    }

    private fun initToolBar() {
        MenuCompat.setGroupDividerEnabled(binding.toolbar.menu, true)
        binding.toolbar.onMenuItemClickListener(R.id.name) {
            Configure.SearchConfigure.sortModel = FilesFragmentViewModel.ListSort.NAME
            binding.stateLayout.showLoading()
            viewModel.refresh()
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.name_desc) {
            Configure.SearchConfigure.sortModel = FilesFragmentViewModel.ListSort.NAME_DESC
            binding.stateLayout.showLoading()
            viewModel.refresh()
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.date) {
            Configure.SearchConfigure.sortModel = FilesFragmentViewModel.ListSort.DATE
            binding.stateLayout.showLoading()
            viewModel.refresh()
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.date_desc) {
            Configure.SearchConfigure.sortModel = FilesFragmentViewModel.ListSort.DATE_DESC
            binding.stateLayout.showLoading()
            viewModel.refresh()
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.size) {
            Configure.SearchConfigure.sortModel = FilesFragmentViewModel.ListSort.SIZE
            binding.stateLayout.showLoading()
            viewModel.refresh()
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.size_desc) {
            Configure.SearchConfigure.sortModel = FilesFragmentViewModel.ListSort.SIZE_DESC
            binding.stateLayout.showLoading()
            viewModel.refresh()
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.show_hidden) {
            Configure.SearchConfigure.showHidden = !Configure.SearchConfigure.showHidden
            binding.stateLayout.showLoading()
            viewModel.refresh()
            updateSortMenu()
        }
        updateSortMenu()
    }

    private fun updateSortMenu() {
        val sort = Configure.SearchConfigure.sortModel
        binding.toolbar.menu.findItem(R.id.name).isChecked =
            sort == FilesFragmentViewModel.ListSort.NAME
        binding.toolbar.menu.findItem(R.id.date).isChecked =
            sort == FilesFragmentViewModel.ListSort.DATE
        binding.toolbar.menu.findItem(R.id.size).isChecked =
            sort == FilesFragmentViewModel.ListSort.SIZE
        binding.toolbar.menu.findItem(R.id.name_desc).isChecked =
            sort == FilesFragmentViewModel.ListSort.NAME_DESC
        binding.toolbar.menu.findItem(R.id.date_desc).isChecked =
            sort == FilesFragmentViewModel.ListSort.DATE_DESC
        binding.toolbar.menu.findItem(R.id.size_desc).isChecked =
            sort == FilesFragmentViewModel.ListSort.SIZE_DESC

        binding.toolbar.menu.findItem(R.id.show_hidden).isChecked =
            Configure.SearchConfigure.showHidden
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initViewModel() {
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
}