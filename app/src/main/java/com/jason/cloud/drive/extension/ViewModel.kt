package com.jason.cloud.drive.extension

import android.content.Context
import androidx.lifecycle.AndroidViewModel

inline val AndroidViewModel.context: Context
    get() {
        return getApplication()
    }