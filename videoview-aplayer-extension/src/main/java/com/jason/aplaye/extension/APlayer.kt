package com.jason.aplaye.extension

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import com.aplayer.APlayerAndroid
import xyz.doikki.videoplayer.model.Track
import xyz.doikki.videoplayer.player.AbstractPlayer
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.L


class APlayer(val context: Context, private val enableToast: Boolean) : AbstractPlayer() {
    private var path: String = ""
    private var isLooping = false
    private lateinit var aPlayer: APlayerAndroid

    override fun initPlayer() {
        aPlayer = APlayerAndroid()
        L.d("version: ${APlayerAndroid.getVersion()}")
        setOptions()

        aPlayer.setOnFirstFrameRenderListener {
            mPlayerEventListener.onInfo(MEDIA_INFO_RENDERING_START, 0)
        }

        aPlayer.setOnSeekCompleteListener {
            mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_END, 0)
        }

        aPlayer.setOnBufferListener { progress ->
            L.d("onBuffer: $progress")
            if (progress == 0) {
                mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_START, progress)
            }
            if (progress == 100) {
                mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_END, progress)
            }
        }

        aPlayer.setOnOpenCompleteListener { isOpenSuccess ->
            if (isOpenSuccess) {
                mPlayerEventListener.onVideoSizeChanged(aPlayer.videoWidth, aPlayer.videoHeight)
                mPlayerEventListener.onPrepared()
                mPlayerEventListener.onInfo(MEDIA_INFO_RENDERING_START, 0)
                //获取当前的视频和播放器是否可以支持hdr原彩显示
                L.d("HDR_HAVE = ${aPlayer.getConfig(APlayerAndroid.CONFIGID.HDR_HAVE)}")
                L.d("HW_DECODER_USE = ${aPlayer.getConfig(APlayerAndroid.CONFIGID.HW_DECODER_USE)}")

            } else {
                mPlayerEventListener.onError()
            }
        }

        aPlayer.setOnPlayCompleteListener { playRet ->
            when (playRet) {
                APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_COMPLETE -> {
                    L.d("onPlayComplete: COMPLETE")
                    if (isLooping) {
                        prepareAsync()
                    } else {
                        mPlayerEventListener.onCompletion()
                    }
                }

                APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_OPENRROR -> {
                    L.d("onPlayComplete: OPEN_ERROR")
                    mPlayerEventListener.onError()
                    if (enableToast) {
                        Toast.makeText(context, "OPEN_ERROR", Toast.LENGTH_SHORT).show()
                    }
                }

                APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_HARDDECODERROR -> {
                    L.d("onPlayComplete: HARD_DECODE_ERROR")
                    mPlayerEventListener.onError()
                    if (enableToast) {
                        Toast.makeText(context, "HARD_DECODE_ERROR", Toast.LENGTH_SHORT).show()
                    }
                }

                APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_SEEKERROR -> {
                    L.d("onPlayComplete: SEEK_ERROR")
                    //mPlayerEventListener.onError()
                    if (enableToast) {
                        Toast.makeText(context, "SEEK_ERROR", Toast.LENGTH_SHORT).show()
                    }
                }

                APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_READEFRAMERROR -> {
                    L.d("onPlayComplete: READ_FRAME_ERROR")
                    mPlayerEventListener.onError()
                    if (enableToast) {
                        Toast.makeText(context, "READ_FRAME_ERROR", Toast.LENGTH_SHORT).show()
                    }
                }

                APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_CREATEGRAPHERROR -> {
                    L.d("onPlayComplete: CREATE_GRAPH_ERROR")
                    mPlayerEventListener.onError()
                    if (enableToast) {
                        Toast.makeText(context, "CREATE_GRAPH_ERROR", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun setOptions() {
        aPlayer.setConfig(APlayerAndroid.CONFIGID.HTTP_USER_AHTTP2, "1")
        //Dolby-AC3解码开启
        aPlayer.setConfig(APlayerAndroid.CONFIGID.AUDIO_COPYRIGHT_CHECK, "0")
        aPlayer.setConfig(APlayerAndroid.CONFIGID.AUDIO_COPYRIGHT_DISABLE, "1")

        //是否开启硬件解码侦测
        aPlayer.setConfig(APlayerAndroid.CONFIGID.HW_DECODER_DETEC, "1")
        //获取/设置硬件解码状态，默认关闭。
        aPlayer.setConfig(APlayerAndroid.CONFIGID.HW_DECODER_USE, "1")
        //准备完后自动播放
        aPlayer.setConfig(APlayerAndroid.CONFIGID.AUTO_PLAY, "1")

        //是否开启画质增强选项
        aPlayer.setConfig(APlayerAndroid.CONFIGID.IMAGE_NORMALIZE_ENABLE, "1")

        aPlayer.setConfig(APlayerAndroid.CONFIGID.SUBTITLE_SHOW, "1")
        //播放库是否可以渲染字幕 1为是 0为否
        aPlayer.setConfig(APlayerAndroid.CONFIGID.SUBTITLE_ENGINE_RENDER, "1")

        aPlayer.setConfig(APlayerAndroid.CONFIGID.STRETCH_MODE, "2")


        //APlayerAndroid.CONFIGID.HDR_HAVE
        //获取当前的视频和播放器是否可以支持hdr原彩显示,“1”表示当前的状态是可以支持hdr原彩显示或者SDR显示的并且播放的视频也为HDR视频。

        //支持hdr视频的显示，可能显示为SDR或者HDR原彩显示。
        aPlayer.setConfig(APlayerAndroid.CONFIGID.HDR_ALLOW, "1")
        //HDR_ALLOW，HDR_HAVE皆为1时即选择渲染器。为1时为hdr播放 为0时为sdr播放
        aPlayer.setConfig(APlayerAndroid.CONFIGID.HDR_ENABLE, "1")
        //对于某些视频其为HDR视频但是播放器无法检测起为HDR，故用服务端返回的hdr信息来启用hdrforce强制开启HDR模式，每次要在open之前设置。
        aPlayer.setConfig(APlayerAndroid.CONFIGID.HDR_FORCE, "1")
        //该值为TV 迅雷专属，强制开启HDR，SurfaceView 的渲染模式，提高帧率。
        aPlayer.setConfig(APlayerAndroid.CONFIGID.TV_MODE, "1")
    }

    override fun setDataSource(path: String, headers: MutableMap<String, String>?) {
        this.path = path
        val sb = StringBuilder()
        headers?.entries?.forEach { entry ->
            if (entry.key == "User-Agent") {
                aPlayer.setConfig(APlayerAndroid.CONFIGID.HTTP_USER_AGENT, entry.value)
            } else {
                sb.append(entry.key).append(":").append(entry.value).append("\r\n")
            }
        }
        aPlayer.setConfig(APlayerAndroid.CONFIGID.HTTP_CUSTOM_HEADERS, sb.toString())
    }

    override fun setDataSource(fd: AssetFileDescriptor?) {}

    override fun start() {
        aPlayer.play()
    }

    override fun pause() {
        aPlayer.pause()
    }

    override fun stop() {}

    override fun prepareAsync() {
        if (path.isNotBlank()) {
            aPlayer.open(path)
        }
    }

    override fun reset() {
    }

    override fun isPlaying(): Boolean {
        return aPlayer.state == APlayerAndroid.PlayerState.APLAYER_PLAYING
    }

    override fun seekTo(time: Long) {
        mPlayerEventListener.onInfo(MEDIA_INFO_BUFFERING_START, 0)
        aPlayer.position = time.toInt()
    }

    override fun release() {
        aPlayer.close()
        aPlayer.destroy()
    }

    override fun getCurrentPosition(): Long {
        return aPlayer.position.toLong()
    }

    override fun getDuration(): Long {
        return aPlayer.duration.toLong()
    }

    override fun getBufferedPercentage(): Int {
        val strPos: String = aPlayer.getConfig(APlayerAndroid.CONFIGID.READPOSITION)
        val duration = duration
        return if (duration == 0L) 0 else (strPos.toInt() * 100 / duration).toInt()
    }

    override fun setSurface(surface: Surface) {
        aPlayer.setView(surface)
    }

    override fun setDisplay(holder: SurfaceHolder) {
        aPlayer.setView(holder.surface)
    }

    override fun setVolume(v1: Float, v2: Float) {
        aPlayer.volume = v1.toInt() * 100
        L.e("setVolume: v1 = $v1,v2 = $v2")
    }

    override fun setLooping(isLooping: Boolean) {
        this.isLooping = isLooping
    }

    override fun setSpeed(speed: Float) {
        aPlayer.setConfig(APlayerAndroid.CONFIGID.PLAY_SPEED, (speed * 100).toInt().toString())
    }

    override fun getSpeed(): Float {
        val strSpeed = aPlayer.getConfig(APlayerAndroid.CONFIGID.PLAY_SPEED)
        val fSpeed: Float = try {
            strSpeed.toFloat()
        } catch (e: NumberFormatException) {
            100f
        }
        return fSpeed / 100
    }

    override fun getTcpSpeed(): Long {
        return aPlayer.getConfig(APlayerAndroid.CONFIGID.DOWN_SPEED).toLong()
    }

    override fun getSubtitles(): List<Track> {
        val subtitle = aPlayer.getConfig(APlayerAndroid.CONFIGID.SUBTITLE_LANGLIST)
        if (subtitle.isBlank()) return emptyList()
        return ArrayList<Track>().apply {
            subtitle.split(";").forEachIndexed { index, s ->
                add(Track(index, s))
                L.e("subtitle: $s")
            }
        }
    }

    override fun selectSubtitle(track: Track) {
        aPlayer.setConfig(APlayerAndroid.CONFIGID.SUBTITLE_CURLANG, track.index.toString())
    }

    override fun getSelectedSubtitleIndex(): Int {
        return aPlayer.getConfig(APlayerAndroid.CONFIGID.SUBTITLE_CURLANG).toInt()
    }

    override fun getAudioTracks(): List<Track> {
        val trackList = aPlayer.getConfig(APlayerAndroid.CONFIGID.AUDIO_TRACK_LIST)
        if (trackList.isBlank()) return emptyList()
        return ArrayList<Track>().apply {
            trackList.split(";").forEachIndexed { index, s ->
                add(Track(index, s))
                L.e("track: $s")
            }
        }
    }

    override fun selectAudioTrack(track: Track) {
        aPlayer.setConfig(APlayerAndroid.CONFIGID.AUDIO_TRACK_CURRENT, track.index.toString())
    }

    override fun getSelectedAudioTrackIndex(): Int {
        return aPlayer.getConfig(APlayerAndroid.CONFIGID.AUDIO_TRACK_LIST).toInt()
    }

    override fun setScreenScale(scale: Int) {
        super.setScreenScale(scale)
        Log.i(
            "Aplayer",
            "setScreenScale: mCurrentScreenScaleType = $scale"
        )
        Handler(Looper.getMainLooper()).postDelayed({ applyScale(scale) }, 200)
    }

    private fun applyScale(scale: Int) {
        when (scale) {
            VideoView.SCREEN_SCALE_DEFAULT -> aPlayer.setConfig(
                APlayerAndroid.CONFIGID.STRETCH_MODE,
                "2"
            )

            VideoView.SCREEN_SCALE_ORIGINAL -> aPlayer.setConfig(
                APlayerAndroid.CONFIGID.STRETCH_MODE,
                "2"
            )

            VideoView.SCREEN_SCALE_16_9 -> {
                aPlayer.setConfig(
                    APlayerAndroid.CONFIGID.STRETCH_MODE,
                    "2"
                )
            }

            VideoView.SCREEN_SCALE_4_3 -> {
                aPlayer.setConfig(
                    APlayerAndroid.CONFIGID.STRETCH_MODE,
                    "2"
                )
            }

            VideoView.SCREEN_SCALE_MATCH_PARENT -> aPlayer.setConfig(
                APlayerAndroid.CONFIGID.STRETCH_MODE,
                "2"
            )

            VideoView.SCREEN_SCALE_CENTER_CROP -> aPlayer.setConfig(
                APlayerAndroid.CONFIGID.STRETCH_MODE,
                "1"
            )
        }
    }

    override fun isVideo(): Boolean {
        return try {
            aPlayer.getConfig(APlayerAndroid.CONFIGID.STREAM_TYPE).let {
                Integer.toBinaryString(it.toInt())
            }.toString()[1].toString() == "2"
        } catch (e: Exception) {
            true
        }
    }
}