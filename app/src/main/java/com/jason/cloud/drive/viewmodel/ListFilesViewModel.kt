package com.jason.cloud.drive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.scopeNetLife
import com.drake.net.Get
import com.drake.net.cache.CacheMode
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.model.FileListRespondEntity
import com.jason.cloud.drive.model.FileNavigationEntity
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.UrlBuilder
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.extension.asJSONObject

class ListFilesViewModel(application: Application) : AndroidViewModel(application) {
    private val histories = arrayListOf(FileNavigationEntity("Drive", "%root"))
    val onError = MutableLiveData<String>()
    val onSucceed = MutableLiveData<FileListRespond>()
    var isLoading = false

    class FileListRespond(val isGoBack: Boolean, val respond: FileListRespondEntity)

    fun current(): String {
        return if (histories.isEmpty()) "%root" else histories.last().hash
    }

    fun getList(file: FileEntity) {
        refresh(file.hash, false)
    }

    fun getList(hash: String) {
        if (hash.isBlank() || hash == "%root") {
            histories.clear()
        }
        refresh(hash, false)
    }

    fun canGoBack(): Boolean {
        return histories.size > 1
    }

    fun goBack() {
        refresh(histories[histories.lastIndex - 1].hash, true)
    }

    /**
     * 如果目标目录Hash为空则刷新当前目录，否则枚举指定目录文件
     */
    fun refresh(hash: String? = null, isGoBack: Boolean, noneCache: Boolean = false) {
        isLoading = true
        scopeNetLife {
            val url = UrlBuilder(Configure.hostURL).path("/list").build()
            Get<String>(url) {
                param("hash", hash ?: current())
                param("sort", Configure.CloudFileConfigure.sortModel.name)
                param("showHidden", Configure.CloudFileConfigure.showHidden)
                if (noneCache) {//强制刷新，不读缓存
                    setCacheMode(CacheMode.WRITE)
                } else {
                    if (isGoBack) { //如果是返回则读取缓存
                        setCacheMode(CacheMode.READ_THEN_REQUEST)
                    } else {
                        setCacheMode(CacheMode.REQUEST_THEN_READ)
                    }
                }
            }.await().asJSONObject().also {
                if (it.has("code")) {
                    onError.postValue(it.getString("message"))
                } else {
                    val respond = FileListRespondEntity.createFromJson(it)
                    histories.clear()
                    histories.addAll(respond.navigation)
                    onSucceed.postValue(FileListRespond(isGoBack, respond))
                }
            }
        }.catch {
            onError.postValue(it.toMessage())
        }.finally {
            isLoading = false
        }
    }
}