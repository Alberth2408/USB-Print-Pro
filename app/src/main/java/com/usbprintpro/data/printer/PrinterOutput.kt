package com.usbprintpro.data.printer

interface PrinterOutput {
    fun write(data: ByteArray): Boolean
    fun close()
}
