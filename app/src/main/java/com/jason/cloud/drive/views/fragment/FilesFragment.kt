package com.jason.cloud.drive.views.fragment

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.net.Get
import com.drake.net.utils.scopeDialog
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.ColorSpan
import com.flyjingfish.openimagelib.OpenImage
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.CloudFileAdapter
import com.jason.cloud.drive.adapter.CloudFilePathIndicatorAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.contract.SelectFilesContract
import com.jason.cloud.drive.contract.SelectFolderContract
import com.jason.cloud.drive.databinding.FragmentFilesBinding
import com.jason.cloud.drive.interfaces.CallActivityInterface
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.model.toOpenImageUrl
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.service.UploadService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.DirManager
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.drive.viewmodel.FileViewModel
import com.jason.cloud.drive.views.activity.VideoPreviewActivity
import com.jason.cloud.drive.views.dialog.FileMenuDialog
import com.jason.cloud.drive.views.dialog.LoadDialog
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.drive.views.dialog.TextEditDialog
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration
import com.jason.cloud.drive.views.widgets.decoration.CloudFilePathIndicatorDecoration
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.toast
import com.jason.videocat.utils.extension.view.onMenuItemClickListener
import com.jason.videocat.utils.extension.view.setTitleFont
import com.jason.videoview.model.VideoData

