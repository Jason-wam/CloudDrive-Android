package com.jason.cast

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.jason.cast.exception.AVTransportServiceNotFoundException
import com.jason.cast.exception.DeviceExecuteException
import com.jason.cast.exception.DeviceNotSelectedException
import com.jason.cast.util.MetadataUtil
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.message.header.DeviceTypeHeader
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.DeviceType
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import org.fourthline.cling.support.avtransport.callback.Pause
import org.fourthline.cling.support.avtransport.callback.Play
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI
import org.fourthline.cling.support.avtransport.callback.Stop

/**
 * @Author: 进阶的面条
 * @Date: 2022-02-24 18:58
 * @Description: TODO
 */
class MediaCaster(private val context: Context) {
    private var service: AndroidUpnpService? = null
    private var controlPoint: ControlPoint? = null
    private var selectedDevice: RemoteDevice? = null
    private var deviceListener: DeviceListener? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AndroidUpnpService) {
                this@MediaCaster.service = service

                controlPoint = service.controlPoint
                service.registry.addListener(registryListener)

                val type = DeviceType("schemas-upnp-org", "MediaRenderer", 1)
                controlPoint?.search(DeviceTypeHeader(type))
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    fun start() {
        val intent = Intent(context, AndroidUpnpServiceImpl::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun selectDevice(device: RemoteDevice) {
        this.selectedDevice = device
    }

    fun getSelectDevice(): RemoteDevice? {
        return selectedDevice
    }

    fun post(
        name: String,
        url: String,
        listener: ((succeed: Boolean, error: Exception?) -> Unit)? = null
    ) {
        try {
            if (selectedDevice == null) {
                listener?.invoke(false, DeviceNotSelectedException())
                return
            }
            val service = selectedDevice?.findService(UDAServiceType("AVTransport"))
            if (service == null) {
                listener?.invoke(false, AVTransportServiceNotFoundException())
                return
            }
            val metadata: String =
                MetadataUtil.createMetadata(url, "id", name, MetadataUtil.Type.VIDEO)
            controlPoint?.execute(object : SetAVTransportURI(service, url, metadata) {
                override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                    super.success(invocation)
                    listener?.invoke(true, null)
                }

                override fun failure(
                    invocation: ActionInvocation<out Service<*, *>>?,
                    operation: UpnpResponse?,
                    defaultMsg: String
                ) {
                    listener?.invoke(false, DeviceExecuteException(defaultMsg))
                }
            })
        } catch (e: Exception) {
            listener?.invoke(false, e)
            e.printStackTrace()
        }
    }

    fun play(listener: ((succeed: Boolean, error: Exception?) -> Unit)? = null) {
        try {
            if (selectedDevice == null) {
                listener?.invoke(false, DeviceNotSelectedException())
                return
            }
            val service = selectedDevice?.findService(UDAServiceType("AVTransport"))
            if (service == null) {
                listener?.invoke(false, AVTransportServiceNotFoundException())
                return
            }
            controlPoint?.execute(object : Play(service) {
                override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                    super.success(invocation)
                    listener?.invoke(true, null)
                }

                override fun failure(
                    invocation: ActionInvocation<out Service<*, *>>?,
                    operation: UpnpResponse?,
                    defaultMsg: String
                ) {
                    listener?.invoke(false, DeviceExecuteException(defaultMsg))
                }
            })
        } catch (e: Exception) {
            listener?.invoke(false, e)
            e.printStackTrace()
        }
    }

    fun pause(listener: ((succeed: Boolean, error: Exception?) -> Unit)? = null) {
        try {
            if (selectedDevice == null) {
                listener?.invoke(false, DeviceNotSelectedException())
                return
            }
            val service = selectedDevice?.findService(UDAServiceType("AVTransport"))
            if (service == null) {
                listener?.invoke(false, AVTransportServiceNotFoundException())
                return
            }
            controlPoint?.execute(object : Pause(service) {
                override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                    super.success(invocation)
                    listener?.invoke(true, null)
                }

                override fun failure(
                    invocation: ActionInvocation<out Service<*, *>>?,
                    operation: UpnpResponse?,
                    defaultMsg: String
                ) {
                    listener?.invoke(false, DeviceExecuteException(defaultMsg))
                }
            })
        } catch (e: Exception) {
            listener?.invoke(false, e)
            e.printStackTrace()
        }
    }

    fun stop(listener: ((succeed: Boolean, error: Exception?) -> Unit)? = null) {
        try {
            if (selectedDevice == null) {
                listener?.invoke(false, DeviceNotSelectedException())
                return
            }
            val service = selectedDevice?.findService(UDAServiceType("AVTransport"))
            if (service == null) {
                listener?.invoke(false, AVTransportServiceNotFoundException())
                return
            }
            controlPoint?.execute(object : Stop(service) {
                override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                    super.success(invocation)
                    listener?.invoke(true, null)
                }

                override fun failure(
                    invocation: ActionInvocation<out Service<*, *>>?,
                    operation: UpnpResponse?,
                    defaultMsg: String
                ) {
                    listener?.invoke(false, DeviceExecuteException(defaultMsg))
                }
            })
        } catch (e: Exception) {
            listener?.invoke(false, e)
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            service?.registry?.removeListener(registryListener)
            context.unbindService(connection)
            selectedDevice = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setDeviceListener(listener: DeviceListener?) {
        this.deviceListener = listener
    }

    private val registryListener: RegistryListener = object : DefaultRegistryListener() {
        override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice?) {
            Log.i("Cling", "remoteDeviceDiscoveryStarted..")
        }

        override fun remoteDeviceDiscoveryFailed(
            registry: Registry?,
            device: RemoteDevice,
            ex: java.lang.Exception?
        ) {
            Log.i(
                "Cling",
                "remoteDeviceDiscoveryFailed:${device.displayString} because of : ${ex?.stackTraceToString()}"
            )
            //deviceListener?.onDeviceRemoved(device)
        }

        override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice) {
            Log.i("Cling", "remoteDeviceAdded:${device.displayString}")
            println(device.details.modelDetails.modelURI)
            deviceListener?.onDeviceFound(device)
        }

        override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice) {
            Log.i("Cling", "remoteDeviceRemoved:${device.displayString}")
            deviceListener?.onDeviceRemoved(device)
        }
    }
}