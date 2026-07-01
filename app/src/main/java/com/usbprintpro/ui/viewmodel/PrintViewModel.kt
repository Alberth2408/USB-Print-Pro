package com.usbprintpro.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usbprintpro.data.printer.PCLGenerator
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

@HiltViewModel
class PrintViewModel @Inject constructor(
    application: Application,
    private val pclGenerator: PCLGenerator
) : AndroidViewModel(application) {

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _settings = MutableStateFlow(PrintSettings())
    val settings: StateFlow<PrintSettings> = _settings.asStateFlow()

    private val _hexPreview = MutableStateFlow("")
    val hexPreview: StateFlow<String> = _hexPreview.asStateFlow()

    private val _status = MutableStateFlow("Escribe texto y presiona Probar")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()

    private var output: PrinterOutput = PrinterSimulation()

    fun updateText(newText: String) {
        _text.value = newText
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

    fun clearExportPath() {
        _exportPath.value = null
    }
}
