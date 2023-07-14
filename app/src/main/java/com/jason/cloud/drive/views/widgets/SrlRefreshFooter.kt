package com.jason.cloud.drive.views.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.jason.cloud.drive.R
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle

@SuppressLint("RestrictedApi")
class SrlRefreshFooter(context: Context?, attributeSet: AttributeSet? = null) : LinearLayout(context, attributeSet), RefreshFooter {
    private var mNoMoreData = false
    private val tvStatus: TextView
    
    init {
        LayoutInflater.from(context).inflate(R.layout.layout_refresh_footer, this, true)
        this.gravity = Gravity.CENTER
        this.tvStatus = findViewById(R.id.tvLoad)
    }
    
    override fun getView(): View {
        return this //真实的视图就是自己，不能返回null
    }
    
    override fun getSpinnerStyle(): SpinnerStyle {
        return SpinnerStyle.Translate //指定为平移，不能null
    }
    
    override fun onFinish(layout: RefreshLayout, success: Boolean): Int {
        if (mNoMoreData) {
            tvStatus.text = "没有更多数据了"
            return 0
        }
        if (success) {
            tvStatus.text = "加载完毕"
        } else {
            tvStatus.text = "加载失败,请重试"
        }
        return 0 //延迟500毫秒之后再弹回
    }
    
    override fun onStateChanged(refreshLayout: RefreshLayout, oldState: RefreshState, newState: RefreshState) {
        if (!mNoMoreData) {
            when (newState) {
                RefreshState.None, RefreshState.PullUpToLoad -> tvStatus.text = "上拉加载更多..."
                RefreshState.Loading -> tvStatus.text = "正在加载数据..."
                RefreshState.ReleaseToLoad -> tvStatus.text = "释放立即加载..."
                RefreshState.LoadFinish -> {
                }
                else -> {
                }
            }
        } else {
            tvStatus.text = "没有更多数据了..."
        }
    }
    
    override fun setPrimaryColors(vararg colors: Int) {}
    
    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {}
    
    override fun onMoving(isDragging: Boolean, percent: Float, offset: Int, height: Int, maxDragHeight: Int) {}
    
    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {}
    
    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {}
    
    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {}
    
    override fun isSupportHorizontalDrag(): Boolean {
        return false
    }
    
    override fun setNoMoreData(noMoreData: Boolean): Boolean {
        this.mNoMoreData = noMoreData
        return true
    }
}