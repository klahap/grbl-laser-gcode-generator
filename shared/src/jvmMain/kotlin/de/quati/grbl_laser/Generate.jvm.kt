package de.quati.grbl_laser

import java.awt.Font
import java.awt.font.TextAttribute


class GeneratorJvmImpl(
    override val settings: GeneratorSettings,
) : Generator {
    val font = Font(buildMap {
        put(TextAttribute.FAMILY, settings.fontName)
        put(TextAttribute.SIZE, settings.fontSize)
        if (FontStyle.BOLD in settings.fontStyles)
            put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)
        if (FontStyle.ITALIC in settings.fontStyles)
            put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE)
    })

    override fun generateShape(data: String) = font.generateShape(text = data)
        .align(hAlign = settings.hAlign, vAlign = settings.vAlign)
        .toShapeLinear(tolerance = tolerance)
}

actual fun GeneratorSettings.toGenerator(): Generator = GeneratorJvmImpl(settings = this)