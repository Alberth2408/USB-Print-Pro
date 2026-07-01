package com.usbprintpro.data.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class USBDeviceReceiver : BroadcastReceiver() {

    @Inject lateinit var hostManager: USBHostManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = IntentCompat.getParcelableExtra(
                    intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java
                )
                device?.let { hostManager.onDeviceAttached(it) }
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = IntentCompat.getParcelableExtra(
                    intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java
                )
                device?.let { hostManager.onDeviceDetached(it) }
            }
        }
    }
}
