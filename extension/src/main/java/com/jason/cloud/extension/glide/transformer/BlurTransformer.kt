package com.jason.cloud.extension.glide.transformer

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import com.jason.cloud.extension.scale
import java.security.MessageDigest

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-14 3:09
 * @Description: TODO
 */
class BlurTransformer(val context: Context, val radius: Int, private val scale: Float = 1f) :
    BitmapTransformation() {
    private val id = "com.bumptech.glide.transformations.BlurTransformer"

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update((id + radius + scale).toByteArray(CHARSET))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val bitmap = blurCrop(toTransform)
        return TransformationUtils.centerCrop(pool, bitmap, outWidth, outHeight)
    }

    override fun equals(other: Any?): Boolean {
        return other is BlurTransformer
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    private fun blurCrop(source: Bitmap): Bitmap {
        var newBitmap = source.scale(scale)
        try {
            newBitmap = FastBlurUtil.doBlur(newBitmap, radius, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newBitmap
    }
}