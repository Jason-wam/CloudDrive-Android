package com.jason.cloud.drive.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class FileSelectContract : ActivityResultContract<String, Uri?>() {
    override fun createIntent(context: Context, input: String): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = input
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == Activity.RESULT_OK) {
            intent?.data
        } else {
            null
        }
    }
}