package com.usbprintpro.data.printer

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

class PDFCommunication(
    private val connection: UsbDeviceConnection,
    private val usbInterface: UsbInterface
) : PrinterOutput {

    private val outEndpoint: UsbEndpoint? = findEndpoint(UsbConstants.USB_DIR_OUT)

    override fun write(data: ByteArray): Boolean {
        val endpoint = outEndpoint ?: return false
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(data.size - offset, 1024)
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            val sent = connection.bulkTransfer(endpoint, chunk, chunk.size, 10000)
            if (sent < 0) return false
            offset += sent
        }
        return true
    }

    override fun close() {
        try {
            connection.releaseInterface(usbInterface)
            connection.close()
        } catch (_: Exception) { }
    }

    private fun findEndpoint(direction: Int): UsbEndpoint? {
        for (i in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                ep.direction == direction
            ) {
                return ep
            }
        }
        return null
    }
}
