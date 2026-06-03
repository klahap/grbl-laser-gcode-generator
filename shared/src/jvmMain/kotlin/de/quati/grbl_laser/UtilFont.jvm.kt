package de.quati.grbl_laser

import java.awt.Font
import java.awt.GraphicsEnvironment


actual fun getFonts(): List<String> {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val fontFamilies = ge.availableFontFamilyNames?.filterNotNull() ?: emptyList()
    return fontFamilies
}

actual fun getDefaultFont(): String? = runCatching {
    Font(Font.SANS_SERIF, Font.PLAIN, 12).family
}.getOrNull()
