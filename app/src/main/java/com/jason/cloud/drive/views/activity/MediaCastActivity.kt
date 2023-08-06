package com.jason.cloud.drive.views.activity

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuCompat
import androidx.core.view.isVisible
import com.jason.cast.DeviceListener
import com.jason.cast.MediaCaster
import com.jason.cast.exception.AVTransportServiceNotFoundException
import com.jason.cast.exception.DeviceExecuteException
import com.jason.cast.exception.DeviceNotSelectedException
import com.jason.cloud.drive.R
import com.jason.cloud.drive.adapter.MediaCastDeviceAdapter
import com.jason.cloud.drive.base.BaseBindActivity
import com.jason.cloud.drive.databinding.ActivityMediaCastBinding
import com.jason.cloud.drive.model.FileEntity
import com.jason.cloud.drive.views.dialog.TextDialog
import com.jason.cloud.extension.startActivity
import com.jason.cloud.extension.toMessage
import com.jason.cloud.extension.toast
import org.fourthline.cling.model.meta.RemoteDevice

class MediaCastActivity : BaseBindActivity<ActivityMediaCastBinding>(R.layout.activity_media_cast),
    DeviceListener {
    private lateinit var caster: MediaCaster
    private val adapter = MediaCastDeviceAdapter().apply {
        addOnClickObserver { _, item, _ ->
            selectDevice(item)
        }
    }

    companion object {
        private var position: Int = 0
        private var videoDataList: ArrayList<FileEntity> = arrayListOf()

        fun start(context: Context?, videoList: List<FileEntity>, position: Int = 0) {
            this.position = position
            this.videoDataList.clear()
            this.videoDataList.addAll(videoList)
            context.startActivity(MediaCastActivity::class)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initView() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvDevices.adapter = adapter

        adapter.setHeaderView(View.inflate(context, R.layout.item_cast_device_finder, null))
        adapter.notifyDataSetChanged()

        caster = MediaCaster(this)
        caster.setDeviceListener(this)
        caster.start()
    }

    private fun selectDevice(device: RemoteDevice) {
        if (caster.getSelectDevice() != device) {
            caster.selectDevice(device)
            play(videoDataList[position].name, videoDataList[position].rawURL)

            binding.toolbar.menu.findItem(R.id.select)?.isVisible = videoDataList.size > 1
            binding.toolbar.findViewById<View>(R.id.select)?.also {
                it.isVisible = videoDataList.size > 1
                it.setOnClickListener { anchor ->
                    showVideoSelectMenu(anchor)
                }
            }
        }
    }

    private var isPaused = false

    private fun pauseOrResume() {
        if (isPaused) {
            resume()
        } else {
            pause()
        }
    }

    private fun pause() {
        caster.pause { succeed, error ->
            if (succeed.not()) {
                actionResultListener.invoke(false, error)
            } else {
                isPaused = true
                runOnUiThread {
                    binding.toolbar.menu.findItem(R.id.play)
                        ?.setIcon(R.drawable.ic_round_play_arrow_24)
                }
            }
        }
    }

    private fun resume() {
        caster.play { succeed, error ->
            if (succeed.not()) {
                actionResultListener.invoke(false, error)
            } else {
                isPaused = false
                runOnUiThread {
                    binding.toolbar.menu.findItem(R.id.play)?.setIcon(R.drawable.ic_round_pause_24)
                }
            }
        }
    }

    private fun play(name: String, url: String) {
        binding.toolbar.menu.findItem(R.id.play)?.isVisible = false
        caster.post(name, url) { succeed, error ->
            if (succeed.not()) {
                actionResultListener.invoke(false, error)
                runOnUiThread {
                    binding.toolbar.menu.findItem(R.id.play)?.isVisible = false
                }
            } else {
                isPaused = false
                runOnUiThread {
                    toast("媒体投送成功")
                    binding.toolbar.menu.findItem(R.id.play)?.also {
                        it.isVisible = true
                        it.setIcon(R.drawable.ic_round_pause_24)
                        it.setOnMenuItemClickListener {
                            pauseOrResume()
                            true
                        }
                    }

                    binding.toolbar.menu.findItem(R.id.stop)?.also {
                        it.isVisible = true
                        it.setOnMenuItemClickListener {
                            caster.stop { succeed, error ->
                                if (succeed.not()) {
                                    actionResultListener.invoke(false, error)
                                } else {
                                    runOnUiThread {
                                        binding.toolbar.menu.findItem(R.id.stop)?.isVisible = false
                                        binding.toolbar.menu.findItem(R.id.play)?.isVisible = false
                                    }
                                }
                            }
                            true
                        }
                    }
                }
            }
        }
    }

    private fun showVideoSelectMenu(anchor: View) {
        PopupMenu(context, anchor).apply {
            MenuCompat.setGroupDividerEnabled(this.menu, true)
            videoDataList.forEachIndexed { index, extendVideoData ->
                val item = menu.add(0, index, index, extendVideoData.name)
                item.isCheckable = true
                item.isChecked = index == position
                item.setOnMenuItemClickListener {
                    position = index
                    play(videoDataList[position].name, videoDataList[position].rawURL)
                    true
                }
            }
            menu.add(1, videoDataList.size, videoDataList.size, "取消")
        }.show()
    }


    private val actionResultListener = { succeed: Boolean, error: Exception? ->
        runOnUiThread {
            if (succeed) {
                toast("操作执行成功")
            } else {
                if (error is DeviceExecuteException) {
                    TextDialog(this).setTitle("设备执行请求失败").setText(error.toMessage())
                        .onPositive("确定").show()
                }
                if (error is DeviceNotSelectedException) {
                    toast("请先选择投屏设备！")
                }
                if (error is AVTransportServiceNotFoundException) {
                    toast("搜寻媒体设备失败！")
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onDeviceFound(device: RemoteDevice) {
        runOnUiThread {
            if (adapter.getData().contains(device).not()) {
                adapter.addData(device)
                adapter.getData().sortedBy { it.details.friendlyName }
                adapter.notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onDeviceRemoved(device: RemoteDevice) {
        runOnUiThread {
            if (adapter.getData().contains(device)) {
                adapter.removeData(device)
                adapter.getData().sortedBy { it.details.friendlyName }
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        caster.release()
        videoDataList.clear()
    }
}