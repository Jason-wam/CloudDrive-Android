package com.jason.cloud.drive.views.dialog

import androidx.fragment.app.FragmentActivity
import com.drake.net.Get
import com.drake.net.Post
import com.drake.net.utils.scopeDialog
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.ColorSpan
import com.flyjingfish.openimagelib.OpenImage
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.service.DownloadService
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.DirManager
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.PositionStore
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.drive.views.activity.MediaCastActivity
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.openURL
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.activity.VideoPlayActivity
import com.jason.cloud.media3.model.Media3Item
import com.jason.theme.R
import java.io.File

/**
 * 显示文件菜单
 */
fun FragmentActivity.showFileMenu(
    list: List<FileEntity>,
    position: Int,
    onDelete: () -> Unit,
    onRenamed: () -> Unit
) {
    FileMenuDialog(this).setFile(list, position)
        .setOnFileDeleteListener { onDelete.invoke() }
        .setOnFileRenamedListener { onRenamed.invoke() }
        .showNow(supportFragmentManager, "menu")
}

/**
 * 显示目录菜单
 */
fun FragmentActivity.showFolderMenu(
    file: FileEntity,
    onDelete: () -> Unit,
    onRenamed: () -> Unit
) {
    FolderMenuDialog(this).setFile(file).setOnFileDeleteListener { onDelete.invoke() }
        .setOnFileRenamedListener { onRenamed.invoke() }
        .showNow(supportFragmentManager, "menu")
}

/**
 * 播放同目录的所有视频
 */
fun FragmentActivity.viewVideos(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val videos = list.filter { FileType.isVideo(it.name) }
    val videoIndex = videos.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    VideoPlayActivity.positionStore = PositionStore()
    VideoPlayActivity.open(this, videos.map {
        Media3Item.create(it.name, it.rawURL, true)
    }, videoIndex)
}

/**
 * 播放同目录的全部音频文件
 */
fun FragmentActivity.viewAudios(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val audioList = list.filter { FileType.isAudio(it.name) }
    val audioIndex = audioList.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    AudioPlayDialog().setData(audioList, audioIndex)
        .showNow(supportFragmentManager, "audio")
}

/**
 * 查看目录下的全部图片
 */
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

/**
 * 打开文件，下载后打开
 */
fun FragmentActivity.viewOthers(list: List<FileEntity>, position: Int) {
    AttachFileDialog().setFile(list[position])
        .showNow(supportFragmentManager, "attach")
}

/**
 * DLNA投送同目录的所有视频
 */
fun FragmentActivity.castMedia(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val videos = list.filter { FileType.isMedia(it.name) }
    val videoIndex = videos.indexOfFirst { it.hash == hash }.coerceAtLeast(0)

    MediaCastActivity.start(this, videos, videoIndex)
}

/**
 * 使用第三方软件打开文件地址
 */
fun FragmentActivity.openWithOtherApplication(list: List<FileEntity>, position: Int) {
    val current = list[position]
    openURL(current.rawURL, current.mimeType())
}

/**
 * 查看视频文件详细信息，支持单独播放
 */
fun FragmentActivity.viewVideoDetail(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val videos = list.filter { FileType.isVideo(it.name) }
    val videoIndex = videos.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    DetailVideoDialog().setFileList(videos, videoIndex)
        .showNow(supportFragmentManager, "detail")
}

/**
 * 查看音频文件的详细信息，支持单独播放
 */
fun FragmentActivity.viewAudioDetail(list: List<FileEntity>, position: Int) {
    val hash = list[position].hash
    val videos = list.filter { FileType.isAudio(it.name) }
    val videoIndex = videos.indexOfFirst { it.hash == hash }.coerceAtLeast(0)
    DetailAudioDialog().setFileList(videos, videoIndex)
        .showNow(supportFragmentManager, "detail")
}

/**
 * 查看其他文件的详细信息，不支持打开
 */
fun FragmentActivity.viewOtherDetail(file: FileEntity) {
    DetailOtherDialog(this).setFile(file).showNow(supportFragmentManager, "detail")
}

/**
 * 下载单个文件
 */
fun FragmentActivity.downloadFile(file: FileEntity) {
    val dir = DirManager.getDownloadDir(this)
    val taskParam = DownloadService.DownloadParam(file.name, file.rawURL, file.hash, dir)
    DownloadService.launchWith(this, listOf(taskParam)) {
        toast("正在取回文件：${file.name}")
    }
}

