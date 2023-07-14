package com.jason.videocat.utils.extension.view

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat

fun Toolbar.getTitleTextView(): TextView? {
    getChildAt(0)?.let {
        if (it is TextView) {
            return it
        }
    }
    return null
}

fun Toolbar.setTitleFont(path: String) {
    try {
        getTitleTextView()?.let {
            it.typeface = Typeface.createFromAsset(context.assets, path)
            it.includeFontPadding = false
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

inline fun Toolbar.onMenuItemClickListener(crossinline block: (id: Int, MenuItem, View?) -> Unit) {
    setOnMenuItemClickListener {
        block.invoke(it.itemId, it, findViewById(it.itemId))
        true
    }
}

inline fun Toolbar.onMenuItemClickListener(@IdRes id: Int, crossinline block: (View?) -> Unit) {
    menu.findItem(id)?.setOnMenuItemClickListener {
        if (it.itemId == id) {
            block.invoke(findViewById(it.itemId))
        }
        true
    }
}

fun Toolbar.setMenuIcon(@IdRes id: Int, icon: Drawable) {
    menu.findItem(id)?.icon = icon
}

fun Toolbar.setMenuIcon(@IdRes id: Int, @DrawableRes icon: Int) {
    menu.findItem(id)?.setIcon(icon)
}

@RequiresApi(26)
fun Toolbar.setMenuIconTintColor(@IdRes id: Int, @ColorRes colorRes:Int) {
    val tint = ContextCompat.getColor(context,colorRes)
    menu.findItem(id)?.iconTintList = ColorStateList.valueOf(tint)
}

@RequiresApi(26)
fun Toolbar.setMenuIconTintColorInt(@IdRes id: Int, @ColorInt color:Int) {
    menu.findItem(id)?.iconTintList = ColorStateList.valueOf(color)
}