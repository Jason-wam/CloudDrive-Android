package com.jason.cloud.drive.utils

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
        var sortModel: ListSort = ListSort.DATE_DESC
            set(value) {
                field = value
                MMKVStore.with("CloudFilesConfigure").put("sortModel", value.name)
            }
            get() {
                return MMKVStore.with("CloudFilesConfigure")
                    .getString("sortModel", ListSort.DATE_DESC.name).let {
                        ListSort.valueOf(it)
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
        var sortModel: ListSort = ListSort.DATE_DESC
            set(value) {
                field = value
                MMKVStore.with("SearchFilesConfigure").put("sortModel", value.name)
            }
            get() {
                return MMKVStore.with("SearchFilesConfigure")
                    .getString("sortModel", ListSort.DATE_DESC.name).let {
                        ListSort.valueOf(it)
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