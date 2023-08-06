package com.jason.cloud.drive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.scopeNetLife
import com.drake.net.Get
import com.drake.net.Net
import com.drake.net.cache.CacheMode
import com.jason.cloud.drive.model.SearchRespondEntity
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.UrlBuilder
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.toMessage

class SearchFilesViewModel(application: Application) : AndroidViewModel(application) {
    private var page = 1
    private var searchWords = ""

    var onError = MutableLiveData<String>()
    var onSucceed = MutableLiveData<SearchRespondEntity>()

    fun refresh(noneCache: Boolean = true) {
        page = 1
        doSearch(noneCache)
    }

    fun retry() {
        doSearch(true)
    }

    fun nextPage() {
        doSearch {
            page += 1
        }
    }

    fun search(kw: String) {
        page = 1
        searchWords = kw
        doSearch()
    }

    private fun doSearch(noneCache: Boolean = false, block: (() -> Unit)? = null) {
        scopeNetLife {
            Get<String>(UrlBuilder(Configure.hostURL).path("/search").build()) {
                setGroup("search")
                param("kw", searchWords)
                param("page", page)
                param("sort", Configure.SearchConfigure.sortModel.name)
                param("showHidden", Configure.SearchConfigure.showHidden)
                if (noneCache) {
                    setCacheMode(CacheMode.WRITE)
                } else {
                    setCacheMode(CacheMode.READ_THEN_REQUEST)
                }
            }.await().asJSONObject().also { obj ->
                if (obj.has("code")) {
                    onError.postValue(obj.getString("message"))
                } else {
                    block?.invoke()
                    onSucceed.postValue(SearchRespondEntity.createFromJson(obj))
                }
            }
        }.catch {
            onError.postValue(it.toMessage())
        }
    }

    fun cancel() {
        Net.cancelGroup("search")
    }
}