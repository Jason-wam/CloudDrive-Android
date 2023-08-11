package com.jason.cloud.media3.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.utils.Media3Configure.cachePoolDir
import com.jason.cloud.media3.utils.Media3Configure.cachePoolSize
import okhttp3.OkHttpClient
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@UnstableApi
class Media3SourceHelper private constructor(private val applicationContext: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: Media3SourceHelper? = null
        fun newInstance(applicationContext: Context): Media3SourceHelper {
            if (instance == null) {
                instance = Media3SourceHelper(applicationContext)
            }
            return instance!!
        }
    }

    private val mCache: Cache by lazy {
        newCache()
    }

    private val dataSourceFactory: DataSource.Factory by lazy {
        DefaultDataSource.Factory(applicationContext, httpDataSourceFactory)
    }

    private val httpDataSourceFactory: OkHttpDataSource.Factory by lazy {
        val builder = OkHttpClient.Builder()
        builder.followRedirects(true)
        builder.followSslRedirects(true)
        builder.hostnameVerifier { _, _ -> true }
        builder.trustSSLCertificate()
        val client: OkHttpClient = builder.build()
        OkHttpDataSource.Factory { request ->
            Log.i("DataSourceFactory", "newCall: $request")
            client.newCall(request)
        }
    }

    private val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        CacheDataSource.Factory().setCache(mCache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun OkHttpClient.Builder.trustSSLCertificate() {
        try {
            val trustManager = @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            sslSocketFactory(sslContext.socketFactory, trustManager)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMediaSource(
        uri: String,
        isCache: Boolean = false,
        headers: Map<String, String>? = null
    ): MediaSource {
        return getMediaSource(uri, headers, isCache)
    }

    fun getMediaSource(
        uri: String,
        headers: Map<String, String>?,
        isCache: Boolean
    ): MediaSource {
        val contentUri = Uri.parse(uri)
        if ("rtmp" == contentUri.scheme) {
            return ProgressiveMediaSource.Factory(RtmpDataSource.Factory())
                .createMediaSource(MediaItem.fromUri(contentUri))
        }

        if ("rtsp" == contentUri.scheme) {
            return RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(contentUri))
        }

        var cacheEnabled = isCache
        if (uri.startsWith("http://localhost", true)) {
            cacheEnabled = false
        }
        if (uri.startsWith("http://127.0.0.1", true)) {
            cacheEnabled = false
        }

        val factory = if (cacheEnabled) {
            cacheDataSourceFactory
        } else {
            dataSourceFactory
        }

        setHeaders(headers)

        return when (inferContentType(uri)) {
            C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(contentUri))

            C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(contentUri))

            C.CONTENT_TYPE_OTHER -> ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(contentUri))

            else -> ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(contentUri))
        }
    }

    fun getMediaSource(item: Media3Item): MediaSource {
        return getMediaSource(item, null, item.cacheEnabled)
    }

    fun getMediaSource(item: Media3Item, isCache: Boolean): MediaSource {
        return getMediaSource(item, item.headers, isCache)
    }

    fun getMediaSource(itemList: List<Media3Item>): ConcatenatingMediaSource {
        val source = ConcatenatingMediaSource()
        for (i in itemList.indices) {
            val item = itemList[i]
            source.addMediaSource(getMediaSource(item, item.headers, item.cacheEnabled))
        }
        return source
    }

    fun getMediaSource(
        itemList: List<Media3Item>,
        headers: Map<String, String>? = null,
        isCache: Boolean
    ): ConcatenatingMediaSource {
        val source = ConcatenatingMediaSource()
        for (i in itemList.indices) {
            val item = itemList[i]
            source.addMediaSource(getMediaSource(item, headers, isCache))
        }
        return source
    }

    fun getMediaSource(itemList: List<Media3Item>, isCache: Boolean): ConcatenatingMediaSource {
        val source = ConcatenatingMediaSource()
        for (i in itemList.indices) {
            source.addMediaSource(getMediaSource(itemList[i], isCache))
        }
        return source
    }

    fun getMediaSource(
        item: Media3Item,
        headers: Map<String, String>? = null,
        isCache: Boolean
    ): MediaSource {
        val contentUri = Uri.parse(item.url)
        if ("rtmp" == contentUri.scheme) {
            return ProgressiveMediaSource.Factory(RtmpDataSource.Factory())
                .createMediaSource(createMediaItem(item))
        }

        if ("rtsp" == contentUri.scheme) {
            return RtspMediaSource.Factory().createMediaSource(createMediaItem(item))
        }

        var cacheEnabled = isCache
        if (item.url.startsWith("http://localhost", true)) {
            cacheEnabled = false
        }
        if (item.url.startsWith("http://127.0.0.1", true)) {
            cacheEnabled = false
        }

        val factory = if (cacheEnabled) {
            cacheDataSourceFactory
        } else {
            dataSourceFactory
        }

        setHeaders(headers)

        return when (inferContentType(item.url)) {
            C.CONTENT_TYPE_DASH -> DashMediaSource.Factory(factory)
                .createMediaSource(createMediaItem(item))

            C.CONTENT_TYPE_HLS -> HlsMediaSource.Factory(factory)
                .createMediaSource(createMediaItem(item))

            C.CONTENT_TYPE_OTHER -> ProgressiveMediaSource.Factory(factory)
                .createMediaSource(createMediaItem(item))

            else -> ProgressiveMediaSource.Factory(factory).createMediaSource(createMediaItem(item))
        }
    }

    private fun createMediaItem(item: Media3Item): MediaItem {
        return MediaItem.Builder().setTag(item).setUri(item.url).build()
    }

    private fun inferContentType(fileName: String): Int {
        return if (fileName.contains(".mpd", true)) {
            C.CONTENT_TYPE_DASH
        } else if (fileName.contains(".m3u8", true)) {
            C.CONTENT_TYPE_HLS
        } else {
            C.CONTENT_TYPE_OTHER
        }
    }


    //1k = 1024 b
    //1M = 1024 * 1k
    //1G = 1024 * 1M
    private fun newCache(): Cache {
        var cacheDir = cachePoolDir
        if (cacheDir == null) {
            cacheDir = File(applicationContext.externalCacheDir, "media3-cache")
        }
        return SimpleCache(
            cacheDir,  //缓存目录
            LeastRecentlyUsedCacheEvictor(cachePoolSize),  //缓存大小，默认512M，使用LRU算法实现
            StandaloneDatabaseProvider(applicationContext)
        )
    }


    private fun setHeaders(headers: Map<String, String>?) {
        headers ?: return
        //如果发现用户通过header传递了UA，则强行将HttpDataSourceFactory里面的userAgent字段替换成用户的
        val value = headers["User-Agent"]
        if (value?.isNotBlank() == true) {
            httpDataSourceFactory.setUserAgent(value)
        }
        httpDataSourceFactory.setDefaultRequestProperties(headers)
    }
}