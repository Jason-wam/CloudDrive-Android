package com.jason.cloud.drive.base

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


abstract class BaseBottomSheetDialogFragment(@LayoutRes open val layoutId: Int) :
    BottomSheetDialogFragment() {
    protected lateinit var behavior: BottomSheetBehavior<FrameLayout>
    protected val onShowListeners = arrayListOf<DialogInterface.OnShowListener>()
    protected val onDismissListeners = arrayListOf<DialogInterface.OnDismissListener>()

    init {
        arguments = Bundle()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(
            requireContext(),
            com.jason.theme.R.style.Theme_BottomSheetDialog
        ).also {
            behavior = it.behavior
            it.setOnShowListener { dialog ->
                onShowListeners.forEach { listener ->
                    listener.onShow(dialog)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //解决导航栏灰色 <item name="android:windowIsFloating">false</item>//
        //ImmersionBar.with(this).navigationBarColor(R.color.colorDialogBackground).init()
        try {
            initView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }

//        behavior.state = BottomSheetBehavior.STATE_EXPANDED
//        val darkFont = resources.getBoolean(R.bool.isStatusDarkFont)
//        val immersionBar = ImmersionBar.with(this,true)
//        immersionBar.statusBarColor(R.color.colorStatusBar)
//        immersionBar.navigationBarColor(R.color.colorNavigationBar)
//        immersionBar.autoDarkModeEnable(darkFont, 0.2f)
//        immersionBar.init()
    }

    protected open fun initView(view: View) {

    }

    fun setCanceledOnTouchOutside(cancelable: Boolean) {
        this.dialog?.setCanceledOnTouchOutside(cancelable)
    }

    fun addOnShowListener(listener: DialogInterface.OnShowListener): BottomSheetDialogFragment {
        this.onShowListeners.add(listener)
        return this
    }

    fun addOnDismissListener(listener: DialogInterface.OnDismissListener): BottomSheetDialogFragment {
        this.onDismissListeners.add(listener)
        return this
    }

    fun removeOnDismissListener(listener: DialogInterface.OnDismissListener): BottomSheetDialogFragment {
        this.onDismissListeners.remove(listener)
        return this
    }

    fun clearOnDismissListener(): BottomSheetDialogFragment {
        this.onDismissListeners.clear()
        return this
    }

    fun removeOnShowListener(listener: DialogInterface.OnShowListener): BottomSheetDialogFragment {
        this.onShowListeners.remove(listener)
        return this
    }

    fun clearOnShowListener(): BottomSheetDialogFragment {
        this.onShowListeners.clear()
        return this
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        for (listener in onDismissListeners) {
            listener.onDismiss(dialog)
        }
    }
}