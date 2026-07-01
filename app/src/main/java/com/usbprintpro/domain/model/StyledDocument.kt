package com.usbprintpro.domain.model

import java.util.Objects

data class StyledDocument(
    val elements: List<StyledElement>,
    val pageSettings: PageSettings = PageSettings()
)

data class PageSettings(
    val orientation: String = "portrait",
    val paperSize: String = "A4",
    val marginTopMm: Float = 25.4f,
    val marginBottomMm: Float = 25.4f,
    val marginLeftMm: Float = 31.8f,
    val marginRightMm: Float = 31.8f
) {
    val isLandscape: Boolean get() = orientation.equals("landscape", ignoreCase = true)
}

sealed interface StyledElement {
    data class Paragraph(
        val runs: List<FormattedRun>,
        val alignment: String = "left"
    ) : StyledElement

    data class Image(
        val imageBytes: ByteArray,
        val format: String,
        val widthPt: Float,
        val heightPt: Float
    ) : StyledElement {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false
            return imageBytes.contentEquals(other.imageBytes) &&
                format == other.format &&
                widthPt == other.widthPt &&
                heightPt == other.heightPt
        }

        override fun hashCode(): Int {
            return Objects.hash(imageBytes.contentHashCode(), format, widthPt, heightPt)
        }
    }

    data class Table(
        val rows: List<TableRow>
    ) : StyledElement
}

data class FormattedRun(
    val text: String,
    val fontName: String = "Times New Roman",
    val fontSizePt: Float = 12f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val colorArgb: Int? = null,
    val strikethrough: Boolean = false,
    val subscript: Boolean = false,
    val superscript: Boolean = false
)

data class TableRow(
    val cells: List<TableCell>
)

data class TableCell(
    val paragraphs: List<StyledElement.Paragraph>,
    val widthPt: Float? = null,
    val backgroundColorArgb: Int? = null
)
