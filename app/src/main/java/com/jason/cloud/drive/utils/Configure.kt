package com.jason.cloud.drive.utils

import android.os.Build
import com.jason.cloud.utils.MMKVStore

object Configure {
    val hostURL: String
        get() {
            return "http://$host:$port"
        }

    var host: String
        set(value) {
            MMKVStore.with("Configure").put("host", value)
        }
        get() {
            return MMKVStore.with("Configure").getString("host")
        }

    var port: Int
        set(value) {
            MMKVStore.with("Configure").put("port", value)
        }
        get() {
            return MMKVStore.with("Configure").getInt("port", 8820)
        }

    var password: String
        set(value) {
            MMKVStore.with("Configure").put("password", value)
        }
        get() {
            return MMKVStore.with("Configure").getString("password")
        }

    val deviceName by lazy {
        (Build.BRAND + " - " + Build.MODEL).uppercase()
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

        var showHidden: Boolean = false
            set(value) {
                field = value
                MMKVStore.with("CloudFilesConfigure").put("showHidden", value)
            }
            get() {
                return MMKVStore.with("CloudFilesConfigure")
                    .getBool("showHidden", false)
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

        var showHidden: Boolean = false
            set(value) {
                field = value
                MMKVStore.with("SearchFilesConfigure").put("showHidden", value)
            }
            get() {
                return MMKVStore.with("SearchFilesConfigure")
                    .getBool("showHidden", false)
            }
    }
}