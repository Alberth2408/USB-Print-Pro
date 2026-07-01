package com.usbprintpro.domain.model

enum class PaperSize(val label: String, val pclCode: String) {
    A4("A4", "26"),
    LETTER("Carta", "2"),
    LEGAL("Oficio", "3"),
    A5("A5", "25"),
    EXECUTIVE("Ejecutivo", "1");
}
