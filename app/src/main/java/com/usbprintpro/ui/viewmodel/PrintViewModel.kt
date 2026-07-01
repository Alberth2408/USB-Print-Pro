package com.usbprintpro.ui.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usbprintpro.data.document.HTMLProcessor
import com.usbprintpro.data.document.ImageProcessor
import com.usbprintpro.data.document.PDFGenerator
import com.usbprintpro.data.document.PDFProcessor
import com.usbprintpro.data.document.WordProcessorV2
import com.usbprintpro.data.printer.PCLGenerator
import com.usbprintpro.data.printer.PCLRasterizer
import com.usbprintpro.data.printer.PrinterOutput
import com.usbprintpro.data.usb.PrinterSimulation
import com.usbprintpro.domain.model.Orientation
import com.usbprintpro.domain.model.PaperSize
import com.usbprintpro.domain.model.PrintSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

sealed class DocumentType {
    data object Text : DocumentType()
    data object PDF : DocumentType()
    data object Image : DocumentType()
    data object HTML : DocumentType()
    data class Word(val hasFormatting: Boolean = false) : DocumentType()
}

@HiltViewModel
class PrintViewModel @Inject constructor(
    application: Application,
    private val pclGenerator: PCLGenerator,
    private val pdfProcessor: PDFProcessor,
    private val imageProcessor: ImageProcessor,
    private val htmlProcessor: HTMLProcessor,
    private val wordProcessorV2: WordProcessorV2,
    private val pdfGenerator: PDFGenerator,
    private val pclRasterizer: PCLRasterizer
) : AndroidViewModel(application) {

    private val _docType = MutableStateFlow<DocumentType>(DocumentType.Text)
    val docType: StateFlow<DocumentType> = _docType.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _fileName = MutableStateFlow<String?>(null)
    val fileName: StateFlow<String?> = _fileName.asStateFlow()

    private val _settings = MutableStateFlow(PrintSettings())
    val settings: StateFlow<PrintSettings> = _settings.asStateFlow()

    private val _hexPreview = MutableStateFlow("")
    val hexPreview: StateFlow<String> = _hexPreview.asStateFlow()

    private val _status = MutableStateFlow("Escribe texto o selecciona un archivo")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()

    private val _pdfBytes = MutableStateFlow<ByteArray?>(null)
    val pdfBytes: StateFlow<ByteArray?> = _pdfBytes.asStateFlow()

    private var output: PrinterOutput = PrinterSimulation()

    fun processPDF(uri: Uri) {
        viewModelScope.launch {
            _status.value = "Procesando PDF..."
            val bytes = readBytes(uri) ?: run {
                _status.value = "Error al leer el PDF"
                return@launch
            }
            pdfProcessor.extractText(bytes).fold(
                onSuccess = { extractedText ->
                    _text.value = extractedText
                    _docType.value = DocumentType.PDF
                    _fileName.value = uri.lastPathSegment
                    _pdfBytes.value = null
                    _status.value = "PDF cargado (${bytes.size / 1024} KB)"
                },
                onFailure = { e ->
                    _status.value = "Error al procesar PDF: ${e.message}"
                }
            )
        }
    }

    fun processImage(uri: Uri) {
        viewModelScope.launch {
            _status.value = "Procesando imagen..."
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                ?: run {
                    _status.value = "Error al leer la imagen"
                    return@launch
                }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) {
                _status.value = "Error al decodificar la imagen"
                return@launch
            }
            val rasterData = imageProcessor.convertToPCLRaster(bitmap)
            val header = pclGenerator.generateHeader(_settings.value)
            val pclData = header + rasterData + byteArrayOf(0x0C)

            _text.value = "[Imagen procesada - ${rasterData.size} bytes PCL]"
            _docType.value = DocumentType.Image
            _fileName.value = uri.lastPathSegment
            _pdfBytes.value = null

            val sim = PrinterSimulation()
            sim.write(pclData)
            _hexPreview.value = sim.getHexDump().take(500)
            _status.value = "Imagen procesada (${pclData.size} bytes PCL)"
        }
    }

    fun processHTML(uri: Uri) {
        viewModelScope.launch {
            _status.value = "Procesando HTML..."
            val html = readText(uri) ?: run {
                _status.value = "Error al leer el HTML"
                return@launch
            }
            val plainText = htmlProcessor.extractText(html)
            _text.value = plainText
            _docType.value = DocumentType.HTML
            _fileName.value = uri.lastPathSegment
            _pdfBytes.value = null
            _status.value = "HTML cargado (${plainText.length} caracteres)"
        }
    }

    fun processWord(uri: Uri) {
        viewModelScope.launch {
            _status.value = "Procesando documento Word..."
            val bytes = readBytes(uri) ?: run {
                _status.value = "Error al leer el archivo Word"
                return@launch
            }

            wordProcessorV2.extractStyledDocument(bytes).fold(
                onSuccess = { styledDoc ->
                    val runs = styledDoc.elements.flatMap { element ->
                        when (element) {
                            is com.usbprintpro.domain.model.StyledElement.Paragraph ->
                                element.runs.map { it.text }
                            else -> emptyList()
                        }
                    }
                    _text.value = runs.joinToString("\n")

                    pdfGenerator.generate(styledDoc).fold(
                        onSuccess = { outputBytes ->
                            _pdfBytes.value = outputBytes
                            _docType.value = DocumentType.Word(hasFormatting = true)
                            _fileName.value = uri.lastPathSegment
                            _status.value = "Word con formato cargado (PDF: ${outputBytes.size / 1024} KB)"
                        },
                        onFailure = { e ->
                            _docType.value = DocumentType.Word(hasFormatting = false)
                            _fileName.value = uri.lastPathSegment
                            _status.value = "Word cargado (solo texto, error PDF: ${e.message})"
                        }
                    )
                },
                onFailure = { e ->
                    processWordLegacy(bytes, uri)
                }
            )
        }
    }

    private fun processWordLegacy(bytes: ByteArray, uri: Uri) {
        viewModelScope.launch {
            val text = try {
                val doc = org.apache.poi.hwpf.HWPFDocument(java.io.ByteArrayInputStream(bytes))
                val extractor = org.apache.poi.hwpf.extractor.WordExtractor(doc)
                extractor.text.trim()
            } catch (e: Exception) {
                _status.value = "Error al procesar Word: ${e.message}"
                return@launch
            }
            _text.value = text
            _docType.value = DocumentType.Word(hasFormatting = false)
            _fileName.value = uri.lastPathSegment
            _pdfBytes.value = null
            _status.value = "Word (.doc antiguo) cargado (${text.length} caracteres)"
        }
    }

    fun updateText(newText: String) {
        _text.value = newText
        if (_docType.value !is DocumentType.Text) {
            _docType.value = DocumentType.Text
            _fileName.value = null
            _pdfBytes.value = null
        }
    }

    fun updateCopies(copies: Int) {
        _settings.value = _settings.value.copy(copies = copies.coerceIn(1, 99))
    }

    fun updateOrientation(orientation: Orientation) {
        _settings.value = _settings.value.copy(orientation = orientation)
    }

    fun updatePaperSize(paperSize: PaperSize) {
        _settings.value = _settings.value.copy(paperSize = paperSize)
    }

    fun testPrint() {
        viewModelScope.launch {
            val printerSim = PrinterSimulation()
            output = printerSim

            val data = pclGenerator.generatePage(_text.value, _settings.value)
            val success = output.write(data)

            if (success) {
                _hexPreview.value = printerSim.getHexDump()
                _status.value = "Datos PCL generados (${data.size} bytes)"
            } else {
                _status.value = "Error al generar datos PCL"
            }
        }
    }

    fun exportToFile() {
        viewModelScope.launch {
            val data = pclGenerator.generatePage(_text.value, _settings.value)
            val dir = getApplication<Application>().getExternalFilesDir("prints")
            dir?.mkdirs()
            val file = File(dir, "print_${System.currentTimeMillis()}.prn")
            FileOutputStream(file).use { it.write(data) }
            _exportPath.value = file.absolutePath
            _status.value = "Exportado a: ${file.name} (${data.size} bytes)"
        }
    }

    fun exportPDF() {
        viewModelScope.launch {
            val pdfData = _pdfBytes.value ?: run {
                _status.value = "No hay PDF generado"
                return@launch
            }
            val dir = getApplication<Application>().getExternalFilesDir("prints")
            dir?.mkdirs()
            val file = File(dir, "document_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { it.write(pdfData) }
            _exportPath.value = file.absolutePath
            _status.value = "PDF exportado a: ${file.name} (${pdfData.size / 1024} KB)"
        }
    }

    fun clearExportPath() {
        _exportPath.value = null
    }

    private fun readBytes(uri: Uri): ByteArray? {
        return try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readText(uri: Uri): String? {
        return try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }
}
