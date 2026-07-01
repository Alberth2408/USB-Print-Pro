package com.usbprintpro.domain.model

data class USBPrinter(
    val deviceId: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String,
    val hasPermission: Boolean,
    val isConnected: Boolean
)
