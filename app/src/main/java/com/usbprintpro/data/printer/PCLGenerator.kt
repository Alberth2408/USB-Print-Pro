package com.usbprintpro.data.printer

import com.usbprintpro.domain.model.PrintSettings
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PCLGenerator @Inject constructor() : PrinterProtocol {

    private val charset = Charset.forName("ISO-8859-1")

    override fun generateHeader(settings: PrintSettings): ByteArray {
        val sb = StringBuilder()

        sb.append(ESC + "E")                          // Reset
        sb.append(ESC + "&l${settings.copies}X")     // Copias
        sb.append(ESC + "&l${settings.orientation.pclCode}O")  // Orientación
        sb.append(ESC + "&l${settings.paperSize.pclCode}A")    // Tamaño papel
        sb.append(ESC + "(s10H")                      // Font pitch 10
        sb.append(ESC + "(s12V")                      // Font height 12pt
        sb.append(ESC + "&l0E")                       // Top margin 0

        return sb.toString().toByteArray(charset)
    }

    override fun generateText(text: String): ByteArray {
        val sb = StringBuilder()
        for (line in text.lines()) {
            sb.appendLine(line)
        }
        return sb.toString().toByteArray(charset)
    }

    override fun generateFooter(): ByteArray {
        return byteArrayOf(0x0C)  // Form Feed - expulsa página
    }

    override fun generatePage(text: String, settings: PrintSettings): ByteArray {
        return generateHeader(settings) +
                generateText(text) +
                generateFooter()
    }

    companion object {
        private const val ESC = "\u001B"
    }
}
