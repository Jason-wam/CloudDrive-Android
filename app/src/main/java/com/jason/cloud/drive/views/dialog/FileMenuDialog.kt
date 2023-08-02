package com.jason.cloud.drive.views.dialog

import android.view.View
import androidx.fragment.app.FragmentActivity
import com.drake.net.Get
import com.drake.net.utils.scopeDialog
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.ColorSpan
import com.flyjingfish.openimagelib.OpenImage
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutFileMenuDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.DirManager
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.openURL
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.activity.VideoPlayActivity
import com.jason.cloud.media3.model.Media3VideoItem
import java.io.Serializable

class FileMenuDialog(val parent: FragmentActivity) :
    BaseBindBottomSheetDialogFragment<LayoutFileMenuDialogBinding>(R.layout.layout_file_menu_dialog) {

    private var onFileDeleteListener: (() -> Unit)? = null

    fun setOnFileDeleteListener(listener: () -> Unit): FileMenuDialog {
        this.onFileDeleteListener = listener
        return this
    }

    fun setFile(list: List<FileEntity>, position: Int): FileMenuDialog {
        arguments?.putSerializable("list", list as Serializable)
        arguments?.putInt("position", position)
        return this
    }

    override fun initView(view: View) {
        super.initView(view)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.handler.isSelected = slideOffset != 0f
            }
        })

        val position = arguments?.getInt("position") ?: 0
        val list = arguments?.getSerializableListExtraEx<FileEntity>("list") ?: emptyList()
        if (list.isNotEmpty()) {
            val current = list[position]
            binding.tvTitle.text = current.name

            binding.btnOpen.setOnClickListener {
                if (FileType.isVideo(current.name)) {
                    parent.viewVideos(list, position)
                    dismiss()
                } else if (FileType.isImage(current.name)) {
                    parent.viewImages(list, position)
                    dismiss()
                } else if (FileType.isAudio(current.name)) {
                    parent.viewAudios(list, position)
                    dismiss()
                } else {
                    parent.viewOthers(list, position)
                    dismiss()
                }
            }

            binding.btnOtherApp.setOnClickListener {
                parent.openWithOtherApplication(list, position)
                dismiss()
            }

            binding.btnDownload.setOnClickListener {
                parent.downloadFile(current)
                dismiss()
            }

            binding.btnDetail.setOnClickListener {
                if (FileType.isVideo(current.name)) {
                    parent.viewVideoDetail(list, position)
                    dismiss()
                } else if (FileType.isAudio(current.name)) {
                    parent.viewAudioDetail(list, position)
                    dismiss()
                } else {

                }
            }

            binding.btnDelete.setOnClickListener {
                parent.deleteFile(current) {
                    onFileDeleteListener?.invoke()
                }
                dismiss()
            }

            binding.btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }
}

fun FragmentActivity.showFileMenu(list: List<FileEntity>, position: Int, onDelete: () -> Unit) {
    FileMenuDialog(this).setFile(list, position).setOnFileDeleteListener {
        onDelete.invoke()
    }.showNow(supportFragmentManager, "menu")
}

fun FragmentActivity.viewVideos(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val videos = list.filter { FileType.isVideo(it.name) }
    val videoIndex = videos.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    VideoPlayActivity.open(this, videos.map {
        Media3VideoItem.create(it.name, it.rawURL, true)
    }, videoIndex)
}

fun FragmentActivity.viewAudios(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val audioList = list.filter { FileType.isAudio(it.name) }
    val audioIndex = audioList.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    AudioPlayDialog().setData(audioList, audioIndex)
        .showNow(supportFragmentManager, "audio")
}

fun FragmentActivity.viewImages(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val images = list.filter { FileType.isImage(it.name) }
    val imageIndex = images.indexOfFirst { it.hash == hash }.coerceAtLeast(0)

    images.map { item ->
        item.toOpenImageUrl()
    }.also {
        OpenImage.with(this).setNoneClickView().setImageUrlList(it)
            .setClickPosition(imageIndex).show()
    }
}

fun FragmentActivity.viewOthers(list: List<FileEntity>, position: Int) {
    AttachFileDialog().setFile(list[position])
        .showNow(supportFragmentManager, "attach")
}

fun FragmentActivity.openWithOtherApplication(list: List<FileEntity>, position: Int) {
    val current = list[position]
    openURL(current.rawURL, current.mimeType())
}

fun FragmentActivity.viewVideoDetail(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val videos = list.filter { FileType.isVideo(it.name) }
    val videoIndex = videos.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    VideoDetailDialog().setFileList(videos, videoIndex)
        .showNow(supportFragmentManager, "detail")
}

fun FragmentActivity.viewAudioDetail(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val videos = list.filter { FileType.isAudio(it.name) }
    val videoIndex = videos.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    AudioDetailDialog().setFileList(videos, videoIndex)
        .showNow(supportFragmentManager, "detail")
}

fun FragmentActivity.downloadFile(file: FileEntity) {
    val dir = DirManager.getDownloadDir(this)
    val taskParam = DownloadService.DownloadParam(file.name, file.rawURL, file.hash, dir)
    DownloadService.launchWith(this, listOf(taskParam)) {
        toast("正在取回文件：${file.name}")
    }
}

fun FragmentActivity.deleteFile(file: FileEntity, deleted: (() -> Unit)? = null) {
    TextDialog(parent).setTitle("删除提醒")
        .setText("是否确认删除文件：${file.name}? 删除后无法恢复！".replaceSpan(file.name) {
            ColorSpan(this, com.jason.theme.R.color.colorSecondary)
        }).onPositive("取消") {
            //啥也不做
        }.onNegative("确认删除") {
            val dialog = LoadDialog(this).setMessage("正在删除文件...")
            scopeDialog(dialog, cancelable = true) {
                Get<String>("${Configure.hostURL}/delete") {
                    param("path", file.path)
                }.await().asJSONObject().also {
                    if (it.optInt("code") == 200) {
                        toast("文件删除成功！")
                        deleted?.invoke()
                    } else {
                        toast(it.getString("message"))
                    }
                }
            }.catch {
                toast(it.toMessage())
            }
        }.show()
}