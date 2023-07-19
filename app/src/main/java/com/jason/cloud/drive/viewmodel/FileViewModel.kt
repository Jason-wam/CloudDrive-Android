package com.jason.cloud.drive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.scopeNetLife
import com.drake.net.Get
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.utils.extension.asJSONObject
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.drive.model.FileListRespondEntity
import com.jason.cloud.drive.model.FileNavigationEntity
import com.jason.cloud.drive.utils.Configure

class FileViewModel(application: Application) : AndroidViewModel(application) {
    val histories = arrayListOf(FileNavigationEntity("Drive", "%root"))
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

    fun refresh(hash: String? = null, isGoBack: Boolean) {
        isLoading = true
        scopeNetLife {
            Get<String>("${Configure.hostURL}/list") {
                param("hash", hash ?: current())
            }.await().asJSONObject().also {
                if (it.has("code")) {
                    onError.postValue(it.getString("message"))
                } else {
                    val respond = FileListRespondEntity.createFromJson(it)
                    histories.clear()
                    histories.addAll(respond.navigation)
                    onSucceed.postValue(
                        FileListRespond(
                            isGoBack,
                            respond
                        )
                    )
                }
            }
        }.catch {
            onError.postValue(it.toMessage())
        }.finally {
            isLoading = false
        }
    }

}