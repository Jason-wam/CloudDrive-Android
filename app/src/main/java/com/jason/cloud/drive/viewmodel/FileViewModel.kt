package com.jason.cloud.drive.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.scopeNetLife
import com.drake.net.Delete
import com.drake.net.Get
import com.drake.net.Post
import com.drake.net.Put
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import com.drake.net.utils.fileName
import com.drake.net.utils.toRequestBody
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.extension.asJSONObject
import com.jason.cloud.drive.extension.forEachObject
import com.jason.cloud.drive.extension.toMessage
import com.jason.cloud.drive.model.FileIndicatorEntity
import com.jason.cloud.drive.model.FileListRespondEntity
import com.jason.cloud.drive.utils.Configure

class FileViewModel(application: Application) : AndroidViewModel(application) {
    val histories = arrayListOf(FileIndicatorEntity("%root", "%root", "root"))
    val onError = MutableLiveData<String>()
    val onSucceed = MutableLiveData<FileListRespondEntity>()

    fun current(): String {
        return if (histories.isEmpty()) "%root" else histories.last().hash
    }

    fun getList(file: FileEntity) {
        if (file.hash.isBlank() || file.hash == "%root") {
            histories.clear()
        }
        val index = histories.indexOfFirst { it.hash == file.hash }
        if (index > 0) {
            histories.subList(index, histories.lastIndex + 1).clear()
        }
        histories.add(FileIndicatorEntity(file.hash, file.name, file.path))
        refresh()
    }

    fun getList(hash: String, name: String, path: String) {
        if (hash.isBlank() || hash == "%root") {
            histories.clear()
        }
        val index = histories.indexOfFirst { it.hash == hash }
        if (index > 0) {
            histories.subList(index, histories.lastIndex + 1).clear()
        }
        histories.add(FileIndicatorEntity(hash, name, path))
        refresh()
    }

    fun canGoBack(): Boolean {
        return histories.size > 1
    }

    fun goBack() {
        histories.removeAt(histories.lastIndex)
        refresh()
    }

    fun refresh() {
        scopeNetLife {
            Get<String>("${Configure.hostURL}/list") {
                param("hash", current())
            }.await().asJSONObject().also {
                if (it.has("code")) {
                    onError.postValue(it.getString("message"))
                } else {
                    onSucceed.postValue(
                        FileListRespondEntity(
                            it.getString("hash"),
                            it.getString("name"),
                            it.getString("path"),
                            ArrayList<FileEntity>().apply {
                                it.getJSONArray("list").forEachObject { obj ->
                                    add(
                                        FileEntity(
                                            obj.getString("name"),
                                            obj.getString("path"),
                                            obj.getString("hash"),
                                            obj.getLong("size"),
                                            obj.getLong("date"),
                                            obj.getBoolean("isFile"),
                                            obj.getBoolean("isDirectory"),
                                            obj.getInt("childCount"),
                                            obj.getBoolean("hasImage"),
                                        )
                                    )
                                }
                            }
                        )
                    )
                }
            }
        }.catch {
            onError.postValue(it.toMessage())
        }
    }

    val onCreateFolderError = MutableLiveData<String>()
    val onCreateFolderSucceed = MutableLiveData<Boolean>()

    fun createFolder(name: String) {
        scopeNetLife {
            Get<String>("${Configure.hostURL}/newFolder") {
                param("hash", current())
                param("name", name)
            }.await().asJSONObject().also {
                if (it.optInt("code") == 200) {
                    onCreateFolderSucceed.postValue(true)
                } else {
                    onCreateFolderError.postValue(it.getString("message"))
                }
            }
        }.catch {
            onCreateFolderError.postValue(it.toMessage())
        }
    }

    val onDeleteError = MutableLiveData<String>()
    val onDeleteSucceed = MutableLiveData<Boolean>()

    fun delete(hash: String) {
        scopeNetLife {
            Delete<String>("${Configure.hostURL}/delete") {
                param("hash", hash)
            }.await().asJSONObject().also {
                if (it.optInt("code") == 200) {
                    onDeleteSucceed.postValue(true)
                } else {
                    onDeleteError.postValue(it.getString("message"))
                }
            }
        }.catch {
            onDeleteError.postValue(it.toMessage())
        }
    }
}