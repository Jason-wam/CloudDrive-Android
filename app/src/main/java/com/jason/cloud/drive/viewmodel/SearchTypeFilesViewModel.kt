package com.jason.cloud.drive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.scopeNetLife
import com.drake.net.Get
import com.drake.net.Net
import com.jason.cloud.drive.model.SearchRespondEntity
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.FileType
import com.jason.cloud.drive.utils.UrlBuilder
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.extension.asJSONObject

class SearchTypeFilesViewModel(application: Application) : AndroidViewModel(application) {
    private var page = 1
    private var searchType: FileType.Media? = null

    var onError = MutableLiveData<String>()
    var onSucceed = MutableLiveData<SearchRespondEntity>()

    fun refresh() {
        page = 1
        doSearch()
    }

    fun retry() {
        doSearch()
    }

    fun nextPage() {
        page += 1
        doSearch {
            if (it.not()) {
                page -= 1
            }
        }
    }

    fun search(type: FileType.Media) {
        page = 1
        searchType = type
        doSearch()
    }

    private fun doSearch(block: ((succeed: Boolean) -> Unit)? = null) {
        scopeNetLife {
            searchType ?: throw Exception("Type can't be null !")
            Get<String>(UrlBuilder(Configure.hostURL).path("/searchType").build()) {
                setGroup("search")
                param("type", searchType?.name)
                param("page", page)
                param("sort", Configure.SearchConfigure.sortModel.name)
                param("showHidden", Configure.SearchConfigure.showHidden)
                setHeader("password", Configure.password)
            }.await().asJSONObject().also { obj ->
                if (obj.has("code")) {
                    onError.postValue(obj.getString("message"))
                    block?.invoke(false)
                } else {
                    block?.invoke(true)
                    onSucceed.postValue(SearchRespondEntity.createFromJson(obj))
                }
            }
        }.catch {
            onError.postValue(it.toMessage())
            block?.invoke(false)
        }
    }

    fun cancel() {
        Net.cancelGroup("search")
    }
}