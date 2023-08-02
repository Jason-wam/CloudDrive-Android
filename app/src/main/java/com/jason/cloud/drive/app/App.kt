package com.jason.cloud.drive.app

import android.app.Application
import com.drake.net.NetConfig
import com.drake.net.interceptor.LogRecordInterceptor
import com.drake.net.interfaces.NetErrorHandler
import com.drake.net.okhttp.setDebug
import com.drake.net.okhttp.setErrorHandler
import com.drake.net.okhttp.trustSSLCertificate
import com.jason.cloud.drive.database.TaskDatabase
import com.jason.cloud.drive.utils.Configure
import com.jason.cloud.drive.utils.DirManager
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.drive.views.widgets.SrlRefreshFooter
import com.jason.cloud.drive.views.widgets.SrlRefreshHeader
import com.jason.cloud.extension.GB
import com.jason.cloud.extension.toast
import com.jason.cloud.utils.MMKVStore
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import okhttp3.Cache
import java.net.Proxy
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MMKVStore.init(this)
        TaskDatabase.init(this)
        initNet()

        Configure.port = 8820
        Configure.host = "192.168.0.4"

        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, _ ->
            SrlRefreshHeader(context)
        }
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, _ ->
            SrlRefreshFooter(context)
        }
    }

    private fun initNet() {
        NetConfig.initialize(context = this) {
            val cacheDir = DirManager.getNetDir(this@App)
            cache(Cache(cacheDir, 2.GB))
            proxy(Proxy.NO_PROXY)
            trustSSLCertificate()

            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(30, TimeUnit.SECONDS)
            connectTimeout(30, TimeUnit.SECONDS)

            setDebug(true)
            addInterceptor(LogRecordInterceptor(true))
            setErrorHandler(object : NetErrorHandler {
                override fun onError(e: Throwable) {
                    e.stackTraceToString()
                    toast(e.toMessage())
                }
            })
        }
    }
}