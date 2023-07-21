package com.jason.cloud.drive.base

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.gyf.immersionbar.ImmersionBar
import com.jason.cloud.drive.R


abstract class BaseBindActivity<VB : ViewDataBinding>(layoutResID: Int) :
    AppCompatActivity(layoutResID) {

    protected lateinit var binding: VB
    protected lateinit var context: AppCompatActivity

    override fun setContentView(layoutResId: Int) {
        val view = layoutInflater.inflate(layoutResId, null)
        setContentView(view)
        context = this
        binding = DataBindingUtil.bind(view)!!
        initView()
        initImmersionBar()
    }

    abstract fun initView()

    private fun initImmersionBar() {
        val darkFont = resources.getBoolean(R.bool.isStatusDarkFont)
        val immersionBar = ImmersionBar.with(this)
        immersionBar.statusBarColor(com.jason.theme.R.color.colorStatusBar)
        immersionBar.navigationBarColor(com.jason.theme.R.color.colorNavigationBar)
        immersionBar.autoDarkModeEnable(true, 0.2f)
        if (findViewById<View>(R.id.status_view) != null) {
            immersionBar.statusBarView(R.id.status_view)
        }
        immersionBar.init()
    }
}