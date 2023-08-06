package com.jason.cast

import org.fourthline.cling.model.meta.RemoteDevice

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-25 15:28
 * @Description: TODO
 */
interface DeviceListener {
    fun onDeviceFound(device: RemoteDevice)
    fun onDeviceRemoved(device: RemoteDevice)
}