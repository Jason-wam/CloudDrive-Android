package com.jason.cloud.extension

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Activity?.toast(text: CharSequence) {
    runOnMain {
        this?.also {
            Toast.makeText(it, text, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Fragment?.toast(text: CharSequence) {
    runOnMain {
        this?.also {
            Toast.makeText(it.context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Activity?.toast(@StringRes resId: Int) {
    runOnMain {
        this?.also {
            Toast.makeText(it, resources.getString(resId), Toast.LENGTH_SHORT).show()
        }
    }
}

fun Fragment?.toast(@StringRes resId: Int) {
    runOnMain {
        this?.also {
            Toast.makeText(it.context, resources.getString(resId), Toast.LENGTH_SHORT).show()
        }
    }
}

fun Activity?.toast(@StringRes resId: Int, vararg formatArgs: Any) {
    runOnMain {
        this?.also {
            Toast.makeText(it, resources.getString(resId, formatArgs), Toast.LENGTH_SHORT).show()
        }
    }
}

fun Fragment?.toast(@StringRes resId: Int, vararg formatArgs: Any) {
    runOnMain {
        this?.also {
            it.context.toast(resId, formatArgs)
        }
    }
}

fun Context?.toast(text: CharSequence) {
    runOnMain {
        this?.also {
            Toast.makeText(it, text, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context?.toast(@StringRes resId: Int) {
    runOnMain {
        this?.also {
            Toast.makeText(it, resId, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context?.toast(@StringRes resId: Int, vararg formatArgs: Any) {
    runOnMain {
        this?.also {
            Toast.makeText(it, resources.getString(resId, formatArgs), Toast.LENGTH_SHORT).show()
        }
    }
}