package com.jason.cloud.drive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.scopeNetLife
import com.drake.net.Get
import com.drake.net.Net
import com.jason.cloud.drive.model.SearchRespondEntity
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.UrlBuilder
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.extension.asJSONObject

class SearchFilesViewModel(application: Application) : AndroidViewModel(application) {
    private var page = 1
    private var searchWords = ""

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

    fun search(kw: String) {
        page = 1
        searchWords = kw
        doSearch()
    }

    private fun doSearch(block: ((succeed: Boolean) -> Unit)? = null) {
        scopeNetLife {
            Get<String>(UrlBuilder(Configure.host).path("/search").build()) {
                setGroup("search")
                param("kw", searchWords)
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
            block?.invoke(false)
            onError.postValue(it.toMessage())
        }
    }

    fun cancel() {
        Net.cancelGroup("search")
    }
}