package com.jason.cloud.drive.views.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.net.Get
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.scopeNetLife
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.CloudFileAdapter
import com.jason.cloud.drive.adapter.CloudFilePathIndicatorAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.contract.SelectFilesContract
import com.jason.cloud.drive.contract.SelectFolderContract
import com.jason.cloud.drive.databinding.FragmentFileListBinding
import com.jason.cloud.drive.interfaces.CallFragment
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.service.UploadService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.drive.viewmodel.ListFilesViewModel
import com.jason.cloud.drive.views.dialog.LoadDialog
import com.jason.cloud.drive.views.dialog.TextEditDialog
import com.jason.cloud.drive.views.dialog.showFileMenu
import com.jason.cloud.drive.views.widgets.decoration.FileListDecoration
import com.jason.cloud.drive.views.widgets.decoration.FilePathIndicatorDecoration
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.getSerializableEx
import com.jason.cloud.extension.toast
import kotlinx.coroutines.delay

/**
 * 通用文件浏览
 */
class FileListFragment : BaseBindFragment<FragmentFileListBinding>(R.layout.fragment_file_list),
    CallFragment {
    companion object {
        @JvmStatic
        fun newInstance() = FileListFragment()


        @JvmStatic
        fun newInstance(file: FileEntity) = FileListFragment().apply {
            arguments = Bundle().apply {
                putSerializable("file", file)
            }
        }
    }

    private val lastPosition: HashMap<String, Pair<Int, Int>> = HashMap()
    private lateinit var fileSelectLauncher: ActivityResultLauncher<String>
    private lateinit var selectFolderLauncher: ActivityResultLauncher<Any?>

    private val viewModel by lazy {
        ViewModelProvider(this)[ListFilesViewModel::class.java]
    }

    private val adapter = CloudFileAdapter().apply {
        addOnClickObserver { position, item, _ ->
            if (item.isDirectory) {
                openFolder(item)
            } else {
                activity?.showFileMenu(itemData, position) {
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

    override fun callBackPressed(): Boolean {
        if (isVisible.not()) return true
        return if (viewModel.isLoading) false else {
            if (viewModel.canGoBack().not()) {
                true
            } else {
                binding.stateLayout.showLoading()
                viewModel.goBack()
                false
            }
        }
    }

    fun refresh() {
        val hash = arguments?.getString("hash").orEmpty()
        arguments?.remove("hash")
        if (hash.isNotBlank()) {
            binding.stateLayout.showLoading()
            viewModel.refresh(hash, isGoBack = false)
        }
    }

    fun refresh(isGoBack: Boolean = false) {
        binding.stateLayout.showLoading()
        viewModel.refresh(isGoBack = isGoBack)
    }

    private fun openFolder(item: FileEntity) {
        binding.stateLayout.showLoading()
        viewModel.getList(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileSelectLauncher = registerForActivityResult(SelectFilesContract()) { uriList ->
            if (uriList.isNotEmpty()) {
                UploadService.launchWith(requireContext(), viewModel.current(), uriList) {
                    toast("开始上传 ${uriList.size} 个文件")
                }
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
                        UploadService.launchWith(requireContext(), viewModel.current(), uriList) {
                            toast("开始上传 ${uriList.size} 个文件")
                        }
                    }
                }
            }
        }
    }

    private var appbarCallback: ((show: Boolean) -> Unit)? = null

    fun onAppbarCallback(call: (show: Boolean) -> Unit) {
        this.appbarCallback = call
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView(context: Context) {
        binding.rvPathIndicator.adapter = indicatorAdapter
        binding.rvPathIndicator.addItemDecoration(FilePathIndicatorDecoration())
        binding.indicatorBar.addOnOffsetChangedListener { _, verticalOffset ->
            appbarCallback?.invoke(verticalOffset != 0)
        }

        binding.rvData.adapter = adapter
        binding.rvData.addItemDecoration(FileListDecoration(requireContext()))
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

        viewModel.onError.observe(this) {
            binding.refreshLayout.finishRefresh(false)
            binding.refreshLayout.finishLoadMore(false)

            binding.stateLayout.showError(it) {
                binding.stateLayout.showLoading()
                viewModel.refresh(isGoBack = false, noneCache = true)
            }
        }

        viewModel.onSucceed.observe(this) {
            binding.fabUpload.show()

            binding.refreshLayout.finishRefresh(true)
            binding.refreshLayout.finishLoadMore(true)

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

            if (it.isGoBack) {
                lastPosition[viewModel.current()]?.run {
                    binding.rvData.layoutManager?.let { manager ->
                        manager as LinearLayoutManager
                        manager.scrollToPositionWithOffset(second, first)
                        lastPosition[viewModel.current()] = Pair(0, 0)
                    }
                }
            } else {
                val fileHash = arguments?.getString("fileHash").orEmpty()
                arguments?.remove("fileHash")
                val location = adapter.itemData.indexOfFirst { file -> file.hash == fileHash }
                if (location == -1) {
                    binding.rvData.scrollToPosition(0)
                } else {
                    binding.rvData.scrollToPosition(location)
                    animateView(location)
                }
            }
        }

        binding.refreshLayout.setOnRefreshListener {
            adapter.clear()
            adapter.notifyDataSetChanged()
            viewModel.refresh(isGoBack = false, noneCache = true)
        }

        binding.fabUpload.setOnClickListener {
            fileSelectLauncher.launch("*/*")
        }

        val file = arguments?.getSerializableEx("file", FileEntity::class.java)
        if (file != null) {
            openFolder(file)
            arguments?.clear()
        } else {
            val hash = arguments?.getString("hash").orEmpty()
            arguments?.clear()
            binding.stateLayout.showLoading()
            viewModel.refresh(hash, isGoBack = false)
        }
    }

    /**
     * 闪烁指定的ItemView
     */
    private fun animateView(location: Int) {
        scopeNetLife {
            delay(300)
            binding.rvData.findViewHolderForLayoutPosition(location)?.let { holder ->
                val animationView = holder.itemView.findViewById<View>(R.id.animation_view)
                if (animationView != null) {
                    animationView.isVisible = true
                    animationView.startAnimation(AlphaAnimation(0f, 0.2f).apply {
                        duration = 900
                        fillBefore = true
                        repeatCount = 3
                        interpolator = LinearInterpolator()
                        repeatMode = Animation.REVERSE
                    })
                    delay(1000)
                    animationView.isVisible = false
                }
            }
        }
    }

    fun createNewFolder() {
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
}