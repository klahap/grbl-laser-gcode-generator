package de.quati.grbl_laser

import java.awt.Font
import java.awt.font.TextAttribute


class GeneratorJvmImpl(
    val settings: GeneratorSettings,
) : Generator {
    val fontAttributes = buildMap {
        put(TextAttribute.FAMILY, settings.fontName)
        put(TextAttribute.SIZE, settings.fontSize)
        if (FontStyle.BOLD in settings.fontStyles)
            put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)
        if (FontStyle.ITALIC in settings.fontStyles)
            put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE)
    }
    val font = Font(fontAttributes)
    val tolerance = settings.fontSize.toDouble() / 96

    override fun generateGCode(data: String): GCode = font.generateShape(text = data)
        .align(hAlign = settings.hAlign, vAlign = settings.vAlign)
        .toGcode(tolerance = tolerance, power = settings.laserPower, speed = settings.laserSpeed)
}

actual fun GeneratorSettings.toGenerator(): Generator = GeneratorJvmImpl(settings = this)