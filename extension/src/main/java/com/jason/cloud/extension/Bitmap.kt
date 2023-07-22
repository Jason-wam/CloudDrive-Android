package com.jason.cloud.extension

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.provider.MediaStore
import java.io.File

fun Canvas.drawBitmap(bitmap: Bitmap, rotate: Float, rectF: RectF, paint: Paint) {
    val centerX: Float = bitmap.width / 2.0f
    val centerY: Float = bitmap.height / 2.0f
    // 创建一个 Matrix 对象，并设置旋转和平移变换
    val matrix = Matrix()
    matrix.setRotate(rotate, centerX, centerY) // 旋转变换
    matrix.postTranslate(rectF.left, rectF.top) // 平移变换（可选）
    drawBitmap(bitmap, matrix, paint)
}
/**
 * 将图片按指定度数旋转
 */
fun Bitmap.rotate(rotation: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(rotation)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * 将图片按倍数缩放，保持比例
 */
fun Bitmap.scale(scale: Float): Bitmap {
    val input = copy(Bitmap.Config.ARGB_8888, true)
    val matrix = Matrix()
    matrix.postScale(scale, scale)
    return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
}

fun Bitmap.resize(newHeight: Float): Bitmap {
    val scale = newHeight / height.toFloat()
    val newWidth = width * scale
    return resize(newWidth, newHeight)
}

/**
 * 将图片缩放到指定尺寸，不保持比例
 */
fun Bitmap.resize(w: Float, h: Float): Bitmap {
    val newBitmap = copy(Bitmap.Config.ARGB_8888, true)
    val sx = w / newBitmap.width
    val sy = h / newBitmap.height
    val matrix = Matrix()
    matrix.postScale(sx, sy)
    return Bitmap.createBitmap(newBitmap, 0, 0, newBitmap.width, newBitmap.height, matrix, true)
}

/**
 * 将图片缩放到指定尺寸，不保持比例
 */
fun Bitmap.resize(w: Int, h: Int): Bitmap {
    return resize(w.toFloat(), h.toFloat())
}

/**
 * 将图片切割为正方形
 */
inline val Bitmap.squared: Bitmap
    get() = run {
        this.cutSquare()
    }

/**
 * 将图片切割为正方形
 */
fun Bitmap.cutSquare(): Bitmap {
    return when {
        width > height -> {
            val x = (width - height) / 2
            Bitmap.createBitmap(this, x, 0, height, height)
        }

        width < height -> {
            val y = (height - width) / 2
            Bitmap.createBitmap(this, 0, y, width, width)
        }

        else -> this
    }
}

/**
 * 将图片裁剪为圆形
 */
inline val Bitmap.circled: Bitmap
    get() = run {
        this.cutCircle()
    }

/**
 * 将图片裁剪为圆形
 */
fun Bitmap.cutCircle(): Bitmap {
    val squared = cutSquare() //创建新画布
    val newBitmap = Bitmap.createBitmap(squared.width, squared.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(newBitmap) //在画布上裁剪出圆形

    val path = Path()
    path.addCircle(squared.width / 2f, squared.width / 2f, squared.width / 2f, Path.Direction.CW)
    canvas.clipPath(path) //将原图绘制在圆形中

    val paint = Paint()
    paint.color = Color.GRAY
    paint.isAntiAlias = true
    canvas.drawBitmap(squared, 0f, 0f, paint) //返回新绘制的圆形图片
    return newBitmap
}


fun Bitmap?.saveToFile(file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 100): Boolean {
    if (this == null) {
        return false
    }
    if (file.exists()) {
        file.delete()
        file.createNewFile()
    }
    return try {
        file.outputStream().use {
            compress(format, quality, it)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Bitmap.saveToGallery(context: Context, fileName: String): Boolean {
    val contentValues = ContentValues()
    contentValues.put(MediaStore.MediaColumns.TITLE, fileName)
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    try {
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
        context.contentResolver.openOutputStream(uri)?.use {
            if (compress(Bitmap.CompressFormat.JPEG, 100, it)) {
                context.sendBroadcast(Intent("com.android.camera.NEW_PICTURE", uri))
                return true
            }
        }
        return false
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}