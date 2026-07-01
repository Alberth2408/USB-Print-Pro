package com.usbprintpro.data.printer

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface

class SmartPrinterOutput(
    connection: UsbDeviceConnection,
    usbInterface: UsbInterface,
    language: PrinterLanguage,
    private val pclRasterizer: PCLRasterizer
) : PrinterOutput {

    private val delegate: PrinterOutput = when (language) {
        PrinterLanguage.PDF -> PDFCommunication(connection, usbInterface)
        else -> USBCommunication2(connection, usbInterface)
    }

    private var pendingPDF: ByteArray? = null

    fun printPDF(pdfBytes: ByteArray, rasterize: Boolean = false): Boolean {
        return if (delegate is PDFCommunication || rasterize) {
            val data = if (rasterize || delegate !is PDFCommunication) {
                pclRasterizer.rasterize(pdfBytes, com.usbprintpro.domain.model.PrintSettings())
            } else {
                pdfBytes
            }
            delegate.write(data)
        } else {
            pendingPDF = pdfBytes
            false
        }
    }

    override fun write(data: ByteArray): Boolean {
        return delegate.write(data)
    }

    override fun close() {
        delegate.close()
    }

    private class USBCommunication2(
        connection: UsbDeviceConnection,
        usbInterface: UsbInterface
    ) : PrinterOutput {
        private val inner = com.usbprintpro.data.usb.USBCommunication(connection, usbInterface)
        override fun write(data: ByteArray) = inner.write(data)
        override fun close() = inner.close()
    }
}
