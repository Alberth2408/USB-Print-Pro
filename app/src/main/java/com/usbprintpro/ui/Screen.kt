package com.usbprintpro.ui

sealed class Screen {
    data object Home : Screen()
    data object Print : Screen()
}
