package com.jason.cloud.extension

import android.os.Handler
import android.os.Looper

inline fun <T> T.runOnMain(crossinline block: T.() -> Unit): T {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block.invoke(this)
    } else {
        Handler(Looper.getMainLooper()).post {
            block.invoke(this)
        }
    }
    return this
}

inline fun <T> T.runOnMainDelay(delay: Long, crossinline block: T.() -> Unit): T {
    Handler(Looper.getMainLooper()).postDelayed({
        block.invoke(this)
    }, delay)
    return this
}

inline fun <T> T.runOnMainAtFrontOfQueue(crossinline block: T.() -> Unit): T {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        block.invoke(this)
    } else {
        Handler(Looper.getMainLooper()).postAtFrontOfQueue {
            block.invoke(this)
        }
    }
    return this
}