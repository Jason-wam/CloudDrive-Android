package com.jason.cloud.extension.glide.transformer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.jason.cloud.extension.scale
import java.security.MessageDigest

class VideoCoverTransformer : BitmapTransformation() {
    private val id = "com.bumptech.glide.transformations.VideoCoverTransformer"
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        return if (toTransform.height >= toTransform.width) {
            val bitmap = blurCrop(toTransform)
            val centered = TransformationUtils.centerInside(pool, toTransform, outWidth, outHeight)
            val background = TransformationUtils.centerCrop(pool, bitmap, outWidth, outHeight)

            val newBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(newBitmap)
            canvas.drawBitmap(background, 0f, 0f, null)
            canvas.drawColor(Color.parseColor("#30000000"))
            canvas.drawBitmap(centered, outWidth / 2f - centered.width / 2f, 0f, null)
            TransformationUtils.centerCrop(pool, newBitmap, outWidth, outHeight)
        } else {
            TransformationUtils.centerCrop(pool, toTransform, outWidth, outHeight)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is VideoCoverTransformer
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    private fun blurCrop(source: Bitmap): Bitmap {
        var newBitmap = source.scale(0.5f)
        try {
            newBitmap = FastBlurUtil.doBlur(newBitmap, 15, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newBitmap
    }
}