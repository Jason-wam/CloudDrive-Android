package com.jason.videoview.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.jason.videoview.R
import com.jason.videoview.adapter.VideoSelectAdapter
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.controller.MediaDataController.OnDataChangedListener
import com.jason.videoview.controller.MediaDataController.OnPlayListener
import com.jason.videoview.model.VideoData
import xyz.doikki.videoplayer.controller.ControlWrapper
import xyz.doikki.videoplayer.controller.IControlComponent
import xyz.doikki.videoplayer.model.TimedText
import xyz.doikki.videoplayer.util.PlayerUtils

@SuppressLint("NotifyDataSetChanged,SetTextI18n")
class SelectView(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs),
    IControlComponent {
    private val tvTitle: TextView by lazy { findViewById(R.id.tvTitle) }
    private var controlWrapper: ControlWrapper? = null
    private val rvData: RecyclerView by lazy {
        findViewById(R.id.rvData)
    }
    private val adapter = VideoSelectAdapter().apply {
        setOnSelectListener(object : VideoSelectAdapter.OnSelectListener {
            override fun onSelect(position: Int) {
                visibility = View.GONE
                controlWrapper?.startFadeOut()
                controller?.start(position)
            }
        })
    }

    private val onPlayListener = object : OnPlayListener {
        override fun onPlay(position: Int, videoData: VideoData) {
            adapter.selectedPosition = position
            adapter.notifyDataSetChanged()
        }
    }

    private val onDataChangedListener = object : OnDataChangedListener {
        override fun onDataChanged(data: List<VideoData>) {
            tvTitle.text = "选集（${data.size}）"
            adapter.setData(data)
            adapter.notifyDataSetChanged()
        }
    }

    init {
        visibility = View.GONE
        LayoutInflater.from(context).inflate(R.layout.layout_player_video_select_view, this, true)
        findViewById<View>(R.id.outView).setOnClickListener {
            visibility = View.GONE
        }
    }

    override fun attach(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
        rvData.adapter = adapter
        adapter.setData(controller?.getData() ?: emptyList())
        adapter.notifyDataSetChanged()
    }

    override fun getView(): View {
        return this
    }

    override fun onVisibilityChanged(isVisible: Boolean, anim: Animation?) {
        if (isVisible) {
            rvData.scrollToPosition(adapter.selectedPosition)
        }
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
            controller?.getData()?.forEachIndexed { index, videoData ->
                menu.add(0, index, index, videoData.name).also {
                    it.isCheckable = true
                    it.isChecked = adapter.selectedPosition == index
                    it.setOnMenuItemClickListener {
                        adapter.selectedPosition = index
                        controller?.start(index)
                        controlWrapper?.startFadeOut()
                        true
                    }
                }
            }
        }.show()
    }

    private var controller: MediaDataController? = null
    override fun setDataController(controller: MediaDataController) {
        this.tvTitle.text = "选集（${controller.getData().size}）"
        this.controller = controller
        controller.addOnPlayListener(onPlayListener)
        controller.addOnDataChangedListener(onDataChangedListener)
    }
}