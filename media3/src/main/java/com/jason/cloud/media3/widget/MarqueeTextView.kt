package com.jason.cloud.media3.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * @文件名: MarqueeTextView
 * @创建者: 进阶的面条
 * @创建日期: 2020/8/25 8:46
 * @描述: 跑马灯TextView，解决丢失焦点的问题
 */
open class MarqueeTextView(context: Context, attrs: AttributeSet? = null) :
    AppCompatTextView(context, attrs) {
    /*
     *这个属性这个View得到焦点,在这里我们设置为true,这个View就永远是有焦点的
     */
    override fun isFocused(): Boolean {
        return true
    }

    init {
        //设置单行
        isSingleLine = true
        //设置Ellipsize
        ellipsize = TextUtils.TruncateAt.MARQUEE
        //获取焦点
        isFocusable = true
        //走马灯的重复次数，-1代表无限重复
        marqueeRepeatLimit = -1
        //强制获得焦点
        isFocusableInTouchMode = true
        isHorizontalFadingEdgeEnabled = true
    }
}