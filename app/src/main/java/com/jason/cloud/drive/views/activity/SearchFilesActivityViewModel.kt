package com.jason.cloud.drive.views.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.scopeNetLife
import com.drake.net.Get
import com.drake.net.Net
import com.jason.cloud.drive.model.SearchRespondEntity
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.UrlBuilder
import com.jason.cloud.extension.asJSONObject
import com.jason.cloud.extension.toMessage

class SearchFilesActivityViewModel(application: Application) : AndroidViewModel(application) {
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
        doSearch()
    }

    fun search(kw: String) {
        page = 1
        searchWords = kw
        doSearch()
    }

    private fun doSearch() {
        scopeNetLife {
            Get<String>(UrlBuilder(Configure.hostURL).path("/search").build()) {
                param("kw", searchWords)
                param("page", page)
                param("sort", Configure.SearchConfigure.sortModel.name)
                param("showHidden", Configure.SearchConfigure.showHidden)
                setGroup("search")
            }.await().asJSONObject().also { obj ->
                if (obj.has("code")) {
                    onError.postValue(obj.getString("message"))
                } else {
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