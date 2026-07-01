package com.usbprintpro.domain.model

data class PrintSettings(
    val copies: Int = 1,
    val orientation: Orientation = Orientation.PORTRAIT,
    val paperSize: PaperSize = PaperSize.A4
)
