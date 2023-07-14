package com.jason.cloud.drive.views.fragment

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.net.Get
import com.drake.net.utils.scopeDialog
import com.flyjingfish.openimagelib.OpenImage
import com.flyjingfish.openimagelib.beans.OpenImageUrl
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.FileAdapter
import com.jason.cloud.drive.adapter.FilePathIndicatorAdapter
import com.jason.cloud.drive.base.BaseBindFragment
import com.jason.cloud.drive.contract.FileSelectContract
import com.jason.cloud.drive.databinding.FragmentFilesBinding
import com.jason.cloud.drive.extension.asJSONObject
import com.jason.cloud.drive.extension.cast
import com.jason.cloud.drive.extension.runOnMainAtFrontOfQueue
import com.jason.cloud.drive.extension.toMessage
import com.jason.cloud.drive.extension.toast
import com.jason.cloud.drive.interfaces.CallActivityInterface
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.service.UploadService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.MediaType
import com.jason.cloud.drive.viewmodel.FileViewModel
import com.jason.cloud.drive.views.dialog.FileDetailDialog
import com.jason.cloud.drive.views.dialog.LoadDialog
import com.jason.cloud.drive.views.dialog.ProgressDialog
import com.jason.cloud.drive.views.dialog.TextEditDialog
import com.jason.cloud.drive.views.widgets.decoration.FileListDecoration
import com.jason.cloud.drive.views.widgets.decoration.FilePathIndicatorDecoration
import com.jason.videocat.utils.extension.view.onMenuItemClickListener
import com.jason.videocat.utils.extension.view.setTitleFont

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
    private var uploadServiceConnection: ServiceConnection? = null

    private val viewModel by lazy {
        ViewModelProvider(this)[FileViewModel::class.java]
    }

    private val loadDialog by lazy {
        LoadDialog(requireContext())
    }

    private val adapter = FileAdapter().apply {
        addOnClickObserver { _, item, _ ->
            if (item.isDirectory) {
                binding.stateLayout.showLoading()
                viewModel.getList(item)
            } else {
                if (MediaType.isVideo(item.name)) {
                    FileDetailDialog().setFile(item).showNow(parentFragmentManager, "detail")
                } else if (MediaType.isImage(item.name)) {
                    viewImages(item)
                }
            }
        }
    }

    private val indicatorAdapter = FilePathIndicatorAdapter().apply {
        addOnBindViewObserver { _, item, viewHolder ->
            viewHolder.binding.tvPath.setOnClickListener {
                if (item.hash != viewModel.current()) {
                    binding.stateLayout.showLoading()
                    viewModel.getList(item.hash, item.name, item.path)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileSelectLauncher = registerForActivityResult(FileSelectContract()) { uri ->
            if (uri != null) {
                uploadURI(uri)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView(context: Context) {
        initToolBar()
        initRecyclerView()
        initViewModel()

        binding.stateLayout.showLoading()
        viewModel.refresh()

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.canGoBack()) {
                    binding.stateLayout.showLoading()
                    viewModel.goBack()
                } else {
                    activity?.cast<CallActivityInterface>()?.callOnBackPressed()
                }
            }
        })
    }

    private fun initToolBar() {
        binding.toolbar.setTitleFont("FONTS/剑豪体.ttf")
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
        binding.rvPathIndicator.addItemDecoration(FilePathIndicatorDecoration())

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
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initViewModel() {
        viewModel.onError.observe(this) {
            if (adapter.itemCount > 0) {
                toast(it)
            } else {
                binding.stateLayout.showError(it) {
                    binding.stateLayout.showLoading()
                    viewModel.refresh()
                }
            }
        }

        viewModel.onSucceed.observe(this) {
            indicatorAdapter.currentHash = it.hash
            indicatorAdapter.setData(viewModel.histories)
            indicatorAdapter.notifyDataSetChanged()
            binding.rvPathIndicator.scrollToPosition(indicatorAdapter.itemCount - 1)

            adapter.setData(it.list)
            adapter.notifyDataSetChanged()

            if (it.list.isNotEmpty()) {
                binding.stateLayout.showContent()
            } else {
                binding.stateLayout.showEmpty(R.string.state_view_nothing_here)
            }

            lastPosition[viewModel.current()]?.run {
                binding.rvData.layoutManager?.let { manager ->
                    manager as LinearLayoutManager
                    manager.scrollToPositionWithOffset(second, first)
                    lastPosition[viewModel.current()] = Pair(0, 0)
                }
            }
        }

        viewModel.onDeleteError.observe(this) {
            loadDialog.dismiss()
            toast(it)
        }

        viewModel.onDeleteSucceed.observe(this) {
            loadDialog.dismiss()
            toast("文件删除成功！")
            binding.stateLayout.showLoading()
            viewModel.refresh()
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
        val imageUrlList = adapter.itemData.filter { MediaType.isImage(it.name) }.let {
            ArrayList<OpenImageUrl>().apply {
                it.forEachIndexed { index, item ->
                    if (file.path == item.path) {
                        clickPosition = index
                    }
                    add(object : OpenImageUrl {
                        override fun getImageUrl(): String {
                            return "${Configure.hostURL}/file?hash=${item.hash}"
                        }

                        override fun getVideoUrl(): String {
                            return ""
                        }

                        override fun getCoverImageUrl(): String {
                            return "${Configure.hostURL}/file?thumbnail=${item.hash}"
                        }

                        override fun getType(): com.flyjingfish.openimagelib.enums.MediaType {
                            return com.flyjingfish.openimagelib.enums.MediaType.IMAGE
                        }
                    })
                }
            }
        }

        OpenImage.with(requireActivity()).setNoneClickView().setShowDownload()
            .setImageUrlList(imageUrlList).setClickPosition(clickPosition).show()
    }

    //##############################网络操作
    private fun refreshList() {
        binding.stateLayout.showLoading()
        viewModel.refresh()
    }

    private fun createFolder(name: String) {
        val dialog = LoadDialog(requireContext()).setMessage("正在创建文件夹...")
        scopeDialog(dialog, cancelable = true) {
            Get<String>("${Configure.hostURL}/newFolder") {
                param("hash", viewModel.current())
                param("name", name)
            }.await().asJSONObject().also {
                if (it.optInt("code") == 200) {
                    toast("文件夹创建成功！")
                    refreshList()
                } else {
                    toast(it.getString("message"))
                }
            }
        }.catch {
            toast(it.toMessage())
        }
    }

    private fun uploadURI(uri: Uri) {
        XXPermissions.with(requireContext()).permission(Permission.NOTIFICATION_SERVICE)
            .request { _, allGranted ->
                if (allGranted.not()) {
                    toast("请先赋予通知权限")
                } else {
                    val service = Intent(context, UploadService::class.java).apply {
                        putExtra("uri", uri)
                        putExtra("hash", viewModel.current())
                    }
                    val dialog = ProgressDialog(requireContext()).apply {
                        setOnCancelListener {
                            context.unbindService(uploadServiceConnection!!)
                            context.stopService(service)
                            uploadServiceConnection = null
                        }
                    }
                    uploadServiceConnection = createUploadServiceConnection(dialog)
                    context?.startService(service)
                    context?.bindService(
                        service,
                        uploadServiceConnection!!,
                        Context.BIND_AUTO_CREATE
                    )
                }
            }
    }

    private fun createUploadServiceConnection(dialog: ProgressDialog): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder is UploadService.UploadBinder) {
                    binder.onFileCheckListener = {
                        runOnMainAtFrontOfQueue {
                            dialog.setIsIndeterminate(true)
                            dialog.setMessage("正在校验文件...")
                            if (dialog.isShowing.not()) {
                                dialog.show()
                            }
                        }
                    }
                    binder.onProgressListener = { progress, speed ->
                        runOnMainAtFrontOfQueue {
                            dialog.update(progress)
                            dialog.updateSpeed(speed)
                            dialog.setMessage("正在上传文件：$progress%，请稍候..")
                            dialog.setIsIndeterminate(false)
                            if (dialog.isShowing.not()) {
                                dialog.show()
                            }
                        }
                    }
                    binder.onUploadDoneListener = {
                        runOnMainAtFrontOfQueue {
                            dialog.dismiss()
                            refreshList()
                        }
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {

            }
        }
    }
}