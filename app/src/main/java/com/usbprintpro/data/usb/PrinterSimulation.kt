package com.usbprintpro.data.usb

import com.usbprintpro.data.printer.PrinterOutput

class PrinterSimulation : PrinterOutput {

    val writtenData = mutableListOf<Byte>()

    override fun write(data: ByteArray): Boolean {
        writtenData.addAll(data.toList())
        return true
    }

    override fun close() { }

    fun getData(): ByteArray = writtenData.toByteArray()

    fun getHexDump(): String {
        return getData()
            .chunked(16)
            .joinToString("\n") { chunk ->
                chunk.joinToString(" ") { byte ->
                    String.format("%02X", byte)
                }
            }
    }

    fun clear() {
        writtenData.clear()
    }
}
