package de.quati.grbl_laser

import java.awt.Font
import java.awt.font.TextAttribute


actual fun generate(data: GenerateGCodeData): Sequence<Pair<String, GCode>> {
    val fontAttributes = buildMap {
        put(TextAttribute.FAMILY, data.fontName)
        put(TextAttribute.SIZE, data.fontSize)
        if (FontStyle.BOLD in data.fontStyles)
            put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)
        if (FontStyle.ITALIC in data.fontStyles)
            put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE)
    }
    println(data.fontStyles)
    val font = Font(fontAttributes)
    val tolerance = data.fontSize.toDouble() / 96
    return data.inputData.asSequence().map { t ->
        t to font.generateShape(text = t)
            .align(hAlign = data.hAlign, vAlign = data.vAlign)
            .toGcode(tolerance = tolerance, power = data.laserPower, speed = data.laserSpeed)
    }
}

