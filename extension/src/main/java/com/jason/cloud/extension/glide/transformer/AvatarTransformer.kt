package com.jason.cloud.extension.glide.transformer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.jason.cloud.extension.cutCircle
import com.jason.cloud.extension.resize
import java.security.MessageDigest

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-14 3:09
 * @Description: TODO
 */
class AvatarTransformer(
    private val context: Context,
    @DrawableRes private val connerResId: Int = 0,
    private val connerScale: Float = 3f,
    private val gravity: ConnerGravity = ConnerGravity.BOTTOM_END
) : BitmapTransformation() {
    private val id = "com.bumptech.glide.transformations.AvatarTransformer"

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(id.toByteArray(CHARSET))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        var bitmap = toTransform.cutCircle().resize(outWidth, outHeight)
        if (connerResId != 0) {
            ContextCompat.getDrawable(context, connerResId)?.let {
                bitmap = bitmap.addConner(it, connerScale, gravity)
            }
        }
        return bitmap
    }

    override fun equals(other: Any?): Boolean {
        return other is AvatarTransformer
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    private fun Bitmap.addConner(
        conner: Drawable,
        scale: Float = 2f,
        gravity: ConnerGravity = ConnerGravity.BOTTOM_END
    ): Bitmap {
        val baseBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(baseBitmap)

        val paint = Paint()
        paint.color = Color.GRAY
        paint.isAntiAlias = true
        canvas.drawBitmap(this, 0f, 0f, paint)

        val connerW = (width / scale).toInt()
        val connerH = (height / scale).toInt()
        when (gravity) {
            ConnerGravity.TOP_START -> {
                conner.setBounds(0, 0, connerW, connerH)
            }

            ConnerGravity.TOP_CENTER -> {
                conner.setBounds(width / 2 - connerW / 2, 0, width / 2 + connerW / 2, connerH)
            }

            ConnerGravity.TOP_END -> {
                conner.setBounds(width - connerW, 0, width, connerH)
            }

            ConnerGravity.BOTTOM_START -> {
                conner.setBounds(0, height - connerH, connerW, height)
            }

            ConnerGravity.BOTTOM_CENTER -> {
                conner.setBounds(
                    width / 2 - connerW / 2,
                    height - connerH,
                    width / 2 + connerW / 2,
                    height
                )
            }

            ConnerGravity.BOTTOM_END -> {
                conner.setBounds(width - connerW, height - connerH, width, height)
            }

            ConnerGravity.CENTER -> {
                conner.setBounds(
                    width / 2 - connerW / 2,
                    height / 2 - connerH / 2,
                    width / 2 + connerW / 2,
                    height / 2 + connerH / 2
                )
            }

            ConnerGravity.START -> {
                conner.setBounds(0, height / 2 - connerH / 2, connerW, height / 2 + connerH / 2)
            }

            ConnerGravity.END -> {
                conner.setBounds(
                    width - connerW,
                    height / 2 - connerH / 2,
                    width,
                    height / 2 + connerH / 2
                )
            }
        }
        conner.draw(canvas)
        return baseBitmap
    }

    enum class ConnerGravity {
        TOP_START, TOP_CENTER, TOP_END, BOTTOM_START, BOTTOM_CENTER, BOTTOM_END, CENTER, START, END
    }
}