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
import com.jason.cloud.drive.model.AudioEntity
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.service.AudioService
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.toast
import com.jason.videoview.controller.MediaDataController
import com.jason.videoview.model.VideoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.doikki.videoplayer.player.BaseVideoView.SimpleOnStateChangeListener
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils
import java.io.Serializable

class AudioPlayDialog :
    BaseBindBottomSheetDialogFragment<LayoutAudioPlayerDialogBinding>(R.layout.layout_audio_player_dialog),
    MediaDataController.OnCompleteListener, ServiceConnection, MediaDataController.OnPlayListener {

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

        MediaDataController.with("AudioService").addOnPlayListener(this)
        addOnDismissListener {
            MediaDataController.with("AudioService").removeOnPlayListener(this)
        }

        val position = arguments?.getInt("position", 0) ?: 0
        val audioList = arguments?.getSerializableListExtraEx<FileEntity>("list")?.let {
            ArrayList<AudioEntity>().apply {
                it.forEach {
                    add(AudioEntity(it.name, it.rawURL, it.thumbnailURL))
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
        if (MediaDataController.with("AudioService").hasPrevious()) {
            MediaDataController.with("AudioService").previous()
        } else {
            toast("已经是第一个啦")
        }
    }

    private fun next() {
        if (MediaDataController.with("AudioService").hasNext()) {
            MediaDataController.with("AudioService").next()
        } else {
            toast("已经是最后一个啦")
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

            val onStateChangeListener = object : SimpleOnStateChangeListener() {
                @SuppressLint("SetTextI18n")
                override fun onPlayStateChanged(playState: Int) {
                    super.onPlayStateChanged(playState)
                    when (playState) {
                        VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> {
                            binding.ibPause.isVisible = false
                            binding.progressBar.isVisible = true
                            binding.tvPosition.text = "正在连接媒体..."
                            progressJob?.cancel()
                        }

                        VideoView.STATE_PREPARED -> {
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.seekBar.max = binder.videoView.duration.toInt()
                            binding.seekBar.progress = 0
                        }

                        VideoView.STATE_PAUSED -> {
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.resume()
                            }
                        }

                        VideoView.STATE_ERROR, VideoView.STATE_START_ABORT -> {
                            toast("媒体播放错误：STATE_ERROR")
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.start()
                            }
                            binding.tvPosition.text = "媒体播放错误：STATE_ERROR"
                            progressJob?.cancel()
                        }

                        VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED -> {
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.start()
                            }
                            binding.tvPosition.text = "00:00 / 00:00"
                            progressJob?.cancel()
                        }

                        VideoView.STATE_PLAYING -> {
                            binding.progressBar.isVisible = false
                            binding.ibPause.isVisible = true
                            binding.ibPause.setImageResource(R.drawable.ic_round_pause_24)
                            binding.ibPause.setOnClickListener {
                                binder.videoView.pause()
                            }

                            progressJob?.cancel()
                            progressJob = scope.launch {
                                while (isActive && binder.videoView.isPlaying) {
                                    binding.seekBar.progress =
                                        binder.videoView.currentPosition.toInt()
                                    binding.tvPosition.text =
                                        PlayerUtils.stringForTime(binder.videoView.currentPosition.toInt()) + " / " +
                                                PlayerUtils.stringForTime(binder.videoView.duration.toInt())
                                    delay(1000)
                                }
                            }
                        }
                    }
                }
            }


            binder.videoView.addOnStateChangeListener(onStateChangeListener)
            addOnDismissListener {
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
                        binding.tvPosition.text = PlayerUtils.stringForTime(progress) + " / " +
                                PlayerUtils.stringForTime(seekBar.max)
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
        toast("播放停止")
        dismiss()
    }

    override fun onPlay(position: Int, videoData: VideoData) {
        if (videoData is AudioEntity) {
            binding.tvTitle.text = videoData.name
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

            loadMediaInfo(videoData.url)
        }
    }
}