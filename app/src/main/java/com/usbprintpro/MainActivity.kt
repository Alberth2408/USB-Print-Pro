package com.usbprintpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.usbprintpro.ui.Screen
import com.usbprintpro.ui.screens.HomeScreen
import com.usbprintpro.ui.screens.PrintScreen
import com.usbprintpro.ui.theme.USBPrintProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            USBPrintProTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        onNavigateToPrint = { currentScreen = Screen.Print }
                    )
                    Screen.Print -> PrintScreen(
                        onBack = { currentScreen = Screen.Home }
                    )
                }
            }
        }
    }
}
