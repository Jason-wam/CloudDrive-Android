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
import com.jason.videoview.adapter.VideoSpeedAdapter
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.model.SpeedEntity
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-20 16:38
 * @Description: TODO
 */
class SpeedView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs),
    IControlComponent {
    private var controlWrapper: ControlWrapper? = null
    private val rvData: RecyclerView by lazy { findViewById(R.id.rvData) }

    private val adapter: VideoSpeedAdapter by lazy {
        VideoSpeedAdapter(1.0f).apply {
            addData(SpeedEntity("× 3.0", 3.0f))
            addData(SpeedEntity("× 2.0", 2.0f))
            addData(SpeedEntity("× 1.5", 1.5f))
            addData(SpeedEntity("× 1.25", 1.25f))
            addData(SpeedEntity("× 1.0", 1.0f))
            addData(SpeedEntity("× 0.75", 0.75f))
            addData(SpeedEntity("× 0.5", 0.5f))

            setOnSelectListener(object : VideoSpeedAdapter.OnSelectListener {
                override fun onSelect(position: Int, item: SpeedEntity) {
                    visibility = View.GONE
                    controlWrapper?.speed = item.speed
                    controlWrapper?.startFadeOut()
                }
            })
        }
    }

    init {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.layout_player_video_speed_view, this, true)
        view.findViewById<View>(R.id.outView).setOnClickListener {
            visibility = View.GONE
        }
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
        rvData.adapter = adapter
        adapter.getData().forEachIndexed { index, fastForwardEntity ->
            if (fastForwardEntity.speed == controlWrapper.speed) {
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
        if (playState == VideoView.STATE_PREPARED) {
            if (controlWrapper?.speed != adapter.speed) {
                controlWrapper?.speed = adapter.speed
            }
        }
    }

    override fun onPlayerStateChanged(playerState: Int) {
        if (visibility == View.VISIBLE) {
            controlWrapper?.startFadeOut()
            visibility = View.GONE
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
        controlWrapper?.stopFadeOut()
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
                    it.isChecked = adapter.speed == scaleEntity.speed
                    it.setOnMenuItemClickListener {
                        adapter.speed = scaleEntity.speed
                        controlWrapper?.speed = scaleEntity.speed
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