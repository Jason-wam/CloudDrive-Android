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
import com.jason.cloud.drive.utils.FileUtil
import com.jason.cloud.drive.utils.MMKVStore
import com.jason.cloud.drive.utils.extension.GB
import com.jason.cloud.drive.utils.extension.toMessage
import com.jason.cloud.drive.utils.extension.toast
import com.jason.cloud.drive.views.widgets.SrlRefreshFooter
import com.jason.cloud.drive.views.widgets.SrlRefreshHeader
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

        Configure.host = "192.168.0.5"
        Configure.port = 8820

        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, _ ->
            SrlRefreshHeader(context)
        }
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, _ ->
            SrlRefreshFooter(context)
        }
    }

    private fun initNet() {
        val dir = FileUtil.getCacheDir(this, "Net")

        NetConfig.initialize(context = this) {
            cache(Cache(dir, 2.GB))
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