class FilesFragment : BaseBindFragment<FragmentFilesBinding>(R.layout.fragment_files),
    FileMenuDialog.Callback {
    companion object {
        @JvmStatic
        fun newInstance() = FilesFragment()
    }

    /**
     * 记录RecyclerView当前位置
     */
    private val lastPosition: HashMap<String, Pair<Int, Int>> = HashMap()

    private lateinit var fileSelectLauncher: ActivityResultLauncher<String>
    private lateinit var selectFolderLauncher: ActivityResultLauncher<Any?>

    private val viewModel by lazy {
        ViewModelProvider(this)[FileViewModel::class.java]
    }

    private val adapter = CloudFileAdapter().apply {
        addOnClickObserver { position, item, _ ->
            if (item.isDirectory) {
                binding.stateLayout.showLoading()
                viewModel.getList(item)
            } else {
                FileMenuDialog().setFile(itemData, position)
                    .setCallback(this@FilesFragment).showNow(childFragmentManager, "menu")
            }
        }
    }

    private val indicatorAdapter = CloudFilePathIndicatorAdapter().apply {
        addOnBindViewObserver { _, item, viewHolder ->
            viewHolder.binding.tvPath.setOnClickListener {
                if (item.hash != viewModel.current()) {
                    binding.stateLayout.showLoading()
                    viewModel.getList(item.hash)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileSelectLauncher = registerForActivityResult(SelectFilesContract()) { uriList ->
            if (uriList.isNotEmpty()) {
                toast("开始上传 ${uriList.size} 个文件")
                UploadService.launchWith(requireContext(), viewModel.current(), uriList)
            }
        }

        selectFolderLauncher = registerForActivityResult(SelectFolderContract()) { uri ->
            if (uri != null) {
                DocumentFile.fromTreeUri(
                    requireContext(), uri
                )?.listFiles()?.filter {
                    it.isFile
                }?.onEach {
                    println("${it.name} >> ${it.uri}")
                }?.let { list ->
                    ArrayList<Uri>().apply {
                        list.forEach { file ->
                            add(file.uri)
                        }
                    }
                }?.let { uriList ->
                    if (uriList.isEmpty()) {
                        toast("此目录下没有查找到文件")
                    } else {
                        toast("开始上传 ${uriList.size} 个文件")
                        UploadService.launchWith(requireContext(), viewModel.current(), uriList)
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView(context: Context) {
        initToolBar()
        initRecyclerView()
        initViewModel()

        binding.fabUpload.setOnClickListener {
            fileSelectLauncher.launch("*/*")
//            selectFolderLauncher.launch(null)
        }

        binding.stateLayout.showLoading()
        viewModel.refresh(isGoBack = false)

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.isLoading) return
                if (viewModel.canGoBack()) {
                    binding.stateLayout.showLoading()
                    viewModel.goBack()
                } else {
                    if (activity is CallActivityInterface) {
                        (activity as CallActivityInterface).callOnBackPressed()
                    }
                }
            }
        })
    }

    private fun initToolBar() {
        binding.toolbar.setTitleFont("fonts/AaJianHaoTi.ttf")
        binding.toolbar.onMenuItemClickListener(R.id.refresh) {
            binding.stateLayout.showLoading()
            viewModel.refresh(isGoBack = false)
        }
        binding.toolbar.onMenuItemClickListener(R.id.folder) {
            createNewFolder()
        }
        binding.toolbar.onMenuItemClickListener(R.id.upload) {
            fileSelectLauncher.launch("*/*")
        }

        binding.toolbar.onMenuItemClickListener(R.id.name) {
            Configure.sortModel = FileViewModel.ListSort.NAME
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.name_desc) {
            Configure.sortModel = FileViewModel.ListSort.NAME_DESC
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.date) {
            Configure.sortModel = FileViewModel.ListSort.DATE
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.date_desc) {
            Configure.sortModel = FileViewModel.ListSort.DATE_DESC
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.size) {
            Configure.sortModel = FileViewModel.ListSort.SIZE
            updateSortMenu()
        }
        binding.toolbar.onMenuItemClickListener(R.id.size_desc) {
            Configure.sortModel = FileViewModel.ListSort.SIZE_DESC
            updateSortMenu()
        }
        updateSortMenu()

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

    private fun updateSortMenu() {
        val sort = Configure.sortModel
        binding.toolbar.menu.findItem(R.id.name).isChecked = sort == FileViewModel.ListSort.NAME
        binding.toolbar.menu.findItem(R.id.date).isChecked = sort == FileViewModel.ListSort.DATE
        binding.toolbar.menu.findItem(R.id.size).isChecked = sort == FileViewModel.ListSort.SIZE
        binding.toolbar.menu.findItem(R.id.name_desc).isChecked =
            sort == FileViewModel.ListSort.NAME_DESC
        binding.toolbar.menu.findItem(R.id.date_desc).isChecked =
            sort == FileViewModel.ListSort.DATE_DESC
        binding.toolbar.menu.findItem(R.id.size_desc).isChecked =
            sort == FileViewModel.ListSort.SIZE_DESC
        viewModel.refresh(isGoBack = false)
    }

    private fun initRecyclerView() {
        binding.rvPathIndicator.adapter = indicatorAdapter
        binding.rvPathIndicator.addItemDecoration(CloudFilePathIndicatorDecoration())

        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(CloudFileListDecoration(requireContext()))
        binding.rvData.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                recyclerView.layoutManager?.also { layoutManager ->
                    layoutManager.getChildAt(0)?.let {
                        //获取与该view的顶部的偏移量
                        val offset = it.top
                        //得到该View的数组位置
                        val position = layoutManager.getPosition(it)

                        lastPosition[viewModel.current()] = Pair(offset, position)
                    }
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initViewModel() {
        viewModel.onError.observe(this) {
            binding.stateLayout.showError(it) {
                binding.stateLayout.showLoading()
                viewModel.refresh(isGoBack = false)
            }
        }

        viewModel.onSucceed.observe(this) {
            binding.fabUpload.show()

            indicatorAdapter.currentHash = it.respond.hash
            indicatorAdapter.setData(it.respond.navigation)
            indicatorAdapter.notifyDataSetChanged()
            binding.rvPathIndicator.scrollToPosition(indicatorAdapter.itemCount - 1)

            adapter.setData(it.respond.list)
            adapter.notifyDataSetChanged()

            if (it.respond.list.isNotEmpty()) {
                binding.stateLayout.showContent()
            } else {
                binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
            }

            if (it.isGoBack.not()) {
                binding.rvData.scrollToPosition(0)
            } else {
                lastPosition[viewModel.current()]?.run {
                    binding.rvData.layoutManager?.let { manager ->
                        manager as LinearLayoutManager
                        manager.scrollToPositionWithOffset(second, first)
                        lastPosition[viewModel.current()] = Pair(0, 0)
                    }
                }
            }
        }
    }

    private fun createNewFolder() {
        fun create(name: String) {
            val dialog = LoadDialog(requireContext()).setMessage("正在创建文件夹...")
            scopeDialog(dialog, cancelable = true) {
                Get<String>("${Configure.hostURL}/createFolder") {
                    param("hash", viewModel.current())
                    param("name", name)
                }.await().asJSONObject().also {
                    if (it.optInt("code") == 200) {
                        toast("文件夹创建成功！")
                        viewModel.refresh(isGoBack = false)
                    } else {
                        toast(it.getString("message"))
                    }
                }
            }.catch {
                toast(it.toMessage())
            }
        }

        TextEditDialog(requireContext()).apply {
            setTitle("新建文件夹")
            setHintText("请输入文件夹名称...")
            onNegative("取消")
            onPositive {
                if (it.isNullOrBlank()) {
                    toast("请输入文件夹名称！")
                    false
                } else {
                    create(it.trim())
                    true
                }
            }
            show()
        }
    }

    override fun viewVideos(list: List<FileEntity>, position: Int) {
        VideoPreviewActivity.open(requireContext(), position, list.map {
            VideoData(it.hash, it.name, it.rawURL)
        })
    }

    override fun viewImages(list: List<FileEntity>, position: Int) {
        val current = list[position]
        var clickPosition = 0
        val imageUrlList = list.filter {
            FileType.isImage(it.name)
        }.mapIndexed { index, item ->
            if (current.path == item.path) {
                clickPosition = index
            }
            item.toOpenImageUrl()
        }

        OpenImage.with(requireActivity()).setNoneClickView().setImageUrlList(imageUrlList)
            .setClickPosition(clickPosition).show()
    }

    override fun viewOthers(list: List<FileEntity>, position: Int) {

    }

    override fun downloadIt(file: FileEntity) {
        DownloadService.launchWith(
            requireContext(),
            listOf(
                DownloadService.DownloadParam(
                    file.name,
                    file.rawURL,
                    file.hash,
                    DirManager.getDownloadDir(requireContext())
                )
            )
        ) {
            toast("正在取回文件：${file.name}")
        }
    }

    override fun deleteIt(file: FileEntity) {
        TextDialog(requireContext()).setTitle("删除提醒")
            .setText("是否确认删除文件：${file.name}? 删除后无法恢复！".replaceSpan(file.name) {
                ColorSpan(requireContext(), com.jason.theme.R.color.colorSecondary)
            }).onPositive("取消") {
                //啥也不做
            }
            .onNegative("确认删除") {
                val dialog = LoadDialog(requireContext()).setMessage("正在删除文件...")
                scopeDialog(dialog, cancelable = true) {
                    Get<String>("${Configure.hostURL}/delete") {
                        param("hash", viewModel.current())
                        param("fileHash", file.hash)
                    }.await().asJSONObject().also {
                        if (it.optInt("code") == 200) {
                            toast("文件删除成功！")
                            viewModel.refresh(isGoBack = false)
                        } else {
                            toast(it.getString("message"))
                        }
                    }
                }.catch {
                    toast(it.toMessage())
                }
            }.show()
    }

}