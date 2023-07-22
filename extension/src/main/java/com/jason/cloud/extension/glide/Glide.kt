package com.jason.cloud.extension.glide

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.signature.ObjectKey
import com.jason.cloud.extension.glide.transformer.AvatarTransformer
import com.jason.cloud.extension.glide.transformer.BlurTransformer
import java.io.File

fun ImageView.loadIMG(
    @RawRes @DrawableRes resourceId: Int,
    block: (RequestBridge.() -> Any)? = null
): ImageView {
    val builder = Glide.with(this).load(resourceId).timeout(30000)
    val bridge = RequestBridge(this, builder)
    block?.invoke(bridge)
    bridge.into(this)
    return this
}

fun ImageView.loadIMG(url: String, block: (RequestBridge.() -> Any)? = null): ImageView {
    val builder = Glide.with(this).load(url).timeout(30000)
    val bridge = RequestBridge(this, builder)
    block?.invoke(bridge)
    bridge.into(this)
    return this
}

fun ImageView.loadIMG(byte: ByteArray, block: (RequestBridge.() -> Any)? = null): ImageView {
    val builder = Glide.with(this).load(byte).timeout(30000)
    val bridge = RequestBridge(this, builder)
    block?.invoke(bridge)
    bridge.into(this)
    return this
}

fun ImageView.loadIMG(uri: Uri?, block: (RequestBridge.() -> Any)? = null): ImageView {
    val builder = Glide.with(this).load(uri).timeout(30000)
    val bridge = RequestBridge(this, builder)
    block?.invoke(bridge)
    bridge.into(this)
    return this
}

fun ImageView.loadIMG(bitmap: Bitmap?, block: (RequestBridge.() -> Any)? = null): ImageView {
    val builder = Glide.with(this).load(bitmap).timeout(30000)
    val bridge = RequestBridge(this, builder)
    block?.invoke(bridge)
    bridge.into(this)
    return this
}

fun ImageView.loadIMG(file: File?, block: (RequestBridge.() -> Any)? = null): ImageView {
    val builder = Glide.with(this).load(file).timeout(30000)
    val bridge = RequestBridge(this, builder)
    block?.invoke(bridge)
    bridge.into(this)
    return this
}

fun ImageView.loadIMG(drawable: Drawable?, block: (RequestBridge.() -> Any)? = null): ImageView {
    val builder = Glide.with(this).load(drawable).timeout(30000)
    val bridge = RequestBridge(this, builder)
    block?.invoke(bridge)
    bridge.into(this)
    return this
}

inline fun RequestBuilder<Drawable>.listener(crossinline block: (succeed: Boolean, drawable: Drawable?) -> Unit): RequestBuilder<Drawable> {
    return addListener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: com.bumptech.glide.request.target.Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            block.invoke(false, null)
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: com.bumptech.glide.request.target.Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            if (resource != null) {
                block.invoke(true, resource)
            } else {
                block.invoke(false, null)
            }
            return false
        }
    })
}

class RequestBridge(
    private val imageView: ImageView,
    private var builder: RequestBuilder<Drawable>
) {
    fun timeout(timeoutMs: Int): RequestBridge {
        builder = builder.timeout(timeoutMs)
        return this
    }

    fun centerCrop(): RequestBridge {
        builder = builder.centerCrop()
        return this
    }

    fun circleCrop(): RequestBridge {
        builder = builder.circleCrop()
        return this
    }

    fun centerInside(): RequestBridge {
        builder = builder.centerInside()
        return this
    }

    fun fitXY(): RequestBridge {
        imageView.scaleType = ImageView.ScaleType.FIT_XY
        return this
    }

    fun fitCenter(): RequestBridge {
        builder = builder.fitCenter()
        return this
    }

    fun background(@DrawableRes resId: Int): RequestBridge {
        imageView.setBackgroundResource(resId)
        return this
    }

    fun background(drawable: Drawable?): RequestBridge {
        imageView.setImageDrawable(drawable)
        return this
    }

    fun backgroundColor(@ColorInt color: Int): RequestBridge {
        imageView.setBackgroundColor(color)
        return this
    }

    fun override(size: Int): RequestBridge {
        builder = builder.override(size)
        return this
    }

    fun override(w: Int, h: Int): RequestBridge {
        builder = builder.override(w, h)
        return this
    }

    fun placeholder(@DrawableRes resId: Int): RequestBridge {
        builder = builder.placeholder(resId)
        builder = builder.error(resId)
        return this
    }

    fun placeholder(drawable: Drawable?): RequestBridge {
        builder = builder.placeholder(drawable)
        builder = builder.error(drawable)
        return this
    }

    fun error(@DrawableRes resId: Int): RequestBridge {
        builder = builder.error(resId)
        return this
    }

    fun error(drawable: Drawable?): RequestBridge {
        builder = builder.error(drawable)
        return this
    }

    fun transform(transformation: Transformation<Bitmap>): RequestBridge {
        builder = builder.transform(transformation)
        return this
    }

    fun blurCrop(radius: Int, scale: Float = 1f): RequestBridge {
        builder = builder.transform(BlurTransformer(imageView.context, radius, scale))
        return this
    }

    fun applyAvatarTransformer(@DrawableRes connerResId: Int = 0): RequestBridge {
        val transformer = AvatarTransformer(
            imageView.context,
            connerResId,
            3.0f,
            AvatarTransformer.ConnerGravity.BOTTOM_END
        )
        builder = builder.transform(transformer)
        return this
    }

    fun applyAvatarTransformer(
        @DrawableRes connerResId: Int = 0,
        connerScale: Float = 3.0f
    ): RequestBridge {
        val transformer = AvatarTransformer(
            imageView.context,
            connerResId,
            connerScale,
            AvatarTransformer.ConnerGravity.BOTTOM_END
        )
        builder = builder.transform(transformer)
        return this
    }

    fun applyAvatarTransformer(
        @DrawableRes connerResId: Int = 0,
        connerScale: Float = 3.0f,
        gravity: AvatarTransformer.ConnerGravity = AvatarTransformer.ConnerGravity.BOTTOM_END
    ): RequestBridge {
        val transformer = AvatarTransformer(imageView.context, connerResId, connerScale, gravity)
        builder = builder.transform(transformer)
        return this
    }

    fun transition(options: TransitionOptions<*, Drawable>) {
        builder = builder.transition(options)
    }

    fun signature(key: Any) {
        builder = builder.signature(ObjectKey(key))
    }

    fun addListener(block: (succeed: Boolean, drawable: Drawable?) -> Unit): RequestBridge {
        builder = builder.addListener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                block.invoke(false, null)
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                block.invoke(true, resource)
                return false
            }
        })
        return this
    }

    fun into(view: ImageView) {
        builder.into(view)
    }
}
