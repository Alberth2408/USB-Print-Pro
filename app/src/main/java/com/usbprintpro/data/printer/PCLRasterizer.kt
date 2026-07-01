package com.usbprintpro.data.printer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.usbprintpro.data.document.ImageProcessor
import com.usbprintpro.domain.model.PrintSettings
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PCLRasterizer @Inject constructor(
    private val imageProcessor: ImageProcessor
) {
    fun rasterize(pdfBytes: ByteArray, settings: PrintSettings): ByteArray {
        val tempFile = File.createTempFile("print_pdf_", ".pdf")
        FileOutputStream(tempFile).use { it.write(pdfBytes) }

        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        val out = ByteArrayOutputStream()

        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val width = page.width
            val height = page.height

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            val pclRaster = imageProcessor.convertToPCLRaster(bitmap)
            out.write(pclRaster)
            out.write(0x0C)

            bitmap.recycle()
        }

        renderer.close()
        pfd.close()
        tempFile.delete()

        return out.toByteArray()
    }
}