/**
 * 下载选中的文件列表到指定文件夹
 */
fun FragmentActivity.downloadFiles(folderName: String, fileList: List<FileEntity>) {
    val dir = DirManager.getDownloadDir(this)
    val targetDir = File(dir, folderName)
    DownloadService.launchWith(this, fileList.map {
        DownloadService.DownloadParam(it.name, it.rawURL, it.hash, targetDir)
    }) {
        toast("正在取回 ${fileList.size} 个文件")
    }
}

/**
 * 显示创建文件夹对话框
 */
fun FragmentActivity.showCreateFolderDialog(
    targetHash: String,
    folderCreated: (() -> Unit)? = null
) {
    TextEditDialog(this).apply {
        setTitle("新建文件夹")
        setHintText("请输入文件夹名称...")
        onNegative("取消")
        onPositive {
            if (it.isNullOrBlank()) {
                toast("请输入文件夹名称！")
                false
            } else {
                createFolder(targetHash, it.trim()) {
                    folderCreated?.invoke()
                }
                true
            }
        }
        show()
    }
}

/**
 * 创建文件夹
 */
fun FragmentActivity.createFolder(
    targetHash: String,
    name: String,
    folderCreated: (() -> Unit)? = null
) {
    val dialog = LoadDialog(this).setMessage("正在创建文件夹...")
    scopeDialog(dialog, cancelable = true) {
        Get<String>("${Configure.hostURL}/createFolder") {
            param("hash", targetHash)
            param("name", name)
        }.await().asJSONObject().also {
            if (it.optInt("code") == 200) {
                folderCreated?.invoke()
                toast("文件夹创建成功！")
            } else {
                toast(it.getString("message"))
            }
        }
    }.catch {
        toast(it.toMessage())
    }
}

/**
 * 显示重命名文件对话框
 */
fun FragmentActivity.showRenameDialog(file: FileEntity, renamed: (() -> Unit)? = null) {
    TextEditDialog(this)
        .setTitle("重命名文件")
        .setText(file.name.substringBefore(".", file.name))
        .setHintText(file.name.substringBefore(".", file.name))
        .onNegative("取消")
        .onPositive("立即修改") {
            if (it.isNullOrBlank()) {
                return@onPositive false
            } else {
                renameFile(file, it.trim(), renamed)
                return@onPositive true
            }
        }
        .show()
}

/**
 * 重命名文件
 */
fun FragmentActivity.renameFile(file: FileEntity, newName: String, renamed: (() -> Unit)? = null) {
    val dialog = LoadDialog(this).setMessage("正在重命名文件...")
    scopeDialog(dialog, cancelable = true) {
        Post<String>("${Configure.hostURL}/rename") {
            param("path", file.path)
            param("newName", newName)
        }.await().asJSONObject().also {
            if (it.optInt("code") == 200) {
                toast("重命名文件成功！")
                renamed?.invoke()
            } else {
                toast(it.getString("message"))
            }
        }
    }
}

/**
 * 显示文件删除提醒
 */
fun FragmentActivity.showDeleteDialog(file: FileEntity, deleted: (() -> Unit)? = null) {
    val text = "是否确认删除文件：${file.name}? 删除后无法恢复！"
    TextDialog(this).setTitle("删除文件")
        .setText(text.replaceSpan(file.name) {
            ColorSpan(this, R.color.colorSecondary)
        }).onPositive("取消") {
            //啥也不做
        }.onNegative("确认删除") {
            deleteFile(file, deleted)
        }.show()
}

/**
 * 显示删除文件夹对话框
 */
fun FragmentActivity.showDeleteFolderDialog(file: FileEntity, deleted: (() -> Unit)? = null) {
    val text = "是否确认删除文件夹：${file.name}? 此操作将删除文件夹中的全部文件！！删除后无法恢复！"
    TextDialog(this).setTitle("删除文件")
        .setText(text.replaceSpan(file.name) {
            ColorSpan(this, R.color.colorSecondary)
        }).onPositive("取消") {
            //啥也不做
        }.onNegative("确认删除") {
            deleteFile(file, deleted)
        }.show()
}

/**
 * 删除文件或文件夹
 */
fun FragmentActivity.deleteFile(file: FileEntity, deleted: (() -> Unit)? = null) {
    val dialog = LoadDialog(this).setMessage("正在删除文件...")
    scopeDialog(dialog, cancelable = true) {
        Post<String>("${Configure.hostURL}/delete") {
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
}