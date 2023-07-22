package com.jason.cloud.extension

import android.content.Context
import androidx.lifecycle.AndroidViewModel

inline val AndroidViewModel.context: Context
    get() {
        return getApplication()
    }