package com.usbprintpro.data.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Singleton
import javax.inject.Inject

@Singleton
class USBHostManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    sealed class USBEvent {
        data class DeviceAttached(val device: UsbDevice) : USBEvent()
        data class DeviceDetached(val device: UsbDevice) : USBEvent()
        data class PermissionResult(val device: UsbDevice, val granted: Boolean) : USBEvent()
    }

    private val _events = MutableSharedFlow<USBEvent>(replay = 0, extraBufferCapacity = 8)
    val events: SharedFlow<USBEvent> = _events.asSharedFlow()

    private val permissionIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun getConnectedPrinters(): List<UsbDevice> {
        return usbManager.deviceList.values.filter { device ->
            device.deviceClass == 7 || hasPrinterInterface(device)
        }
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun requestPermission(device: UsbDevice) {
        usbManager.requestPermission(device, permissionIntent)
    }

    fun openConnection(device: UsbDevice): UsbDeviceConnection? {
        val usbInterface = findPrinterInterface(device) ?: return null
        val connection = usbManager.openDevice(device)
        connection?.claimInterface(usbInterface, true)
        return connection
    }

    fun onPermissionResult(device: UsbDevice, granted: Boolean) {
        scope.launch {
            _events.emit(USBEvent.PermissionResult(device, granted))
        }
    }

    fun onDeviceAttached(device: UsbDevice) {
        scope.launch {
            _events.emit(USBEvent.DeviceAttached(device))
        }
    }

    fun onDeviceDetached(device: UsbDevice) {
        scope.launch {
            _events.emit(USBEvent.DeviceDetached(device))
        }
    }

    private fun hasPrinterInterface(device: UsbDevice): Boolean {
        return findPrinterInterface(device) != null
    }

    private fun findPrinterInterface(device: UsbDevice) = device.let { dev ->
        for (i in 0 until dev.interfaceCount) {
            val usbInterface = dev.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                return@let usbInterface
            }
        }
        null
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.usbprintpro.USB_PERMISSION"
    }
}
