package com.usbprintpro.data.document

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PDFProcessor @Inject constructor() {

    fun extractText(pdfBytes: ByteArray): Result<String> {
        return try {
            val reader = PdfReader(ByteArrayInputStream(pdfBytes))
            val pdfDoc = PdfDocument(reader)
            val sb = StringBuilder()
            for (i in 1..pdfDoc.numberOfPages) {
                val page = pdfDoc.getPage(i)
                sb.appendLine(PdfTextExtractor.getTextFromPage(page))
                sb.appendLine("\n--- Pagina $i ---\n")
            }
            pdfDoc.close()
            reader.close()
            Result.success(sb.toString().trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
