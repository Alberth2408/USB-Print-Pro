package com.usbprintpro.data.printer

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import javax.inject.Inject
import javax.inject.Singleton

enum class PrinterLanguage {
    PCL,
    PDF,
    PCL_AND_PDF,
    POSTSCRIPT,
    UNKNOWN
}

@Singleton
class PrinterCapabilityDetector @Inject constructor() {

    fun detect(device: UsbDevice): PrinterLanguage {
        val protocol = detectViaInterfaceProtocol(device)
        if (protocol != PrinterLanguage.UNKNOWN) return protocol

        return PrinterLanguage.PCL
    }

    fun detectViaPJL(connection: UsbDeviceConnection, outEndpoint: UsbEndpoint, inEndpoint: UsbEndpoint?): PrinterLanguage {
        val pjlQuery = "@PJL INFO ID\r\n"
        try {
            connection.bulkTransfer(outEndpoint, pjlQuery.toByteArray(), pjlQuery.length, 5000)
            if (inEndpoint != null) {
                val buffer = ByteArray(1024)
                val read = connection.bulkTransfer(inEndpoint, buffer, buffer.size, 3000)
                if (read > 0) {
                    val response = String(buffer, 0, read, Charsets.US_ASCII)
                    return when {
                        response.contains("PDF", ignoreCase = true) -> PrinterLanguage.PCL_AND_PDF
                        response.contains("PCL", ignoreCase = true) -> PrinterLanguage.PCL
                        response.contains("PostScript", ignoreCase = true) -> PrinterLanguage.POSTSCRIPT
                        else -> PrinterLanguage.PCL
                    }
                }
            }
        } catch (_: Exception) { }
        return PrinterLanguage.PCL
    }

    private fun detectViaInterfaceProtocol(device: UsbDevice): PrinterLanguage {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                return when (iface.interfaceProtocol) {
                    1 -> PrinterLanguage.PCL
                    2 -> PrinterLanguage.POSTSCRIPT
                    3 -> PrinterLanguage.PCL_AND_PDF
                    4 -> PrinterLanguage.PCL_AND_PDF
                    5 -> PrinterLanguage.PDF
                    else -> PrinterLanguage.UNKNOWN
                }
            }
        }
        return PrinterLanguage.UNKNOWN
    }
}
