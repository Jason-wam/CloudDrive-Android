package com.jason.cloud.drive.views.fragment

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.net.Delete
import com.drake.net.Get
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.scopeNetLife
import com.flyjingfish.openimagelib.OpenImage
import com.flyjingfish.openimagelib.beans.OpenImageUrl
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.CloudFileAdapter
import com.jason.cloud.drive.adapter.CloudFilePathIndicatorAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.contract.FilesSelectContract
import com.jason.cloud.drive.databinding.FragmentFilesBinding
import com.jason.cloud.drive.extension.asJSONObject
import com.jason.cloud.drive.extension.cast
import com.jason.cloud.drive.extension.toMessage
import com.jason.cloud.drive.extension.toast
import com.jason.cloud.drive.interfaces.CallActivityInterface
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.model.toOpenImageUrl
import com.jason.cloud.drive.model.toOpenImageUrlList
import com.jason.cloud.drive.service.FileUploadService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.MediaType
import com.jason.cloud.drive.utils.uploader.Uploader
import com.jason.cloud.drive.viewmodel.FileViewModel
import com.jason.cloud.drive.views.dialog.FileMenuDialog
import com.jason.cloud.drive.views.dialog.VideoDetailDialog
import com.jason.cloud.drive.views.dialog.LoadDialog
import com.jason.cloud.drive.views.dialog.TextEditDialog
import com.jason.cloud.drive.views.dialog.UploadDialog
import com.jason.cloud.drive.views.widgets.decoration.CloudFileListDecoration
import com.jason.cloud.drive.views.widgets.decoration.CloudFilePathIndicatorDecoration
import com.jason.videocat.utils.extension.view.onMenuItemClickListener
import com.jason.videocat.utils.extension.view.setTitleFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesFragment : BaseBindFragment<FragmentFilesBinding>(R.layout.fragment_files) {
    companion object {
        @JvmStatic
        fun newInstance() = FilesFragment()
    }

    /**
     * 记录RecyclerView当前位置
     */
    private val lastPosition: HashMap<String, Pair<Int, Int>> = HashMap()

    private lateinit var fileSelectLauncher: ActivityResultLauncher<String>

    private val viewModel by lazy {
        ViewModelProvider(this)[FileViewModel::class.java]
    }

    private val adapter = CloudFileAdapter().apply {
        addOnClickObserver { _, item, _ ->
            if (item.isDirectory) {
                binding.stateLayout.showLoading()
                viewModel.getList(item)
            } else {
                if (MediaType.isVideo(item.name)) {
                    VideoDetailDialog().setFile(item).showNow(parentFragmentManager, "detail")
                } else if (MediaType.isImage(item.name)) {
                    viewImages(item)
                } else {
                    FileMenuDialog().setFile(item).showNow(childFragmentManager, "menu")
                }
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
        fileSelectLauncher = registerForActivityResult(FilesSelectContract()) { uriList ->
            if (uriList.isNotEmpty()) {
                println(uriList.first().toString())
//                UploadDialog(requireContext()).setData(uriList.first(), viewModel.current())
//                    .startNow()
//                FileUploadService.upload(
//                    requireContext(),
//                    viewModel.current(),
//                    uriList
//                )


                lifecycleScope.launch {
                    val hash = "25a7560284295162399e2618bd398170"
                    val uploader = Uploader().setData(uriList.first(), hash)
                    uploader.start()
                    while (true) {
                        delay(1000)
                        Log.e("Uploader", uploader.status.toString())
                        if (uploader.isDone()) {
                            break
                        }
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
        }

        binding.stateLayout.showLoading()
        viewModel.refresh(isGoBack = false)

        activity?.onBackPressedDispatcher?.addCallback(
            this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.canGoBack()) {
                        binding.stateLayout.showLoading()
                        viewModel.goBack()
                    } else {
                        activity?.cast<CallActivityInterface>()?.callOnBackPressed()
                    }
                }
            }
        )
    }

    private fun initToolBar() {
        binding.toolbar.setTitleFont("fonts/剑豪体.ttf")
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
        TextEditDialog(requireContext()).apply {
            setTitle("新建文件夹")
            setHintText("请输入文件夹名称...")
            onNegative("取消")
            onPositive {
                if (it.isNullOrBlank()) {
                    toast("请输入文件夹名称！")
                    false
                } else {
                    createFolder(it.trim())
                    true
                }
            }
            show()
        }
    }

    private fun viewImages(file: FileEntity) {
        var clickPosition = 0
        val imageUrlList = adapter.itemData.filter {
            MediaType.isImage(it.name)
        }.mapIndexed { index, item ->
            if (file.path == item.path) {
                clickPosition = index
            }
            item.toOpenImageUrl()
        }

        OpenImage.with(requireActivity()).setNoneClickView()
            .setImageUrlList(imageUrlList).setClickPosition(clickPosition).show()
    }

    //##############################网络操作

    private fun createFolder(name: String) {
        val dialog = LoadDialog(requireContext()).setMessage("正在创建文件夹...")
        scopeDialog(dialog, cancelable = true) {
            Get<String>("${Configure.hostURL}/newFolder") {
                param("hash", viewModel.current())
                param("name", name)
            }.await().asJSONObject().also {
                if (it.optInt("code") == 200) {
                    toast("文件夹创建成功！")
                } else {
                    toast(it.getString("message"))
                }
            }
        }.catch {
            toast(it.toMessage())
        }
    }

    private fun delete(hash: String) {
        scopeNetLife {
            Delete<String>("${Configure.hostURL}/delete") {
                param("hash", hash)
            }.await().asJSONObject().also {
                if (it.optInt("code") == 200) {
                    toast("文件删除成功！")
                } else {
                    toast(it.getString("message"))
                }
            }
        }.catch {
            toast(it.toMessage())
        }
    }

}