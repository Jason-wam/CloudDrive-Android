package com.jason.cloud.drive.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.DiskLruCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.drake.net.okhttp.trustSSLCertificate
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit

@GlideModule
class OkHttpModule : AppGlideModule() {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder().apply {
            trustSSLCertificate()
            readTimeout(2, TimeUnit.MINUTES)
            connectTimeout(2, TimeUnit.MINUTES)
            dispatcher(Dispatcher().apply {
                maxRequests = 200
                maxRequestsPerHost = 200
            })
        }.build()
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        //super.applyOptions(context, builder)
        val diskCacheSizeBytes = 1024 * 1024 * 1024L * 2
        val cacheDir = DirManager.getGlideDir(context)
        builder.setDiskCache(DiskLruCacheFactory(cacheDir.absolutePath, diskCacheSizeBytes))
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(client)
        )
    }
}