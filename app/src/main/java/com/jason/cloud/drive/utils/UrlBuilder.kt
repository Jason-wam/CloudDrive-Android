package com.jason.cloud.drive.utils

import java.net.URLEncoder

/**
 * @Author: 进阶的面条
 * @Date: 2022-01-10 23:38
 * @Description: TODO
 */
class UrlBuilder(baseUrl: String) {
    private var builder = StringBuilder()
    private var charset = "utf-8"

    fun charset(charset: String): UrlBuilder {
        this.charset = charset
        return this
    }

    fun path(value: String): UrlBuilder {
        if (builder.endsWith("/")) {
            builder.append(value.removePrefix("/"))
        } else {
            builder.append("/").append(value.removePrefix("/"))
        }
        return this
    }

    fun param(key: String, value: Any): UrlBuilder {
        if (builder.contains("?")) {
            builder.append("&").append(key).append("=").append(value.toString().encode(charset))
        } else {
            builder.append("?").append(key).append("=").append(value.toString().encode(charset))
        }
        return this
    }

    private fun String.encode(charset: String): String {
        return URLEncoder.encode(this, charset)
    }

    fun build(): String {
        return builder.toString()
    }

    init {
        builder.append(baseUrl)
    }
}