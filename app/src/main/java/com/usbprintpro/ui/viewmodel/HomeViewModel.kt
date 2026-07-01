package com.usbprintpro.ui.viewmodel

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.usbprintpro.data.usb.USBHostManager
import com.usbprintpro.domain.model.USBPrinter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val usbHostManager: USBHostManager
) : AndroidViewModel(application) {

    private val _printers = MutableStateFlow<List<USBPrinter>>(emptyList())
    val printers: StateFlow<List<USBPrinter>> = _printers.asStateFlow()

    private val _status = MutableStateFlow("Esperando conexión USB...")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        observeUSBEvents()
    }

    fun discoverPrinters() {
        viewModelScope.launch {
            _isScanning.value = true
            _status.value = "Buscando impresoras..."

            val devices = usbHostManager.getConnectedPrinters()

            _printers.value = devices.map { device ->
                usbDeviceToPrinter(device)
            }

            when {
                devices.isEmpty() -> {
                    _status.value = "No se encontraron impresoras. Conecta una por USB OTG."
                }
                devices.size == 1 -> {
                    val device = devices.first()
                    if (usbHostManager.hasPermission(device)) {
                        _status.value = "Impresora lista: ${device.productName ?: "Desconocida"}"
                    } else {
                        _status.value = "Solicitando permiso para la impresora..."
                        usbHostManager.requestPermission(device)
                    }
                }
                else -> {
                    _status.value = "${devices.size} impresoras encontradas. Selecciona una."
                }
            }

            _isScanning.value = false
        }
    }

    private fun observeUSBEvents() {
        viewModelScope.launch {
            usbHostManager.events.collect { event ->
                when (event) {
                    is USBHostManager.USBEvent.DeviceAttached -> {
                        _status.value = "Impresora detectada: ${event.device.productName ?: "Desconocida"}"
                        discoverPrinters()
                    }
                    is USBHostManager.USBEvent.DeviceDetached -> {
                        _status.value = "Impresora desconectada"
                        _printers.value = _printers.value.filter {
                            it.deviceId != event.device.deviceId
                        }
                        if (_printers.value.isEmpty()) {
                            _status.value = "Esperando conexión USB..."
                        }
                    }
                    is USBHostManager.USBEvent.PermissionResult -> {
                        if (event.granted) {
                            _status.value = "Permiso concedido"
                            discoverPrinters()
                        } else {
                            _status.value = "Permiso denegado para la impresora"
                        }
                    }
                }
            }
        }
    }

    private fun usbDeviceToPrinter(device: UsbDevice): USBPrinter {
        return USBPrinter(
            deviceId = device.deviceId,
            name = device.productName ?: "Impresora ${String.format("%04X", device.productId)}",
            vendorId = device.vendorId,
            productId = device.productId,
            manufacturer = device.manufacturerName ?: "Fabricante ${String.format("%04X", device.vendorId)}",
            hasPermission = usbHostManager.hasPermission(device),
            isConnected = true
        )
    }
}
