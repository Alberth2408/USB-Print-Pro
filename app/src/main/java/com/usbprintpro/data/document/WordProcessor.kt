package com.usbprintpro.data.document

import android.util.Xml
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordProcessor @Inject constructor() {

    fun extractText(bytes: ByteArray): Result<String> {
        return try {
            val text = if (isDocx(bytes)) extractFromDocx(bytes) else extractFromDoc(bytes)
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isDocx(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()
    }

    private fun extractFromDocx(bytes: ByteArray): String {
        val zis = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zis.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xml = zis.readBytes().toString(Charsets.UTF_8)
                zis.closeEntry()
                zis.close()
                return parseDocxXml(xml)
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        throw IllegalStateException("word/document.xml no encontrado en el archivo")
    }

    private fun parseDocxXml(xml: String): String {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        val sb = StringBuilder()
        var inText = false

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "t" && parser.namespace == NS_WP) {
                        inText = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inText) {
                        sb.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> if (parser.namespace == NS_WP) inText = false
                        "p" -> if (parser.namespace == NS_WP) sb.append('\n')
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun extractFromDoc(bytes: ByteArray): String {
        val doc = HWPFDocument(ByteArrayInputStream(bytes))
        val extractor = WordExtractor(doc)
        return extractor.text
    }

    private companion object {
        private const val NS_WP = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    }
}
