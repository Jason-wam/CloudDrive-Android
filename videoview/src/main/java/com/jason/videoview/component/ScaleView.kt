package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.jason.videoview.R
import com.jason.videoview.adapter.VideoScaleAdapter
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.model.ScaleEntity
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

class ScaleView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs),
    IControlComponent {
    private var controlWrapper: ControlWrapper? = null
    private val rvData: RecyclerView by lazy { findViewById(R.id.rvData) }
    private val adapter: VideoScaleAdapter =
        VideoScaleAdapter(VideoView.SCREEN_SCALE_DEFAULT).apply {
            addData(ScaleEntity("自适应", VideoView.SCREEN_SCALE_DEFAULT))
            addData(ScaleEntity("原始比例", VideoView.SCREEN_SCALE_ORIGINAL))
            addData(ScaleEntity("全屏拉伸", VideoView.SCREEN_SCALE_MATCH_PARENT))
            addData(ScaleEntity("居中裁剪", VideoView.SCREEN_SCALE_CENTER_CROP))
            addData(ScaleEntity("4 × 3", VideoView.SCREEN_SCALE_4_3))
            addData(ScaleEntity("16 × 9", VideoView.SCREEN_SCALE_16_9))

            setOnSelectListener(object : VideoScaleAdapter.OnSelectListener {
                override fun onSelect(position: Int, item: ScaleEntity) {
                    visibility = View.GONE
                    scaleType = item.scale
                    controlWrapper?.setScreenScaleType(scaleType)
                    controlWrapper?.startFadeOut()
                }
            })
        }

    init {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.layout_player_video_scale_view, this, true)
        rvData.adapter = adapter
        view.findViewById<View>(R.id.outView).setOnClickListener {
            visibility = View.GONE
        }
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
        adapter.getData().forEachIndexed { index, fastForwardEntity ->
            if (fastForwardEntity.scale == adapter.scaleType) {
                rvData.scrollToPosition(index)
            }
        }
    }

    override fun getView(): View {
        return this
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        adapter.notifyDataSetChanged()
        if (visibility == View.VISIBLE) {
            controlWrapper?.startFadeOut()
            visibility = View.GONE
        }
    }

    override fun onPlayStateChanged(playState: Int) {
    }

    override fun onPlayerStateChanged(playerState: Int) {
        if (visibility == View.VISIBLE) {
            controlWrapper?.startFadeOut()
            visibility = View.GONE
        }
        if (controlWrapper?.isFullScreen == true) {
            controlWrapper?.setScreenScaleType(adapter.scaleType)
        } else {
            controlWrapper?.setScreenScaleType(VideoView.SCREEN_SCALE_DEFAULT)
        }
    }

    override fun setProgress(duration: Int, position: Int) {
    }

    override fun onLockStateChanged(isLocked: Boolean) {
    }

    override fun onTimedText(timedText: TimedText?) {

    }

    fun show() {
        controlWrapper?.hide()
        controlWrapper?.startFadeOut()
        visibility = View.VISIBLE
        bringToFront()
    }

    fun show(anchor: View) {
        controlWrapper?.stopFadeOut()
        if (PlayerUtils.isNotInLandscape(context)) {
            showMenuView(anchor)
        } else {
            show()
        }
    }

    private fun showMenuView(anchor: View) {
        PopupMenu(anchor.context, anchor).apply {
            adapter.getData().forEachIndexed { index, scaleEntity ->
                menu.add(0, index, index, scaleEntity.title).also {
                    it.isCheckable = true
                    it.isChecked = adapter.scaleType == scaleEntity.scale
                    it.setOnMenuItemClickListener {
                        adapter.scaleType = scaleEntity.scale
                        controlWrapper?.setScreenScaleType(scaleEntity.scale)
                        controlWrapper?.startFadeOut()
                        true
                    }
                }
            }
        }.show()
    }

    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.controller = controller
    }
}