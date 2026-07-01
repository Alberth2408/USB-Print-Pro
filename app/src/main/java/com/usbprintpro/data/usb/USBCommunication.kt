package com.usbprintpro.data.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import com.usbprintpro.data.printer.PrinterOutput

class USBCommunication(
    private val connection: UsbDeviceConnection,
    private val usbInterface: UsbInterface
) : PrinterOutput {

    private val outEndpoint: UsbEndpoint? = findBulkOutEndpoint()

    override fun write(data: ByteArray): Boolean {
        val endpoint = outEndpoint ?: return false
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(data.size - offset, 1024)
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            val sent = connection.bulkTransfer(endpoint, chunk, chunk.size, 5000)
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

    private fun findBulkOutEndpoint(): UsbEndpoint? {
        for (i in 0 until usbInterface.endpointCount) {
            val ep = usbInterface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                ep.direction == UsbConstants.USB_DIR_OUT
            ) {
                return ep
            }
        }
        return null
    }
}
