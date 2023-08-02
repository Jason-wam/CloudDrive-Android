package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutAudioPlayerDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.service.AudioService
import com.jason.cloud.drive.utils.VideoDataController
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3VideoItem
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.utils.Media3PlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Serializable

class AudioPlayDialog :
    BaseBindBottomSheetDialogFragment<LayoutAudioPlayerDialogBinding>(R.layout.layout_audio_player_dialog),
    VideoDataController.OnCompleteListener, ServiceConnection, VideoDataController.OnPlayListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var mediaInfoJob: Job? = null

    private fun testBlurBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog?.window?.let {
                it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                it.attributes.blurBehindRadius = 64
            }
        }
    }

    override fun initView(view: View) {
        super.initView(view)
        setCanceledOnTouchOutside(false)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.handler.isSelected = slideOffset != 0f
            }
        })

        binding.ibPrevious.setOnClickListener {
            previous()
        }
        binding.ibNext.setOnClickListener {
            next()
        }

        VideoDataController.with("AudioService").addOnPlayListener(this)
        VideoDataController.with("AudioService").addOnCompleteListener(this)

        addOnDismissListener {
            VideoDataController.with("AudioService").removeOnPlayListener(this)
            VideoDataController.with("AudioService").removeOnCompleteListener(this)
        }

        val position = arguments?.getInt("position", 0) ?: 0
        val audioList = arguments?.getSerializableListExtraEx<FileEntity>("list")?.let {
            ArrayList<Media3VideoItem>().apply {
                it.forEach {
                    add(Media3VideoItem().apply {
                        this.url = it.rawURL
                        this.title = it.name
                        this.image = it.thumbnailURL
                    })
                }
            }
        }

        if (audioList != null) {
            AudioService.launchWith(requireContext(), audioList, position) {
                context?.bindService(this, this@AudioPlayDialog, 0)
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setData(audioList: List<FileEntity>, position: Int): AudioPlayDialog {
        arguments?.putInt("position", position)
        arguments?.putSerializable("list", audioList as Serializable)
        return this
    }

    private fun previous() {
        if (VideoDataController.with("AudioService").hasPrevious()) {
            VideoDataController.with("AudioService").previous()
        } else {
            toast("已经是第一个啦")
        }
    }

    private fun next() {
        if (VideoDataController.with("AudioService").hasNext()) {
            VideoDataController.with("AudioService").next()
        } else {
            toast("已经是最后一个啦")
        }
    }


    override fun onPlay(position: Int, videoData: Media3VideoItem) {
        loadMediaInfo(videoData.url)
        binding.tvTitle.text = videoData.title
        binding.tvArtist.text = "未知艺术家"
        binding.ivCover.loadIMG(videoData.image) {
            placeholder(R.drawable.ic_default_audio_cover)
            centerCrop()
        }

        binding.btnBackground.alpha = 1f
        binding.btnBackground.isEnabled = true
        binding.btnBackground.setOnClickListener {
            dismiss()
        }
    }

    override fun onCompletion() {
        dismiss()
    }

    @SuppressLint("SetTextI18n")
    private fun loadMediaInfo(url: String) {
        mediaInfoJob?.cancel()
        mediaInfoJob = scope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(url, HashMap<String, String>())
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                if (title?.isNotBlank() == true) {
                    binding.tvTitle.text = title
                }
                if (artist.isNullOrBlank()) {
                    binding.tvArtist.text = "未知艺术家"
                } else {
                    if (album.isNullOrBlank()) {
                        binding.tvArtist.text = artist
                    } else {
                        binding.tvArtist.text = "$artist - $album"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        if (binder is AudioService.AudioBinder) {
            binding.btnCancel.setOnClickListener {
                binder.videoView.release()
                AudioService.stopService(context)
                dismiss()
            }

            val onStateChangeListener = object : OnStateChangeListener {
                override fun onStateChanged(state: Int) {
                    @SuppressLint("SetTextI18n")
                    when (state) {
                        Media3PlayState.STATE_BUFFERING -> {
                            binding.ibPause.isVisible = false
                            binding.progressBar.isVisible = true
                            binding.tvPosition.text = "正在连接媒体..."
                            progressJob?.cancel()
                        }

                        Media3PlayState.STATE_PREPARED -> {
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.seekBar.max = binder.videoView.getDuration().toInt()
                            binding.seekBar.progress = 0
                        }

                        Media3PlayState.STATE_PAUSED -> {
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.start()
                            }
                        }

                        Media3PlayState.STATE_ERROR -> {
                            toast("媒体播放错误：STATE_ERROR")
                            binding.tvPosition.text = "媒体播放错误：STATE_ERROR"
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.start()
                            }
                            progressJob?.cancel()
                        }

                        Media3PlayState.STATE_IDLE, Media3PlayState.STATE_ENDED -> {
                            binding.tvPosition.text = "00:00 / 00:00"
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.start()
                            }
                            progressJob?.cancel()
                        }

                        Media3PlayState.STATE_PLAYING -> {
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_pause_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.pause()
                            }

                            progressJob?.cancel()
                            progressJob = scope.launch {
                                while (isActive && binder.videoView.isPlaying()) {
                                    binding.seekBar.progress =
                                        binder.videoView.getCurrentPosition().toInt()
                                    binding.tvPosition.text =
                                        Media3PlayerUtils.stringForTime(binder.videoView.getCurrentPosition()) + " / " +
                                                Media3PlayerUtils.stringForTime(binder.videoView.getDuration())
                                    delay(1000)
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }

            binder.videoView.addOnStateChangeListener(onStateChangeListener)
            addOnDismissListener {
                progressJob?.cancel()
                binder.videoView.removeOnStateChangeListener(onStateChangeListener)
            }

            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                var isTracking = false

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    isTracking = true
                }

                @SuppressLint("SetTextI18n")
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        binding.tvPosition.text =
                            Media3PlayerUtils.stringForTime(progress.toLong()) + " / " +
                                    Media3PlayerUtils.stringForTime(seekBar.max.toLong())
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    isTracking = false
                    binder.videoView.seekTo(seekBar.progress.toLong())
                }
            })
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        dismiss()
    }
}