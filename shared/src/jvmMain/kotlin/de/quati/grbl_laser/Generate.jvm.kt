package de.quati.grbl_laser

import java.awt.Font

actual fun generate(data: GenerateGCodeData): Sequence<Pair<String, GCode>> {
    val font = Font(data.fontName, Font.PLAIN, 1).deriveFont(data.fontSize)
    val tolerance = data.fontSize.toDouble() / 96
    return data.inputData.asSequence().map { t ->
        t to font.generateShape(text = t)
            .align(hAlign = data.hAlign, vAlign = data.vAlign)
            .toGcode(tolerance = tolerance, power = data.laserPower, speed = data.laserSpeed)
    }
}
