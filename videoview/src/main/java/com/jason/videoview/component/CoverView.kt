package com.jason.videoview.component


import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jason.videoview.R
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.util.FastBlurUtil
import com.jason.videoview.view.ScaleImageView
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView

class CoverView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs),
    IControlComponent {
    private var ivCover: ScaleImageView
    private var ivBlurBackground: ImageView
    private var progressBar: ProgressBar
    private lateinit var controlWrapper: ControlWrapper

    init {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.layout_player_video_cover_view, this, true)
        ivBlurBackground = findViewById(R.id.iv_blur_background)
        progressBar = findViewById(R.id.progressBar)
        ivCover = findViewById(R.id.iv_cover)
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (!isVisible) {
            visibility = View.GONE
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        visibility = View.GONE
    }

    override fun onPlayerStateChanged(playerState: Int) {
        if (playerState == VideoView.STATE_IDLE) {
            visibility = View.VISIBLE
            bringToFront()
        }
    }

    override fun setProgress(duration: Int, position: Int) {

    }

    override fun onLockStateChanged(isLocked: Boolean) {

    }

    override fun onTimedText(timedText: TimedText?) {

    }

    override fun setDataController(controller: MediaDataController) {

    }

    fun setCover(url: String) {
        visibility = View.VISIBLE
        progressBar.isVisible = true
        ivCover.setImageDrawable(null)
        ivBlurBackground.setImageDrawable(null)

        ivCover.post {
            Glide.with(ivCover).asBitmap().override(1080).load(url)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (controlWrapper.isPlaying) {
                            return true
                        }
                        Handler(Looper.getMainLooper()).post {
                            setDefaultCover()
                        }
                        return true
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (controlWrapper.isPlaying) {
                            return true
                        }
                        Handler(Looper.getMainLooper()).post {
                            setCover(resource)
                        }
                        return true
                    }
                }).submit()
        }
    }

    fun setCover(bitmap: Bitmap?) {
        visibility = View.VISIBLE
        ivCover.setImageDrawable(null)
        ivBlurBackground.setImageDrawable(null)
        ivCover.post {
            if (bitmap == null) {
                setDefaultCover()
            } else {
                Log.i("CoverView", "w = ${bitmap.width},h = ${bitmap.height}")
                ivCover.setBasedOnWidth(false)
                ivCover.setScale(bitmap.width, bitmap.height)
                ivCover.setImageBitmap(bitmap)

                val blurBitmap = FastBlurUtil.doBlur(bitmap, 10, 0.2f, false)
                ivBlurBackground.setImageBitmap(blurBitmap)

                progressBar.isVisible = false
            }
        }
    }

    fun setCover(drawable: Drawable?) {
        visibility = View.VISIBLE
        ivCover.setImageDrawable(null)
        ivBlurBackground.setImageDrawable(null)
        ivCover.post {
            if (drawable == null) {
                setDefaultCover()
            } else {
                ivCover.setImageDrawable(drawable)
                val bitmap = drawable.toBitmap()
                ivCover.setBasedOnWidth(false)
                ivCover.setScale(bitmap.width, bitmap.height)
                ivCover.setImageBitmap(bitmap)

                val blurBitmap = FastBlurUtil.doBlur(bitmap, 10, 0.2f, false)
                ivBlurBackground.setImageBitmap(blurBitmap)
                progressBar.isVisible = false
            }
        }
    }

    private fun setDefaultCover() {
        ivCover.post {
            ContextCompat.getDrawable(context, R.drawable.ic_default_cover)?.toBitmap()
                ?.also { bitmap ->
                    ivCover.setBasedOnWidth(false)
                    ivCover.setImageResource(R.drawable.ic_default_cover)
                    ivCover.setImageBitmap(bitmap)

                    val blurBitmap = FastBlurUtil.doBlur(bitmap, 10, 0.2f, false)
                    ivBlurBackground.setImageBitmap(blurBitmap)
                    progressBar.isVisible = false
                }
        }
    }

    fun setInTiny(tiny: Boolean) {
        if (tiny) {
            this.ivCover.visibility = View.GONE
        } else {
            this.ivCover.visibility = View.VISIBLE
        }
    }
}