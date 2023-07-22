package com.jason.cloud.drive.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class SelectFilesContract : ActivityResultContract<String, List<Uri>>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = input
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return if (resultCode != Activity.RESULT_OK) {
            emptyList()
        } else {
            arrayListOf<Uri>().apply {
                val clipData = intent?.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        add(clipData.getItemAt(i).uri)
                    }
                } else {
                    intent?.data?.let {
                        add(it)
                    }
                }
            }
        }
    }
}