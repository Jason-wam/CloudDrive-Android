package com.jason.cloud.drive.utils

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
}