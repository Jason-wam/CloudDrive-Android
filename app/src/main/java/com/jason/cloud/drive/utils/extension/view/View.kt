package com.jason.cloud.drive.utils.extension.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup

val View.screenShot: Bitmap?
    get() {
        return screenShot()
    }

fun View.screenShot(config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap? {
    return try {
        val screenshot = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(screenshot)
        canvas.translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(canvas)
        screenshot
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun View.removeFromParent(): ViewGroup? {
    return if (parent == null) {
        null
    } else {
        val viewGroup = (parent as ViewGroup)
        viewGroup.removeView(this)
        viewGroup
    }
}

fun View.resize(width: Int, height: Int, block: (View.() -> Unit)? = null) {
    post {
        val params = layoutParams
        params.width = width
        params.height = height
        layoutParams = params
        block?.invoke(this)
    }
}

/**
 * 根据View的原始宽度重新缩放大小，宽度不变
 * @param wScale 宽度比例
 * @param hScale 高度比例
 */
fun View.scaleByWidth(wScale: Int, hScale: Int, block: (View.() -> Unit)? = null) {
    post {
        val params = layoutParams
        val scaleSize = (width.toFloat() * hScale / wScale).toInt()
        if (scaleSize != 0) {
            params.height = scaleSize
            layoutParams = params
        }
        block?.invoke(this)
    }
}

/**
 * 根据View的原始宽度重新缩放大小，高度不变
 * @param wScale 宽度比例
 * @param hScale 高度比例
 */
fun View.scaleByHeight(wScale: Int, hScale: Int, block: (View.() -> Unit)? = null) {
    post {
        val params = layoutParams
        val scaleSize = (height.toFloat() * wScale / hScale).toInt()
        if (scaleSize != 0) {
            params.width = scaleSize
            layoutParams = params
        }
        block?.invoke(this)
    }
}

fun View.setItemRippleBackground() {
    val typedValue = TypedValue()
    this.context.theme.resolveAttribute(androidx.appcompat.R.attr.selectableItemBackground, typedValue, true)
    this.setBackgroundResource(typedValue.resourceId)
}

fun View.setItemRippleBackgroundBorderless() {
    val typedValue = TypedValue()
    this.context.theme.resolveAttribute(androidx.appcompat.R.attr.selectableItemBackgroundBorderless, typedValue, true)
    this.setBackgroundResource(typedValue.resourceId)
}

/**
 * 类似Toolbar按钮点击背景
 */
fun View.setNavigationItemBackground() {
    val typedValue = TypedValue()
    this.context.theme.resolveAttribute(androidx.appcompat.R.attr.controlBackground, typedValue, true)
    this.setBackgroundResource(typedValue.resourceId)
}

fun View.setClick(block: ((View) -> Unit)?) {
    this.setOnClickListener {
        block?.invoke(it)
    }
}

fun View.setLongClick(block: ((View) -> Unit)?) {
    this.setOnLongClickListener {
        block?.invoke(it)
        true
    }
}