package com.jason.cloud.drive.utils

import com.jason.cloud.drive.viewmodel.FileViewModel
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

    var sortModel: FileViewModel.ListSort = FileViewModel.ListSort.DATE_DESC
        set(value) {
            field = value
            MMKVStore.with("Configure").put("sort", value.name)
        }
        get() {
            return MMKVStore.with("Configure")
                .getString("sort", FileViewModel.ListSort.DATE_DESC.name).let {
                    FileViewModel.ListSort.valueOf(it)
                }
        }

    var showHidden: Boolean = true
        set(value) {
            field = value
            MMKVStore.with("Configure").put("showHidden", value)
        }
        get() {
            return MMKVStore.with("Configure")
                .getBool("showHidden", true)
        }
}