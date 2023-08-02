package com.jason.cloud.media3.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresPermission

class VibratorUtil(val context: Context) {
    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun vibrateTo(duration: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createOneShot(duration, amplitude)
            vibrator.vibrate(vibrationEffect)
        } else {
            vibrator.vibrate(longArrayOf(0, duration), -1)
        }
    }

    @RequiresPermission(android.Manifest.permission.VIBRATE)
    fun vibrateEffect(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(effect)
        }
    }
}