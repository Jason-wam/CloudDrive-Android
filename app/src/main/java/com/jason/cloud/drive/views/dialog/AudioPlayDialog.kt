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
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.interfaces.OnMediaItemTransitionListener
import com.jason.cloud.media3.interfaces.OnPlayCompleteListener
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.model.Media3Item
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.utils.PlayerUtils
import com.jason.cloud.media3.widget.Media3AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable

class AudioPlayDialog :
    BaseBindBottomSheetDialogFragment<LayoutAudioPlayerDialogBinding>(R.layout.layout_audio_player_dialog),
    OnPlayCompleteListener, OnMediaItemTransitionListener, OnStateChangeListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var mediaInfoJob: Job? = null
    private var audioPlayer: Media3AudioPlayer? = null

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

        val position = arguments?.getInt("position", 0) ?: 0
        val audioList = arrayListOf<Media3Item>()
        arguments?.getSerializableListExtraEx<File>("files")?.let {
            audioList.addAll(ArrayList<Media3Item>().apply {
                it.forEach {
                    add(Media3Item().apply {
                        this.url = it.absolutePath
                        this.title = it.name
                    })
                }
            })
        }
        arguments?.getSerializableListExtraEx<FileEntity>("list")?.let {
            audioList.addAll(ArrayList<Media3Item>().apply {
                it.forEach {
                    add(Media3Item().apply {
                        this.url = it.rawURL
                        this.title = it.name
                        this.image = it.thumbnailURL
                    })
                }
            })
        }

        AudioService.launchWith(requireContext(), audioList, position) {
            context?.bindService(this, serviceConnection, 0)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setFiles(audioList: List<File>, position: Int): AudioPlayDialog {
        arguments?.putInt("position", position)
        arguments?.putSerializable("files", audioList as Serializable)
        return this
    }

    fun setFileEntities(audioList: List<FileEntity>, position: Int): AudioPlayDialog {
        arguments?.putInt("position", position)
        arguments?.putSerializable("list", audioList as Serializable)
        return this
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder is AudioService.AudioBinder) {
                audioPlayer = binder.player
                binder.player.getCurrentMedia3Item()?.let {
                    loadMediaInfo(it)
                }
                binder.player.addOnStateChangeListener(this@AudioPlayDialog)
                binder.player.addOnPlayCompleteListener(this@AudioPlayDialog)
                binder.player.addOnMediaItemTransitionListener(this@AudioPlayDialog)
                addOnDismissListener {
                    progressJob?.cancel()
                    binder.player.removeOnStateChangeListener(this@AudioPlayDialog)
                    binder.player.removeOnPlayCompleteListener(this@AudioPlayDialog)
                    binder.player.removeOnMediaItemTransitionListener(this@AudioPlayDialog)
                }

                binding.seekBar.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    var isTracking = false

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        isTracking = true
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            binding.tvPosition.text =
                                PlayerUtils.stringForTime(progress.toLong()) + " / " +
                                        PlayerUtils.stringForTime(seekBar.max.toLong())
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        isTracking = false
                        binder.player.seekTo(seekBar.progress.toLong())
                    }
                })

                binding.ibPrevious.setOnClickListener {
                    if (binder.player.hasPreviousMediaItem()) {
                        binder.player.seekToPrevious()
                        binder.player.prepare()
                        binder.player.start()
                    } else {
                        toast("已经是第一个啦")
                    }
                }
                binding.ibNext.setOnClickListener {
                    if (binder.player.hasNextMediaItem()) {
                        binder.player.seekToNext()
                        binder.player.prepare()
                        binder.player.start()
                    } else {
                        toast("已经是最后一个啦")
                    }
                }

                binding.btnCancel.setOnClickListener {
                    binder.player.release()
                    AudioService.stopService(context)
                    dismiss()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioPlayer = null
            dismiss()
        }
    }

    override fun onStateChanged(state: Int) {
        if (audioPlayer != null) {
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
                    binding.seekBar.max = audioPlayer!!.getDuration().toInt()
                    binding.seekBar.progress = 0
                }

                Media3PlayState.STATE_PAUSED -> {
                    binding.progressBar.isVisible = false
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                    binding.ibPause.setOnClickListener {
                        audioPlayer!!.start()
                    }
                }

                Media3PlayState.STATE_ERROR -> {
                    toast("媒体播放错误：STATE_ERROR")
                    binding.tvPosition.text = "媒体播放错误：STATE_ERROR"
                    binding.progressBar.isVisible = false
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                    binding.ibPause.setOnClickListener {
                        audioPlayer!!.start()
                    }
                    progressJob?.cancel()
                }

                Media3PlayState.STATE_IDLE, Media3PlayState.STATE_ENDED -> {
                    binding.tvPosition.text = "00:00 / 00:00"
                    binding.progressBar.isVisible = false
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                    binding.ibPause.setOnClickListener {
                        audioPlayer!!.start()
                    }
                    progressJob?.cancel()
                }

                Media3PlayState.STATE_PLAYING -> {
                    binding.progressBar.isVisible = false
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_pause_24)
                    binding.ibPause.setOnClickListener {
                        audioPlayer!!.pause()
                    }

                    progressJob?.cancel()
                    progressJob = scope.launch {
                        while (isActive && audioPlayer!!.isPlaying()) {
                            binding.seekBar.progress =
                                audioPlayer!!.getCurrentPosition().toInt()
                            binding.tvPosition.text =
                                PlayerUtils.stringForTime(audioPlayer!!.getCurrentPosition()) + " / " +
                                        PlayerUtils.stringForTime(audioPlayer!!.getDuration())
                            delay(1000)
                        }
                    }
                }

                else -> {}
            }
        }
    }

    override fun onTransition(index: Int, item: Media3Item) {
        loadMediaInfo(item)
    }

    @SuppressLint("SetTextI18n")
    private fun loadMediaInfo(item: Media3Item) {
        binding.tvTitle.text = item.title
        binding.tvArtist.text = "未知艺术家"
        binding.ivCover.loadIMG(item.image) {
            placeholder(R.drawable.ic_default_audio_cover)
            centerCrop()
        }

        binding.btnBackground.alpha = 1f
        binding.btnBackground.isEnabled = true
        binding.btnBackground.setOnClickListener {
            dismiss()
        }

        mediaInfoJob?.cancel()
        mediaInfoJob = scope.launch(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(item.url, HashMap<String, String>())
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

    override fun onCompletion() {
        dismiss()
    }
}