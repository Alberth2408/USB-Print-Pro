package com.usbprintpro.data.document

import com.usbprintpro.domain.model.FormattedRun
import com.usbprintpro.domain.model.PageSettings
import com.usbprintpro.domain.model.StyledDocument
import com.usbprintpro.domain.model.StyledElement
import com.usbprintpro.domain.model.TableCell
import com.usbprintpro.domain.model.TableRow
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordProcessorV2 @Inject constructor() {

    fun extractStyledDocument(bytes: ByteArray): Result<StyledDocument> {
        return try {
            val doc = if (isDocx(bytes)) {
                extractFromDocx(bytes)
            } else {
                extractFromDocLegacy(bytes)
            }
            Result.success(doc)
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

    private fun extractFromDocx(bytes: ByteArray): StyledDocument {
        val doc = XWPFDocument(ByteArrayInputStream(bytes))
        val elements = mutableListOf<StyledElement>()
        var pageSettings = PageSettings()

        for (bodyElement in doc.bodyElements) {
            when (bodyElement.elementType) {
                org.apache.poi.xwpf.usermodel.BodyElementType.PARAGRAPH -> {
                    val paragraph = bodyElement as XWPFParagraph
                    val parsed = parseParagraph(paragraph)
                    if (parsed != null) elements.add(parsed)
                }
                org.apache.poi.xwpf.usermodel.BodyElementType.TABLE -> {
                    val table = bodyElement as XWPFTable
                    elements.add(parseTable(table))
                }
                org.apache.poi.xwpf.usermodel.BodyElementType.CONTENTCONTROL -> {
                    // skip content controls
                }
            }
        }

        try {
            val sectPr = doc.document.body.getSectPr()
            if (sectPr != null) {
                val pgSz = sectPr.getPgSz()
                val pgMar = sectPr.getPgMar()

                val orientationStr = pgSz?.orient?.toString() ?: "portrait"

                pageSettings = PageSettings(
                    orientation = if (orientationStr.contains("landscape", ignoreCase = true)) "landscape" else "portrait",
                    paperSize = "A4",
                    marginTopMm = pgMar?.top?.let { twipsToMm(it) } ?: 25.4f,
                    marginBottomMm = pgMar?.bottom?.let { twipsToMm(it) } ?: 25.4f,
                    marginLeftMm = pgMar?.left?.let { twipsToMm(it) } ?: 31.8f,
                    marginRightMm = pgMar?.right?.let { twipsToMm(it) } ?: 31.8f
                )
            }
        } catch (_: Exception) {
            // Fallback to default PageSettings
        }

        doc.close()
        return StyledDocument(elements, pageSettings)
    }

    private fun parseParagraph(paragraph: XWPFParagraph): StyledElement.Paragraph? {
        val runs = mutableListOf<FormattedRun>()
        for (run in paragraph.runs) {
            val text = run.getText(0) ?: continue
            if (text.isBlank() && run.embeddedPictures.isNotEmpty()) {
                continue
            }
            runs.add(parseRun(run))
        }

        val alignment = when (paragraph.alignment) {
            ParagraphAlignment.LEFT -> "left"
            ParagraphAlignment.CENTER -> "center"
            ParagraphAlignment.RIGHT -> "right"
            ParagraphAlignment.BOTH -> "justify"
            else -> "left"
        }

        if (runs.isEmpty()) {
            val text = paragraph.text
            if (text.isBlank()) {
                runs.add(
                    FormattedRun(
                        text = text,
                        fontName = "Times New Roman",
                        fontSizePt = 12f
                    )
                )
            }
        }

        if (runs.isEmpty()) return null

        val images = mutableListOf<StyledElement.Image>()
        for (run in paragraph.runs) {
            for (pic in run.embeddedPictures) {
                val imgBytes = pic.pictureData.data
                val format = inferImageFormat(pic.pictureData.suggestFileExtension() ?: "png")
                val width = pic.width
                val height = pic.depth
                images.add(
                    StyledElement.Image(
                        imageBytes = imgBytes,
                        format = format,
                        widthPt = width.toFloat(),
                        heightPt = height.toFloat()
                    )
                )
            }
        }

        if (images.isNotEmpty()) {
            images.first().let { img ->
                return StyledElement.Paragraph(
                    runs = listOf(
                        FormattedRun(
                            text = "[Imagen: ${img.format}]",
                            fontName = runs.firstOrNull()?.fontName ?: "Times New Roman",
                            fontSizePt = runs.firstOrNull()?.fontSizePt ?: 12f
                        )
                    ),
                    alignment = alignment
                )
            }
        }

        return StyledElement.Paragraph(runs = runs, alignment = alignment)
    }

    private fun parseRun(run: XWPFRun): FormattedRun {
        val text = run.getText(0) ?: ""
        val color = run.color?.let {
            try {
                Integer.parseInt(it, 16).let { hex ->
                    0xFF000000.toInt() or ((hex shr 16) and 0xFF) or (hex and 0xFF00) or ((hex and 0xFF) shl 16)
                }
            } catch (_: NumberFormatException) { null }
        }

        return FormattedRun(
            text = text,
            fontName = run.fontName ?: "Times New Roman",
            fontSizePt = (run.fontSizeAsDouble ?: 12.0).toFloat(),
            bold = run.isBold,
            italic = run.isItalic,
            underline = run.underline != UnderlinePatterns.NONE,
            colorArgb = color,
            strikethrough = run.isStrikeThrough,
            subscript = run.verticalAlignment?.toString() == "subscript",
            superscript = run.verticalAlignment?.toString() == "superscript"
        )
    }

    private fun parseTable(table: XWPFTable): StyledElement.Table {
        val rows = mutableListOf<TableRow>()
        for (row in table.rows) {
            val cells = mutableListOf<TableCell>()
            for (cell in row.tableCells) {
                val cellParagraphs = mutableListOf<StyledElement.Paragraph>()
                for (p in cell.paragraphs) {
                    val parsed = parseParagraph(p)
                    if (parsed != null) cellParagraphs.add(parsed)
                }
                cells.add(
                    TableCell(
                        paragraphs = cellParagraphs,
                        widthPt = cell.width.toFloat(),
                        backgroundColorArgb = null
                    )
                )
            }
            rows.add(TableRow(cells = cells))
        }
        return StyledElement.Table(rows = rows)
    }

    private fun extractFromDocLegacy(bytes: ByteArray): StyledDocument {
        val doc = HWPFDocument(ByteArrayInputStream(bytes))
        val extractor = WordExtractor(doc)
        val text = extractor.text
        doc.close()

        val runs = text.lines().map { line ->
            FormattedRun(
                text = line,
                fontName = "Times New Roman",
                fontSizePt = 12f
            )
        }

        val paragraphs = if (runs.isEmpty()) {
            listOf(StyledElement.Paragraph(runs = listOf(FormattedRun(""))))
        } else {
            listOf(StyledElement.Paragraph(runs = runs))
        }

        return StyledDocument(paragraphs)
    }

    private fun inferImageFormat(ext: String): String {
        return when (ext.lowercase()) {
            "png" -> "png"
            "jpg", "jpeg" -> "jpeg"
            "gif" -> "gif"
            "bmp" -> "bmp"
            "wmf" -> "png"
            "svg" -> "png"
            else -> "png"
        }
    }

    private fun ptsToMm(pts: Double): Float {
        return (pts * 0.352778).toFloat()
    }

    private fun twipsToMm(twips: Any): Float {
        val value = when (twips) {
            is Number -> twips.toDouble()
            else -> twips.toString().toDoubleOrNull() ?: 0.0
        }
        return ptsToMm(value / 20.0)
    }
}
