package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.drake.net.utils.scopeNetLife
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutDetailAudioDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.toDateMinuteString
import com.jason.cloud.extension.toFileSizeString
import com.jason.cloud.extension.toast
import com.jason.cloud.media3.interfaces.OnStateChangeListener
import com.jason.cloud.media3.utils.Media3PlayState
import com.jason.cloud.media3.widget.Media3AudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable

class DetailAudioDialog :
    BaseBindBottomSheetDialogFragment<LayoutDetailAudioDialogBinding>(R.layout.layout_detail_audio_dialog),
    OnStateChangeListener {
    private val videoView by lazy { Media3AudioPlayer(requireContext()) }

    fun setFileList(list: List<FileEntity>, position: Int): DetailAudioDialog {
        arguments?.putSerializable("list", list as Serializable)
        arguments?.putInt("position", position)
        return this
    }

    @SuppressLint("SetTextI18n")
    override fun initView(view: View) {
        super.initView(view)
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.handler.isSelected = slideOffset != 0f
            }
        })

        val position = arguments?.getInt("position") ?: 0
        val fileList = arguments?.getSerializableListExtraEx<FileEntity>("list") ?: return
        val file = fileList[position]

        videoView.addOnStateChangeListener(this)
        videoView.setDataSource(file.rawURL)

        loadMediaInfo(file)
        addOnDismissListener {
            videoView.release()
        }

        binding.ivAudioCover.loadIMG(file.thumbnailURL) {
            timeout(60000)
            placeholder(R.drawable.ic_default_audio_cover)
            addListener { _, _ ->
                binding.progressBar.isVisible = false
            }
        }

        binding.tvName.text = file.name
        binding.tvURL.text = file.path
        binding.tvDate.text = file.date.toDateMinuteString()
        binding.tvSize.text = file.size.toFileSizeString()

        binding.ibPause.setOnClickListener {
            videoView.prepare()
            videoView.start()
        }

        binding.btnPlay.setOnClickListener {
            videoView.prepare()
            videoView.start()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadMediaInfo(file: FileEntity) = scopeNetLife(dispatcher = Dispatchers.IO) {
        var title = ""
        var album = ""
        var artist = ""
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.rawURL, HashMap<String, String>())
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
            withContext(Dispatchers.Main) {
                binding.tvAudioName.text = title.ifBlank { file.name }
                if (artist.isBlank()) {
                    binding.tvAudioArtist.text = "未知艺术家"
                } else {
                    if (album.isBlank()) {
                        binding.tvAudioArtist.text = artist
                    } else {
                        binding.tvAudioArtist.text = "$artist - $album"
                    }
                }
            }
        }
    }

    override fun onStateChanged(state: Int) {
        Log.e("AudioDetailDialog", "onNewPlayState = $state")
        if (state == Media3PlayState.STATE_BUFFERING) {
            binding.ibPause.isVisible = false
            binding.progressBar.isVisible = true
        } else {
            binding.ibPause.isVisible = true
            binding.progressBar.isVisible = false
            binding.btnPlay.alpha = 1f
            binding.btnPlay.isEnabled = true
        }

        binding.ibPause.isVisible = true
        binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)

        when (state) {
            Media3PlayState.STATE_BUFFERING -> {
                binding.btnPlay.text = "正在连接媒体..."
                binding.btnPlay.alpha = 0.5f
                binding.btnPlay.isEnabled = false
            }

            Media3PlayState.STATE_PAUSED -> {
                binding.btnPlay.text = "继续播放"
                binding.btnPlay.setOnClickListener {
                    videoView.start()
                }
                binding.ibPause.setOnClickListener {
                    videoView.start()
                }
            }

            Media3PlayState.STATE_ERROR -> {
                toast("媒体播放错误：STATE_ERROR")
                binding.btnPlay.text = "媒体播放错误：STATE_ERROR"
            }

            Media3PlayState.STATE_IDLE, Media3PlayState.STATE_ENDED -> {
                binding.btnPlay.text = "立即播放"
                binding.btnPlay.setOnClickListener {
                    videoView.start()
                }
                binding.ibPause.setOnClickListener {
                    videoView.start()
                }
            }

            Media3PlayState.STATE_PLAYING -> {
                binding.ibPause.isVisible = true
                binding.ibPause.setImageResource(R.drawable.ic_round_pause_24)
                binding.ibPause.setOnClickListener {
                    videoView.pause()
                }

                binding.btnPlay.text = "暂停播放"
                binding.btnPlay.setOnClickListener {
                    videoView.pause()
                }
            }
        }
    }
}