package de.quati.grbl_laser

import java.awt.GraphicsEnvironment


actual fun getFonts(): List<String> {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val fontFamilies = ge.availableFontFamilyNames?.filterNotNull() ?: emptyList()
    return fontFamilies
}