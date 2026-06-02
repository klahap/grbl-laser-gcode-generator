package de.quati.grbl_laser

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Grbl Laser GCode Generator",
    ) {
        App()
    }
}