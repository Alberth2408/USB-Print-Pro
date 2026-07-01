package com.usbprintpro.data.document

import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.layout.properties.HorizontalAlignment
import com.usbprintpro.domain.model.FormattedRun
import com.usbprintpro.domain.model.StyledDocument
import com.usbprintpro.domain.model.StyledElement
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PDFGenerator @Inject constructor() {

    fun generate(doc: StyledDocument): Result<ByteArray> {
        return try {
            val out = ByteArrayOutputStream()
            val writer = PdfWriter(out)
            val pdf = PdfDocument(writer)

            val pageSize = resolvePageSize(doc.pageSettings.paperSize, doc.pageSettings.isLandscape)
            pdf.addNewPage(pageSize)

            val layoutDoc = Document(pdf, pageSize)
            applyMargins(layoutDoc, doc)

            for (element in doc.elements) {
                when (element) {
                    is StyledElement.Paragraph -> layoutDoc.add(createParagraph(element))
                    is StyledElement.Table -> layoutDoc.add(createTable(element))
                    is StyledElement.Image -> {
                        val img = createImage(element)
                        if (img != null) layoutDoc.add(img)
                    }
                }
            }

            layoutDoc.close()
            pdf.close()
            Result.success(out.toByteArray())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createParagraph(element: StyledElement.Paragraph): Paragraph {
        val paragraph = Paragraph()
        paragraph.setTextAlignment(resolveAlignment(element.alignment))

        for (run in element.runs) {
            val text = Text(run.text)
            applyRunStyle(text, run)
            paragraph.add(text)
        }

        return paragraph
    }

    private fun createTable(element: StyledElement.Table): Table {
        val cols = element.rows.firstOrNull()?.cells?.size ?: 1
        val table = Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth()

        for (row in element.rows) {
            for (cellData in row.cells) {
                val cell = Cell()
                for (p in cellData.paragraphs) {
                    cell.add(createParagraph(p))
                }
                cellData.widthPt?.let { cell.setWidth(it) }
                cellData.backgroundColorArgb?.let { color ->
                    cell.setBackgroundColor(
                        DeviceRgb(
                            (color shr 16) and 0xFF,
                            (color shr 8) and 0xFF,
                            color and 0xFF
                        )
                    )
                }
                table.addCell(cell)
            }
        }

        return table
    }

    private fun createImage(imageElement: StyledElement.Image): Image? {
        return try {
            val imgData = ImageDataFactory.create(imageElement.imageBytes, false)
            val img = Image(imgData)
            if (imageElement.widthPt > 0 && imageElement.heightPt > 0) {
                img.setWidth(imageElement.widthPt)
                img.setHeight(imageElement.heightPt)
            }
            img.setHorizontalAlignment(HorizontalAlignment.CENTER)
            img
        } catch (_: Exception) { null }
    }

    private fun applyRunStyle(text: Text, run: FormattedRun) {
        try {
            val font = loadFont(run.fontName, run.bold, run.italic)
            text.setFont(font)
        } catch (_: Exception) {
            // fallback to default font
        }

        text.setFontSize(run.fontSizePt)

        run.colorArgb?.let { color ->
            text.setFontColor(
                DeviceRgb(
                    (color shr 16) and 0xFF,
                    (color shr 8) and 0xFF,
                    color and 0xFF
                )
            )
        }

        if (run.underline) text.setUnderline()
        if (run.strikethrough) text.setLineThrough()
    }

    private fun loadFont(fontName: String, bold: Boolean, italic: Boolean): PdfFont {
        val baseFont = when (fontName.lowercase()) {
            "arial" -> "Helvetica"
            "times new roman" -> "Times-Roman"
            "courier new" -> "Courier"
            "helvetica" -> "Helvetica"
            "times" -> "Times-Roman"
            "courier" -> "Courier"
            "symbol" -> "Symbol"
            "zapfdingbats" -> "ZapfDingbats"
            else -> "Times-Roman"
        }

        val style = when {
            bold && italic -> ",BoldItalic"
            bold -> ",Bold"
            italic -> ",Italic"
            else -> ""
        }

        val fontNameWithStyle = baseFont + style
        return PdfFontFactory.createRegisteredFont(fontNameWithStyle)
    }

    private fun resolvePageSize(paperSize: String, landscape: Boolean): PageSize {
        val base = when (paperSize.lowercase()) {
            "a4" -> PageSize.A4
            "a5" -> PageSize.A5
            "a3" -> PageSize.A3
            "letter" -> PageSize.LETTER
            "legal" -> PageSize.LEGAL
            "tabloid" -> PageSize.TABLOID
            "executive" -> PageSize.EXECUTIVE
            else -> PageSize.A4
        }
        return if (landscape) PageSize(base.rotate()) else base
    }

    private fun applyMargins(layoutDoc: Document, doc: StyledDocument) {
        layoutDoc.setMargins(
            doc.pageSettings.marginTopMm,
            doc.pageSettings.marginRightMm,
            doc.pageSettings.marginBottomMm,
            doc.pageSettings.marginLeftMm
        )
    }

    private fun resolveAlignment(alignment: String): TextAlignment {
        return when (alignment.lowercase()) {
            "center" -> TextAlignment.CENTER
            "right" -> TextAlignment.RIGHT
            "justify" -> TextAlignment.JUSTIFIED
            else -> TextAlignment.LEFT
        }
    }
}
