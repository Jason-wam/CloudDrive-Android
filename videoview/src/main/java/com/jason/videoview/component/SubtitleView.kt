package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.jason.videoview.R
import com.jason.videoview.adapter.VideoTrackAdapter
import com.jason.videoview.controller.MediaDataController
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.model.Track
import xyz.doikki.videoplayer.util.PlayerUtils

/**
 * @文件名:
 * @创建者: 进阶的面条
 * @创建日期:
 * @描述: TODO
 */
class SubtitleView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs),
    IControlComponent {
    private lateinit var controlWrapper: ControlWrapper
    private lateinit var adapter: VideoTrackAdapter
    private var rvData: RecyclerView

    init {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.layout_player_video_track_view, this, true)
        view.findViewById<View>(R.id.outView).setOnClickListener {
            visibility = View.GONE
        }
        rvData = view.findViewById(R.id.rvData)
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
        adapter = VideoTrackAdapter(controlWrapper.selectedSubtitleIndex).apply {
            setOnSelectListener(object : VideoTrackAdapter.OnSelectListener {
                override fun onSelect(position: Int, item: Track) {
                    visibility = View.GONE
                    trackIndex = item.index
                    controlWrapper.selectSubtitle(item)
                    controlWrapper.startFadeOut()
                }
            })
        }
        rvData.adapter = adapter
    }

    override fun getView(): View {
        return this
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (visibility == View.VISIBLE) {
            controlWrapper.startFadeOut()
            visibility = View.GONE
        }
    }

    override fun onPlayStateChanged(playState: Int) {
    }

    override fun onPlayerStateChanged(playerState: Int) {
        if (visibility == View.VISIBLE) {
            controlWrapper.startFadeOut()
            visibility = View.GONE
        }
    }

    override fun setProgress(duration: Int, position: Int) {
    }

    override fun onLockStateChanged(isLocked: Boolean) {
    }

    override fun onTimedText(timedText: TimedText?) {
    }

    @SuppressLint("NotifyDataSetChanged")
    fun show() {
        visibility = VISIBLE
        bringToFront()
        Log.i("SubtitleView", "showSelectView")
        Log.i("SubtitleView", "size = ${controlWrapper.subtitles.size}")
        adapter.setData(controlWrapper.subtitles)
        adapter.getData().forEachIndexed { index, fastForwardEntity ->
            if (fastForwardEntity.index == adapter.trackIndex) {
                rvData.scrollToPosition(index)
            }
        }
        adapter.notifyDataSetChanged()
    }

    fun show(anchor: View) {
        controlWrapper.stopFadeOut()
        if (PlayerUtils.isNotInLandscape(context)) {
            showMenuView(anchor)
        } else {
            show()
        }
    }

    private fun showMenuView(anchor: View) {
        PopupMenu(anchor.context, anchor).apply {
            controlWrapper.subtitles.forEachIndexed { index, track ->
                menu.add(0, index, index, track.name).also {
                    it.isCheckable = true
                    it.isChecked = adapter.trackIndex == track.index
                    it.setOnMenuItemClickListener {
                        adapter.trackIndex = track.index
                        controlWrapper.selectSubtitle(track)
                        controlWrapper.startFadeOut()
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