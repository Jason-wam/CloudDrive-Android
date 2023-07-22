package com.jason.cloud.extension

import android.content.res.Resources
import android.util.TypedValue

inline val Int.dp: Int
    get() = run {
        val scale: Float = Resources.getSystem().displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }

inline val Float.dp: Float
    get() = run {
        val scale: Float = Resources.getSystem().displayMetrics.density
        return this * scale + 0.5f
    }

inline val Double.dp: Float
    get() = run {
        return toFloat().dp
    }

inline val Int.px: Int
    get() = run {
        return dp2px(this.toFloat())
    }

inline val Float.px: Int
    get() = run {
        return dp2px(this)
    }


inline val Double.px: Int
    get() = run {
        return dp2px(this.toFloat())
    }

fun dp2px(dpValue: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, Resources.getSystem().displayMetrics).toInt()
}
