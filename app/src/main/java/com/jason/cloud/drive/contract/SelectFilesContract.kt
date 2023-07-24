package com.jason.cloud.drive.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class SelectFilesContract : ActivityResultContract<String, List<Uri>>() {
    override fun createIntent(context: Context, input: String): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = input
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        return Intent.createChooser(intent, "上传文件到此目录")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return if (resultCode != Activity.RESULT_OK) {//啥也没选
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