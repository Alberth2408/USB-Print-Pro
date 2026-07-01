package com.usbprintpro.data.document

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageProcessor @Inject constructor() {

    fun convertToPCLRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = ((width + 31) / 32) * 4

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bwData = ByteArray(bytesPerRow * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (lum < 128) {
                    val byteIdx = y * bytesPerRow + (x / 8)
                    val bitIdx = 7 - (x % 8)
                    bwData[byteIdx] = (bwData[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                }
            }
        }

        val out = ByteArrayOutputStream()
        out.write((ESC + "*t${width}H").toByteArray())        // Width in decipoints
        out.write((ESC + "*t${height}V").toByteArray())       // Height
        out.write((ESC + "*r1A").toByteArray())               // Start raster
        out.write((ESC + "*b${bwData.size}W").toByteArray())  // Row data
        out.write(bwData)
        out.write((ESC + "*rB").toByteArray())                // End raster

        return out.toByteArray()
    }

    private companion object {
        private const val ESC = "\u001B"
    }
}
