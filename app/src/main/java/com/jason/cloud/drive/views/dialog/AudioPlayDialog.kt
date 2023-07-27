package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.os.Build
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
    MediaDataController.OnPlayListener, MediaDataController.OnCompleteListener {
    private val videoView by lazy { VideoView(requireContext()) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var mediaInfoJob: Job? = null
    private val controller = MediaDataController.with("AudioPlayDialog")

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
            controller.previous()
        }
        binding.ibNext.setOnClickListener {
            controller.next()
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
                        PlayerUtils.stringForTime(progress) + " / " +
                                PlayerUtils.stringForTime(seekBar.max)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isTracking = false
                videoView.seekTo(seekBar.progress.toLong())
            }
        })

        val position = arguments?.getInt("position", 0) ?: 0
        val audioList = arguments?.getSerializableListExtraEx<FileEntity>("list")
        if (audioList != null) {
            prepareVideoView()
            controller.setVideoView(videoView)
            controller.addOnPlayListener(this)
            controller.start(position)
        }
    }

    fun setData(audioList: List<FileEntity>, position: Int): AudioPlayDialog {
        arguments?.putInt("position", position)
        arguments?.putSerializable("list", audioList as Serializable)
        controller.setData(arrayListOf<AudioEntity>().apply {
            audioList.forEach {
                add(AudioEntity(it.name, it.rawURL, it.thumbnailURL))
            }
        })
        return this
    }

    override fun onPlay(position: Int, videoData: VideoData) {
        if (videoData is AudioEntity) {
            binding.tvTitle.text = videoData.name
            binding.tvArtist.text = "未知艺术家"

            showImage(videoData.image)
            loadMediaInfo(videoData.url)
            videoView.release()
            videoView.setUrl(videoData.url)
            videoView.start()
        }
    }

    override fun onCompletion() {
        dismiss()
    }

    private fun prepareVideoView() {
        videoView.addOnStateChangeListener(object : SimpleOnStateChangeListener() {
            @SuppressLint("SetTextI18n")
            override fun onPlayStateChanged(playState: Int) {
                super.onPlayStateChanged(playState)
                if (playState == VideoView.STATE_BUFFERING || playState == VideoView.STATE_PREPARING) {
                    binding.ibPause.isVisible = false
                    binding.progressBar.isVisible = true
                } else {
                    binding.ibPause.isVisible = true
                    binding.progressBar.isVisible = false
                }

                if (playState != VideoView.STATE_PLAYING && playState != VideoView.STATE_BUFFERED) {
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                    binding.ibPause.setOnClickListener {
                        videoView.resume()
                    }
                } else {
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_pause_24)
                    binding.ibPause.setOnClickListener {
                        videoView.pause()
                    }
                }

                when (playState) {
                    VideoView.STATE_PREPARING -> {
                        binding.tvPosition.text = "正在连接媒体..."
                        progressJob?.cancel()
                    }

                    VideoView.STATE_PREPARED -> {
                        binding.seekBar.max = videoView.duration.toInt()
                        binding.seekBar.progress = 0
                    }

                    VideoView.STATE_ERROR -> {
                        toast("媒体播放错误：STATE_ERROR")
                        binding.tvPosition.text = "媒体播放错误：STATE_ERROR"
                        progressJob?.cancel()
                    }

                    VideoView.STATE_START_ABORT -> {
                        binding.tvPosition.text = "媒体播放错误：STATE_ERROR"
                        toast("媒体播放错误：STATE_START_ABORT")
                        progressJob?.cancel()
                    }

                    VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED -> {
                        binding.tvPosition.text = "00:00 / 00:00"
                        progressJob?.cancel()
                    }

                    VideoView.STATE_BUFFERING, VideoView.STATE_PLAYING -> {
                        progressJob?.cancel()
                        progressJob = scope.launch {
                            while (isActive && videoView.isPlaying) {
                                binding.seekBar.progress = videoView.currentPosition.toInt()
                                binding.tvPosition.text =
                                    PlayerUtils.stringForTime(videoView.currentPosition.toInt()) + " / " +
                                            PlayerUtils.stringForTime(videoView.duration.toInt())
                                delay(1000)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun showImage(image: String) {
        binding.ivCover.loadIMG(image) {
            placeholder(R.drawable.ic_default_audio_cover)
            centerCrop()
        }
        binding.ivBlurBackground.loadIMG(image) {
            blurCrop(15, 0.7f)
            placeholder(R.drawable.ic_default_blur_audio_background)
        }
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

    override fun onDestroy() {
        super.onDestroy()
        controller.removeOnPlayListener(this)
        controller.removeOnCompleteListener(this)
        videoView.release()
    }
}