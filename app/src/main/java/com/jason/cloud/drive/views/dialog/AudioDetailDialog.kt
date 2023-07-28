package com.jason.cloud.drive.views.dialog

import android.annotation.SuppressLint
import android.media.MediaMetadataRetriever
import android.view.View
import androidx.core.view.isVisible
import com.drake.net.utils.scopeNetLife
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jason.cloud.drive.R
import com.jason.cloud.drive.base.BaseBindBottomSheetDialogFragment
import com.jason.cloud.drive.databinding.LayoutAudioDetailDialogBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.extension.getSerializableListExtraEx
import com.jason.cloud.extension.glide.loadIMG
import com.jason.cloud.extension.toDateMinuteString
import com.jason.cloud.extension.toFileSizeString
import com.jason.cloud.extension.toast
import com.jason.videoview.extension.onPlayStateChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.doikki.videoplayer.player.BaseVideoView
import xyz.doikki.videoplayer.player.VideoView
import java.io.Serializable

class AudioDetailDialog :
    BaseBindBottomSheetDialogFragment<LayoutAudioDetailDialogBinding>(R.layout.layout_audio_detail_dialog) {
    fun setFileList(list: List<FileEntity>, position: Int): AudioDetailDialog {
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
        val fileList = arguments?.getSerializableListExtraEx<FileEntity>("list").orEmpty()
        if (position in fileList.indices) {
            val file = fileList[position]
            val videoView = initVideoView(file)

            binding.tvName.text = file.name
            binding.tvURL.text = file.path
            binding.tvInfo.text = file.size.toFileSizeString() + " / " +
                    file.date.toDateMinuteString()

            binding.ivAudioCover.loadIMG(file.thumbnailURL) {
                timeout(60000)
                placeholder(R.drawable.ic_default_audio_cover)
                addListener { _, _ ->
                    binding.progressBar.isVisible = false
                }
            }

            loadMediaInfo(file)
            binding.ibPause.setOnClickListener {
                videoView.setUrl(file.rawURL)
                videoView.start()
            }
            binding.btnPlay.setOnClickListener {
                videoView.setUrl(file.rawURL)
                videoView.start()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initVideoView(file: FileEntity): BaseVideoView<*> {
        return VideoView(requireContext()).apply {
            addOnDismissListener {
                release()
            }
            onPlayStateChanged { playState ->
                if (playState == VideoView.STATE_BUFFERING || playState == VideoView.STATE_PREPARING) {
                    binding.ibPause.isVisible = false
                    binding.progressBar.isVisible = true
                } else {
                    binding.ibPause.isVisible = true
                    binding.progressBar.isVisible = false
                    binding.btnPlay.alpha = 1f
                    binding.btnPlay.isEnabled = true
                }

                if (playState != VideoView.STATE_PLAYING && playState != VideoView.STATE_BUFFERED) {
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_play_arrow_24)
                } else {
                    binding.ibPause.isVisible = true
                    binding.ibPause.setImageResource(R.drawable.ic_round_pause_24)
                }

                when (playState) {
                    VideoView.STATE_BUFFERING, VideoView.STATE_PREPARING -> {
                        binding.btnPlay.text = "正在连接媒体..."
                        binding.btnPlay.alpha = 0.5f
                        binding.btnPlay.isEnabled = false
                    }

                    VideoView.STATE_PAUSED -> {
                        binding.btnPlay.text = "继续播放"
                        binding.btnPlay.setOnClickListener {
                            resume()
                        }
                        binding.ibPause.setOnClickListener {
                            resume()
                        }
                    }

                    VideoView.STATE_ERROR -> {
                        toast("媒体播放错误：STATE_ERROR")
                        binding.btnPlay.text = "媒体播放错误：STATE_ERROR"
                    }

                    VideoView.STATE_START_ABORT -> {
                        binding.btnPlay.text = "媒体播放错误：STATE_ERROR"
                        toast("媒体播放错误：STATE_START_ABORT")
                    }

                    VideoView.STATE_IDLE, VideoView.STATE_PLAYBACK_COMPLETED -> {
                        binding.btnPlay.text = "立即播放"
                        binding.btnPlay.setOnClickListener {
                            release()
                            setUrl(file.rawURL)
                            start()
                        }
                        binding.ibPause.setOnClickListener {
                            release()
                            setUrl(file.rawURL)
                            start()
                        }
                    }

                    VideoView.STATE_PLAYING -> {
                        binding.btnPlay.text = "暂停播放"
                        binding.btnPlay.setOnClickListener {
                            pause()
                        }
                        binding.ibPause.setOnClickListener {
                            pause()
                        }
                    }
                }
            }
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
}