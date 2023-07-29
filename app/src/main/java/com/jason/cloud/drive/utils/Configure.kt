package com.jason.cloud.drive.utils

import com.jason.cloud.drive.views.fragment.FilesFragmentViewModel
import com.jason.cloud.utils.MMKVStore

object Configure {
    var host: String = ""
        set(value) {
            field = value
            MMKVStore.with("Configure").put("host", value)
        }
        get() {
            return MMKVStore.with("Configure").getString("host")
        }

    var port: Int = 8820
        set(value) {
            field = value
            MMKVStore.with("Configure").put("port", value)
        }
        get() {
            return MMKVStore.with("Configure").getInt("port", 8820)
        }

    val hostURL: String
        get() {
            return "http://$host:$port"
        }

    object CloudFileConfigure {
        var sortModel: FilesFragmentViewModel.ListSort = FilesFragmentViewModel.ListSort.DATE_DESC
            set(value) {
                field = value
                MMKVStore.with("CloudFilesConfigure").put("sort", value.name)
            }
            get() {
                return MMKVStore.with("CloudFilesConfigure")
                    .getString("sort", FilesFragmentViewModel.ListSort.DATE_DESC.name).let {
                        FilesFragmentViewModel.ListSort.valueOf(it)
                    }
            }

        var showHidden: Boolean = true
            set(value) {
                field = value
                MMKVStore.with("CloudFilesConfigure").put("showHidden", value)
            }
            get() {
                return MMKVStore.with("CloudFilesConfigure")
                    .getBool("showHidden", true)
            }
    }

    object SearchConfigure {
        var sortModel: FilesFragmentViewModel.ListSort = FilesFragmentViewModel.ListSort.DATE_DESC
            set(value) {
                field = value
                MMKVStore.with("SearchFilesConfigure").put("sort", value.name)
            }
            get() {
                return MMKVStore.with("SearchFilesConfigure")
                    .getString("sort", FilesFragmentViewModel.ListSort.DATE_DESC.name).let {
                        FilesFragmentViewModel.ListSort.valueOf(it)
                    }
            }

        var showHidden: Boolean = true
            set(value) {
                field = value
                MMKVStore.with("SearchFilesConfigure").put("showHidden", value)
            }
            get() {
                return MMKVStore.with("SearchFilesConfigure")
                    .getBool("showHidden", true)
            }
    }
}