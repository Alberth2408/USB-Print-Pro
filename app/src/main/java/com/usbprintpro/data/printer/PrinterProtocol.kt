package com.usbprintpro.data.printer

import com.usbprintpro.domain.model.PrintSettings

interface PrinterProtocol {
    fun generateHeader(settings: PrintSettings): ByteArray
    fun generateText(text: String): ByteArray
    fun generateFooter(): ByteArray
    fun generatePage(text: String, settings: PrintSettings): ByteArray
}
