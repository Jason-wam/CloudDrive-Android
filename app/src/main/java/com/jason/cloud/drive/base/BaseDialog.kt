package com.jason.cloud.drive.base

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import com.jason.cloud.drive.R

@SuppressLint("ResourceType")
abstract class BaseDialog(context: Context) : Dialog(context, R.style.DialogStyle) {
    private val onShowListeners = arrayListOf<DialogInterface.OnShowListener>()
    private val onDismissListeners = arrayListOf<DialogInterface.OnDismissListener>()

    init {
        this.setOnShowListener {
            for (listener in onShowListeners) {
                listener.onShow(it)
            }
        }
        this.setOnDismissListener {
            for (listener in onDismissListeners) {
                listener.onDismiss(it)
            }
        }
    }

    fun addOnShowListener(listener: DialogInterface.OnShowListener): BaseDialog {
        this.onShowListeners.add(listener)
        return this
    }

    fun addOnDismissListener(listener: DialogInterface.OnDismissListener): BaseDialog {
        this.onDismissListeners.add(listener)
        return this
    }

    fun removeOnDismissListener(listener: DialogInterface.OnDismissListener): BaseDialog {
        this.onDismissListeners.remove(listener)
        return this
    }

    fun clearOnDismissListener(): BaseDialog {
        this.onDismissListeners.clear()
        return this
    }

    fun removeOnShowListener(listener: DialogInterface.OnShowListener): BaseDialog {
        this.onShowListeners.remove(listener)
        return this
    }

    fun clearOnShowListener(): BaseDialog {
        this.onShowListeners.clear()
        return this
    }
}