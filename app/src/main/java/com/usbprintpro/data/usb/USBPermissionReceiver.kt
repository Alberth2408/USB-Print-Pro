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
class USBPermissionReceiver : BroadcastReceiver() {

    @Inject lateinit var hostManager: USBHostManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == USBHostManager.ACTION_USB_PERMISSION) {
            val device = IntentCompat.getParcelableExtra(
                intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java
            )
            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            device?.let { hostManager.onPermissionResult(it, granted) }
        }
    }
}
